package com.music.spotui.audio

import android.net.Uri
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.cache.CacheKeyFactory

/**
 * Builds the media cache key for a stream.
 *
 * Two properties matter and the original key had neither:
 *
 * 1. **It has to identify the exact bytes.** A streaming URL for one track offers several
 *    audio formats with different lengths, and the format is chosen per playback from the
 *    network conditions. Verified against a live response, the four audio formats of one
 *    track were 1,231,355 / 1,300,631 / 3,433,755 / 3,449,447 bytes and every one of them
 *    produced the same key, because only the host path and an id were used. Reusing one
 *    entry for all of them means cached bytes from one format being served as another, and
 *    a recorded content length that belongs to a different format, which ends the song
 *    early with no error at all. So the format tag is part of the key.
 * 2. **It has to survive re-resolution.** Streaming URLs carry a per request opaque id, so
 *    keying on that gives a brand new key every time a track is resolved and the cache is
 *    never actually reused. A stable track id is preferred whenever the caller has one.
 */
object LosslessCacheKeyFactory : CacheKeyFactory {
    private const val SPOTIFY_PREFIX = "spotify:track:"

    /**
     * @param trackId stable id for the track, when the caller knows one.
     * @param url the resolved stream URL.
     * @param videoId stable source id for the track, preferred over the opaque id in [url].
     */
    fun buildCacheKey(trackId: String?, url: String, videoId: String? = null): String {
        val uri = Uri.parse(url)
        val format = formatTag(uri)

        val cleanId = trackId?.removePrefix(SPOTIFY_PREFIX)?.substringBefore('|')?.trim()
        if (!cleanId.isNullOrBlank()) return "spotui-track:$cleanId:$format"

        val cleanVideoId = videoId?.trim()
        if (!cleanVideoId.isNullOrBlank()) return "spotui-yt:$cleanVideoId:$format"

        val host = uri.host.orEmpty()
        if (host.contains("googlevideo.com") || host.contains("youtube.com")) {
            val fromUrl = uri.getQueryParameter("docid")
                ?: uri.getQueryParameter("id")
                ?: uri.getQueryParameter("v")
            if (!fromUrl.isNullOrBlank()) return "spotui-yt:$fromUrl:$format"
        }

        val urlHash = java.util.zip.CRC32().apply { update(url.toByteArray()) }.value
        return "spotui-raw:$urlHash:$format"
    }

    /**
     * Identifies which of a track's formats a URL points at, so two formats of the same
     * track never share an entry. Falls back to the declared length, which also differs
     * per format, and finally to a fixed tag for URLs that carry neither.
     */
    private fun formatTag(uri: Uri): String {
        runCatching { uri.getQueryParameter("itag") }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return "itag$it" }
        runCatching { uri.getQueryParameter("clen") }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let { return "len$it" }
        val extension = uri.path.orEmpty().substringAfterLast('.', "")
        return if (extension.length in 2..5) extension.lowercase() else "default"
    }

    override fun buildCacheKey(dataSpec: DataSpec): String {
        dataSpec.key?.takeIf { it.isNotBlank() }?.let { return it }
        return buildCacheKey(null, dataSpec.uri.toString())
    }
}
