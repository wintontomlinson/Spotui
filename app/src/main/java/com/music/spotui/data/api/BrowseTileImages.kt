package com.music.spotui.data.api

import android.content.Context
import com.metrolist.spotify.Spotify
import java.util.concurrent.ConcurrentHashMap

/**
 * Cover art for the Search "Explore" tiles, like Spotify web shows: each
 * category box carries the cover of a top playlist for that genre, rendered
 * full-bleed behind a royal scrim.
 *
 * Resolved lazily (a small playlist search per category) and cached for the app
 * session. For a professional look we deliberately:
 *   1. Pull a handful of top playlists (not just one) so a hit that happens to
 *      have no artwork doesn't leave the tile blank.
 *   2. Choose the *highest-resolution* image Spotify offers for that playlist,
 *      so the full-bleed tile stays crisp instead of upscaling a thumbnail.
 */
object BrowseTileImages {

    private val cache = ConcurrentHashMap<String, String>()

    suspend fun coverFor(context: Context, genre: String): String {
        cache[genre]?.let { return it }
        if (!SpotifyTokenProvider.ensureToken(context.applicationContext)) return ""

        val playlists = Spotify.search(genre, types = listOf("playlist"), limit = 6)
            .getOrNull()
            ?.playlists?.items
            .orEmpty()

        // First playlist (in relevance order) that actually has artwork.
        val url = playlists
            .asSequence()
            .mapNotNull { it.images.bestQualityUrl() }
            .firstOrNull()
            .orEmpty()

        if (url.isNotBlank()) cache[genre] = url
        return url
    }

    /**
     * Spotify returns a playlist's images in an unspecified size order; pick the
     * largest by area so the full-bleed Explore tile is sharp. Falls back to the
     * first entry when dimensions are missing.
     */
    private fun List<com.metrolist.spotify.models.SpotifyImage>.bestQualityUrl(): String? {
        if (isEmpty()) return null
        val best = maxByOrNull { img ->
            val w = img.width ?: 0
            val h = img.height ?: 0
            w.toLong() * h.toLong()
        } ?: first()
        return best.url.takeIf { it.isNotBlank() } ?: firstOrNull { it.url.isNotBlank() }?.url
    }
}
