package com.music.spotui.audio

import android.net.Uri
import android.util.Log
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.TransferListener
import java.io.EOFException
import java.io.IOException
import java.net.ProtocolException

/**
 * Wraps a data source so a stream that is cut short mid track is reconnected instead of
 * being reported as the end of the song.
 *
 * Streaming hosts routinely close a long lived response early. media3's
 * DefaultHttpDataSource turns that into a plain RESULT_END_OF_INPUT even when the
 * response promised more bytes than it delivered, so the player believes the track
 * finished and the queue advances halfway through the song. Worse, when this source sits
 * under a CacheDataSource that early end is recorded as the content length of the track,
 * which makes every later play of the same song stop at the very same point.
 *
 * This source keeps its own byte accounting against the length promised by the first
 * successful open. When the upstream ends before delivering that many bytes it reopens
 * from the exact offset already consumed and keeps feeding the player. If reconnecting is
 * not possible it raises an IOException rather than a clean end of input, so callers can
 * re-resolve the track and the cache is never told a truncated length.
 */
class ResilientPlaybackDataSourceFactory(
    private val upstreamFactory: DataSource.Factory,
) : DataSource.Factory {
    override fun createDataSource(): DataSource =
        ResilientPlaybackDataSource(upstreamFactory.createDataSource())
}

class ResilientPlaybackDataSource(
    private val upstream: DataSource,
) : DataSource {
    private var baseDataSpec: DataSpec? = null

    /** Bytes handed to the caller since [open], counted across every reconnect. */
    private var bytesReadFromBase = 0L

    /** Total length promised by the first successful open, or LENGTH_UNSET when unknown. */
    private var declaredLength = C.LENGTH_UNSET.toLong()

    private var readRetryCount = 0
    private var bytesAtLastRetry = 0L

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        baseDataSpec = dataSpec
        bytesReadFromBase = 0L
        readRetryCount = 0
        bytesAtLastRetry = 0L
        declaredLength = C.LENGTH_UNSET.toLong()
        val resolvedLength = upstream.open(dataSpec)
        declaredLength = resolvedLength
        return resolvedLength
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
                    if (error.isRecoverableUnexpectedEnd() && reopenFromCurrentOffset()) {
                        continue
                    }
                    throw error
                }

            if (read != C.RESULT_END_OF_INPUT) {
                bytesReadFromBase += read
                return read
            }

            // Upstream reports the resource is finished. Trust that only when it
            // delivered everything it promised.
            val missing = missingBytes()
            if (missing <= 0L) return read

            if (reopenFromCurrentOffset()) continue

            throw IOException(
                "Stream ended $missing bytes early after $bytesReadFromBase of $declaredLength bytes",
            )
        }
    }

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        baseDataSpec = null
        bytesReadFromBase = 0L
        declaredLength = C.LENGTH_UNSET.toLong()
        readRetryCount = 0
        bytesAtLastRetry = 0L
        upstream.close()
    }

    /** Bytes the first open promised but the upstream has not delivered yet. */
    private fun missingBytes(): Long {
        val total = declaredLength
        if (total == C.LENGTH_UNSET.toLong()) return 0L
        return (total - bytesReadFromBase).coerceAtLeast(0L)
    }

    /**
     * Reopens the upstream at the offset already consumed, asking only for the bytes still
     * outstanding. Returns false when there is nothing left to fetch or the retry budget
     * is spent.
     */
    private fun reopenFromCurrentOffset(): Boolean {
        val original = baseDataSpec ?: return false
        val remaining = missingBytes()
        if (remaining <= 0L) return false

        // A long track can legitimately lose its connection several times. Give the
        // budget back whenever a healthy amount of audio has flowed since the last
        // reconnect, while still refusing to loop on a source that returns nothing.
        if (bytesReadFromBase - bytesAtLastRetry >= PROGRESS_RESET_BYTES) {
            readRetryCount = 0
        }
        if (readRetryCount >= MAX_READ_RETRIES) return false

        readRetryCount++
        bytesAtLastRetry = bytesReadFromBase
        runCatching { upstream.close() }

        val retrySpec =
            original
                .buildUpon()
                .setPosition(original.position + bytesReadFromBase)
                .setLength(remaining)
                .build()
        return runCatching { upstream.open(retrySpec) }
            .onFailure { Log.w(TAG, "reconnect at offset $bytesReadFromBase failed", it) }
            .isSuccess
    }

    private fun Throwable.isRecoverableUnexpectedEnd(): Boolean {
        var current: Throwable? = this
        while (current != null) {
            val message = current.message.orEmpty()
            if (
                current is ProtocolException &&
                message.contains("unexpected end of stream", ignoreCase = true)
            ) {
                return true
            }
            if (
                current is EOFException &&
                message.contains("unexpected", ignoreCase = true)
            ) {
                return true
            }
            current = current.cause
        }
        return false
    }

    private companion object {
        private const val TAG = "ResilientPlayback"
        private const val MAX_READ_RETRIES = 4
        private const val PROGRESS_RESET_BYTES = 512L * 1024
    }
}
