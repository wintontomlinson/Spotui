package com.music.spotui.data.api

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.metrolist.innertube.models.SongItem
import com.music.spotui.ui.viewmodel.hiResThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Cover art for the Explore "Browse all" tiles, the way Spotify shows them: each
 * coloured box carries a real cover, tilted into its corner, instead of being a flat
 * block of colour.
 *
 * The cover comes from YouTube Music so it works without any login, which the earlier
 * Spotify backed version did not: it returned an empty string whenever there was no
 * session, so every tile was permanently blank. One search per category, resolved
 * lazily on first display and cached for the app session.
 */
object BrowseTileImages {

    private val cache = ConcurrentHashMap<String, String>()

    /** Already resolved cover, so a tile scrolled back into view draws it immediately. */
    fun cachedFor(genre: String): String = cache[genre].orEmpty()

    suspend fun coverFor(genre: String): String {
        cache[genre]?.let { return it }

        val url = withContext(Dispatchers.IO) {
            runCatching {
                // Bias the image search toward fresh/latest artwork so tiles show
                // current covers rather than a decade-old album. Falls back to the
                // plain genre if the "latest" query returns nothing.
                val freshQuery = "$genre 2026 latest"

                fun albumArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()?.items?.filterIsInstance<AlbumItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail

                fun playlistArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()
                    ?.firstOrNull { !it.thumbnail.isNullOrBlank() }?.thumbnail

                fun songArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                    .getOrNull()?.items?.filterIsInstance<SongItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail

                // Prefer fresh album art → fresh playlist art → plain-query album/
                // playlist → song thumbnail, so a tile is never left blank.
                val raw = albumArt(freshQuery)
                    ?: playlistArt(freshQuery)
                    ?: albumArt(genre)
                    ?: playlistArt(genre)
                    ?: songArt(genre)

                // Requested extra large so the full-bleed tile art stays crisp on
                // high-density screens (tiles are now image-forward, not thumbnails).
                hiResThumbnail(raw, size = 720)
            }.getOrDefault("")
        }

        if (url.isNotBlank()) cache[genre] = url
        return url
    }
}
