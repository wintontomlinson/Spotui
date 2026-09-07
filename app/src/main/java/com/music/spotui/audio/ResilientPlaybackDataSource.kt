package com.music.spotui.audio

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.IOException

/**
 * Keeps a track playing to its end instead of letting a dropped stream look like the
 * song finishing.
 *
 * Two things go wrong with one long lived open ended request for a whole track:
 *
 * 1. Streaming hosts throttle open ended responses hard and then reset the connection,
 *    usually while the player is sitting on a full buffer and not reading. Measured
 *    against a real stream, a single open ended GET was throttled to roughly 28 KB/s and
 *    then reset partway through, while the same bytes fetched as bounded range requests
 *    arrived complete and at full speed.
 * 2. media3's DefaultHttpDataSource turns a response that stops early into a plain
 *    RESULT_END_OF_INPUT, and a reset surfaces as an IOException that the player treats
 *    as fatal. Either way the queue moves on and the rest of the song is lost. Worse, an
 *    early end of input reaching CacheDataSource is recorded as the content length of the
 *    track, so the song then stops at the same point on every later play.
 *
 * So this source pages through http resources in bounded chunks, and whenever a chunk
 * ends early or the connection dies it reopens from the exact byte offset already
 * delivered. The caller never sees a short read, and an IOException is raised only when
 * continuing is genuinely impossible, which keeps a truncated length out of the cache.
 *
 * Non http sources (local files, content URIs) are passed straight through.
 */
class ResilientPlaybackDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
    private val chunkBytes: Long = DEFAULT_CHUNK_BYTES,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        ResilientPlaybackDataSource(upstreamFactory.createDataSource(), chunkBytes)

    companion object {
        /** Roughly a minute of audio at typical streaming bitrates. */
        const val DEFAULT_CHUNK_BYTES = 1L * 1024 * 1024
    }
}

class ResilientPlaybackDataSource(
    private val upstream: DataSource,
    private val chunkBytes: Long = ResilientPlaybackDataSourceFactory.DEFAULT_CHUNK_BYTES,
) : DataSource {
    private var baseDataSpec: DataSpec? = null

    /** Bytes handed to the caller since [open], counted across every chunk and retry. */
    private var bytesReadFromBase = 0L

    /** Bytes available from the base position, or LENGTH_UNSET when unknown. */
    private var declaredLength = C.LENGTH_UNSET.toLong()

    private var chunked = false
    private var reopensSinceProgress = 0
    private var bytesAtLastReopen = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        baseDataSpec = dataSpec
        bytesReadFromBase = 0L
        declaredLength = C.LENGTH_UNSET.toLong()
        chunked = false
        reopensSinceProgress = 0
        bytesAtLastReopen = 0L

        if (!isChunkable(dataSpec)) return openWhole(dataSpec)

        // Ask for a bounded first chunk rather than the whole track.
        val firstChunk = dataSpec.buildUpon().setLength(chunkBytes).build()
        upstream.open(firstChunk)

        val available = availableFromResponse(dataSpec)
        if (available == C.LENGTH_UNSET.toLong()) {
            // Without the real size there is no way to page forward safely, so fall back
            // to a single request instead of guessing.
            runCatching { upstream.close() }
            return openWhole(dataSpec)
        }

        chunked = true
        declaredLength = available
        return available
    }

    override fun read(
        buffer: ByteArray,
        offset: Int,
        length: Int,
    ): Int {
        while (true) {
            val read =
                try {
                    upstream.read(buffer, offset, length)
                } catch (error: IOException) {
                    if (isFatal(error) || !continueFromCurrentOffset(afterError = true)) throw error
                    continue
                }

            if (read != C.RESULT_END_OF_INPUT) {
                bytesReadFromBase += read
                return read
            }

            // The current request is done. That is either the end of a chunk, or a
            // response that stopped short. Both mean the same thing here: if bytes are
            // still outstanding, go and get them.
            if (missingBytes() <= 0L) return read

            if (!continueFromCurrentOffset(afterError = false)) {
                throw IOException(
                    "Stream stopped ${missingBytes()} bytes early, " +
                        "after $bytesReadFromBase of $declaredLength",
                )
            }
        }
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        baseDataSpec = null
        bytesReadFromBase = 0L
        declaredLength = C.LENGTH_UNSET.toLong()
        chunked = false
        reopensSinceProgress = 0
        bytesAtLastReopen = 0L
        upstream.close()
    }

    private fun openWhole(dataSpec: DataSpec): Long {
        chunked = false
        declaredLength = upstream.open(dataSpec)
        return declaredLength
    }

    /** Chunking only makes sense for a remote resource we were asked to read to the end. */
    private fun isChunkable(dataSpec: DataSpec): Boolean {
        if (chunkBytes <= 0L) return false
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) return false
        val scheme = dataSpec.uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * Bytes available from [dataSpec]'s position, worked out from the Content-Range of the
     * bounded response, falling back to the content length the URL itself carries.
     */
    private fun availableFromResponse(dataSpec: DataSpec): Long {
        val total = totalFromContentRange() ?: totalFromUrl(dataSpec.uri)
        if (total == null || total <= dataSpec.position) return C.LENGTH_UNSET.toLong()
        return total - dataSpec.position
    }

    /** Parses the resource size out of a `Content-Range: bytes 0-1023/4096` header. */
    private fun totalFromContentRange(): Long? {
        val header = upstream.responseHeaders.entries
            .firstOrNull { it.key.equals("Content-Range", ignoreCase = true) }
            ?.value
            ?.firstOrNull()
            ?: return null
        return header.substringAfterLast('/', "").trim().toLongOrNull()?.takeIf { it > 0 }
    }

    /** Streaming URLs carry the size of the media as a query parameter. */
    private fun totalFromUrl(uri: Uri): Long? =
        runCatching { uri.getQueryParameter("clen") }
            .getOrNull()
            ?.toLongOrNull()
            ?.takeIf { it > 0 }

    /** Bytes still outstanding against what [open] said was available. */
    private fun missingBytes(): Long {
        val total = declaredLength
        if (total == C.LENGTH_UNSET.toLong()) return 0L
        return (total - bytesReadFromBase).coerceAtLeast(0L)
    }

    /**
     * Reopens the upstream at the offset already delivered, asking for the next chunk.
     * Returns false when nothing is outstanding or the retry budget is spent.
     */
    private fun continueFromCurrentOffset(afterError: Boolean): Boolean {
        val original = baseDataSpec ?: return false
        val remaining = missingBytes()
        if (remaining <= 0L) return false

        // Reaching a chunk boundary or surviving a blip both count as progress, so a long
        // track can reconnect many times while a source that returns nothing still stops.
        if (bytesReadFromBase - bytesAtLastReopen >= PROGRESS_RESET_BYTES) {
            reopensSinceProgress = 0
        }
        if (reopensSinceProgress >= MAX_REOPENS_WITHOUT_PROGRESS) return false

        reopensSinceProgress++
        bytesAtLastReopen = bytesReadFromBase
        runCatching { upstream.close() }

        if (afterError) {
            // Give a flapping connection a moment rather than burning the budget at once.
            // This runs on the player's loading thread, where blocking is expected.
            runCatching { Thread.sleep(RETRY_BACKOFF_MS * reopensSinceProgress) }
        }

        val take = if (chunked) minOf(chunkBytes, remaining) else remaining
        val next = original
            .buildUpon()
            .setPosition(original.position + bytesReadFromBase)
            .setLength(take)
            .build()
        return runCatching { upstream.open(next) }
            .onFailure { Log.w(TAG, "could not continue at offset $bytesReadFromBase", it) }
            .isSuccess
    }

    /** A rejected request will not start working on a retry, so do not spend the budget. */
    private fun isFatal(error: IOException): Boolean {
        var current: Throwable? = error
        while (current != null) {
            if (current is HttpDataSource.InvalidResponseCodeException &&
                current.responseCode in 400..499
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        private const val TAG = "ResilientPlayback"
        private const val MAX_REOPENS_WITHOUT_PROGRESS = 5
        private const val PROGRESS_RESET_BYTES = 256L * 1024
        private const val RETRY_BACKOFF_MS = 200L
    }
}
