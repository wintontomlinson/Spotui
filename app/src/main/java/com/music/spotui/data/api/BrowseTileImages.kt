package com.music.spotui.data.api

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
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
                // Albums first, since album art is square and crops cleanly into the
                // tile. Playlists are the fallback for queries with no album hits.
                val albumArt = YouTube.search(genre, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()
                    ?.items
                    ?.filterIsInstance<AlbumItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }
                    ?.thumbnail
                    ?: YouTube.search(genre, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<PlaylistItem>()
                        ?.firstOrNull { !it.thumbnail.isNullOrBlank() }
                        ?.thumbnail
                // Requested large so the tile art stays sharp on high density screens.
                hiResThumbnail(albumArt, size = 544)
            }.getOrDefault("")
        }

        if (url.isNotBlank()) cache[genre] = url
        return url
    }
}
