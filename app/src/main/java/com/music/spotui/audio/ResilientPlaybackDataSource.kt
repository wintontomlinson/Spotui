package com.music.spotui.audio

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import com.music.spotui.data.diagnostics.PlaybackLog
import java.io.IOException

/**
 * Keeps a track playing to its end instead of letting a dropped stream look like the song
 * finishing, and asks streaming hosts for byte ranges in the form they actually accept.
 *
 * Everything here is driven by real playback logs:
 *
 * * A single open ended request for a whole track gets throttled hard and then reset.
 *   Measured against a live stream, one open ended GET ran at about 28 KB/s and was reset
 *   at 95% of the file.
 * * media3's DefaultHttpDataSource turns a response that stops early into a plain
 *   RESULT_END_OF_INPUT. The player then treats a cut off stream as the end of the song and
 *   the queue advances, with no error anywhere. Worse, that end of input reaching
 *   CacheDataSource is recorded as the content length of the track.
 * * These hosts serve the first request for a URL and answer later ones with 403 when the
 *   offset is asked for with a Range header. A log showed a stream open successfully for
 *   3261926 bytes and then fail with `Response code: 403` after 194 bytes as soon as a
 *   request at a non zero offset was needed, on every retry. That breaks both seeking and
 *   reconnecting. The official clients pass the offset as a query parameter instead, so
 *   that is what this does for those hosts.
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
        /**
         * Chunking is off.
         *
         * Paging a track into bounded requests does avoid the throttling on open ended
         * responses, but it needs one request per chunk, and these hosts refuse the
         * follow up requests. A log showed the first chunk served and the next one
         * refused with 403 at exactly the 1048576 byte boundary, so playback stopped
         * about a minute into every track. One request per URL is what these URLs
         * support, and a dropped one is recovered by re-resolving the track.
         */
        const val DEFAULT_CHUNK_BYTES = 0L
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

        if (isChunkable(dataSpec)) {
            openUpstream(dataSpec, offset = 0L, length = chunkBytes)
            val available = availableFromResponse(dataSpec)
            if (available != C.LENGTH_UNSET.toLong()) {
                chunked = true
                declaredLength = available
                PlaybackLog.add(
                    "stream",
                    "opened chunked, $available bytes from position ${dataSpec.position}",
                )
                return available
            }
            // Without the real size there is no way to page forward safely.
            runCatching { upstream.close() }
        }

        declaredLength = openUpstream(dataSpec, offset = 0L, length = dataSpec.length)
        PlaybackLog.add(
            "stream",
            "opened $declaredLength bytes from position ${dataSpec.position}" +
                if (usesQueryRange(dataSpec) && dataSpec.position > 0L) " via query range" else "",
        )
        return declaredLength
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
                    if (!isWorthContinuing(error) || !continueFromCurrentOffset(afterError = true)) {
                        PlaybackLog.add(
                            "stream",
                            "gave up at $bytesReadFromBase of $declaredLength: " +
                                "${error.javaClass.simpleName}: ${error.message}",
                        )
                        throw error
                    }
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
                PlaybackLog.add(
                    "stream",
                    "stopped ${missingBytes()} bytes early, after $bytesReadFromBase of $declaredLength",
                )
                throw IOException(
                    "Stream stopped ${missingBytes()} bytes early, " +
                        "after $bytesReadFromBase of $declaredLength",
                )
            }
        }
    }

    /**
     * The base URI, never the one carrying a range parameter. CacheDataSource records the
     * uri a source reports as a redirect target and reuses it for later requests, so
     * handing it a ranged uri would make every later request ask for the wrong bytes.
     */
    override fun getUri(): Uri? = baseDataSpec?.uri ?: upstream.uri

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

    /**
     * Opens the upstream for [length] bytes starting [offset] past the base position.
     *
     * For hosts that only accept a Range header on the very first request for a URL, the
     * offset is put in the query string and the spec is handed down as an unbounded read
     * from position zero, so no Range header is produced at all.
     */
    private fun openUpstream(base: DataSpec, offset: Long, length: Long): Long {
        val absolute = base.position + offset
        val needsExplicitRange = absolute > 0L || length != C.LENGTH_UNSET.toLong()

        if (usesQueryRange(base) && needsExplicitRange) {
            val end = if (length == C.LENGTH_UNSET.toLong()) null else absolute + length - 1
            val rangedUri = base.uri
                .buildUpon()
                .appendQueryParameter("range", if (end == null) "$absolute-" else "$absolute-$end")
                .build()
            val spec = base
                .buildUpon()
                .setUri(rangedUri)
                .setPosition(0L)
                .setLength(C.LENGTH_UNSET.toLong())
                .build()
            return upstream.open(spec)
        }

        val spec = base.buildUpon().setPosition(absolute).setLength(length).build()
        return upstream.open(spec)
    }

    /** Hosts known to refuse a Range header on anything but the first request for a URL. */
    private fun usesQueryRange(dataSpec: DataSpec): Boolean =
        dataSpec.uri.host?.contains("googlevideo.com", ignoreCase = true) == true

    /** Chunking only makes sense for a remote resource we were asked to read to the end. */
    private fun isChunkable(dataSpec: DataSpec): Boolean {
        if (chunkBytes <= 0L) return false
        if (dataSpec.length != C.LENGTH_UNSET.toLong()) return false
        if (chunkingIsRefused()) return false
        val scheme = dataSpec.uri.scheme?.lowercase()
        return scheme == "http" || scheme == "https"
    }

    /**
     * Bytes available from [dataSpec]'s position, worked out from the Content-Range of a
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
     * Reopens the upstream at the offset already delivered. Returns false when nothing is
     * outstanding or the retry budget is spent.
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

        if (afterError) {
            // Give a flapping connection a moment rather than burning the budget at once.
            // This runs on the player's loading thread, where blocking is expected.
            runCatching { Thread.sleep(RETRY_BACKOFF_MS * reopensSinceProgress) }
        }

        val take = if (chunked) minOf(chunkBytes, remaining) else remaining
        PlaybackLog.add(
            "stream",
            (if (afterError) "reconnect after error" else "next chunk") +
                " at $bytesReadFromBase, $remaining of $declaredLength left",
        )
        if (tryOpenAtCurrentOffset(original, take)) return true

        // The first chunk was served and a later one refused, so this URL will not keep
        // serving ranged continuations. Ask for everything that is left in one request.
        if (chunked && take != remaining) {
            chunked = false
            rememberChunkingRefused()
            PlaybackLog.add(
                "stream",
                "chunked continuation refused at $bytesReadFromBase, " +
                    "asking for the remaining $remaining in one request",
            )
            if (tryOpenAtCurrentOffset(original, remaining)) return true
        }
        return false
    }

    private fun tryOpenAtCurrentOffset(original: DataSpec, length: Long): Boolean {
        runCatching { upstream.close() }
        return runCatching { openUpstream(original, bytesReadFromBase, length) }
            .onFailure { Log.w(TAG, "could not continue at offset $bytesReadFromBase", it) }
            .isSuccess
    }

    /**
     * Whether it is worth trying to carry on after [error].
     *
     * A request refused before a single byte arrived means the URL itself is no longer
     * accepted, and retrying it will not change that, so it goes back to the caller to be
     * re-resolved. Once audio has flowed, a refusal is usually the host declining this
     * particular follow up request, which [continueFromCurrentOffset] can work around.
     */
    private fun isWorthContinuing(error: IOException): Boolean {
        if (bytesReadFromBase > 0L) return true
        return !isRejected(error)
    }

    private fun isRejected(error: IOException): Boolean {
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

        /** How long to stop chunking after a host refused a ranged continuation. */
        private const val CHUNKING_COOLDOWN_MS = 10 * 60 * 1000L

        /**
         * A new source is created for every load and seek, so without remembering this
         * each one would spend a request discovering the same refusal all over again.
         */
        @Volatile
        private var chunkingRefusedUntil = 0L

        fun rememberChunkingRefused() {
            chunkingRefusedUntil = android.os.SystemClock.elapsedRealtime() + CHUNKING_COOLDOWN_MS
        }

        fun chunkingIsRefused(): Boolean =
            android.os.SystemClock.elapsedRealtime() < chunkingRefusedUntil
    }
}
