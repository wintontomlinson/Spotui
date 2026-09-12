package com.music.spotui.data.api

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.music.spotui.ui.viewmodel.hiResThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Cover art for the Explore "Browse all" tiles. Each tile shows a real, current
 * cover pulled from YouTube Music (so it works with no login) instead of a flat
 * block of colour.
 *
 * Design goals:
 *  • FRESH — the search is biased toward the current year / "latest", and the
 *    cache is refreshed once a day so the grid keeps showing new artwork over time
 *    rather than freezing on whatever loaded first.
 *  • NEVER BLANK — a chain of fallbacks (album → playlist → video → song) means a
 *    tile always resolves to something.
 *  • CHEAP — resolved covers are cached; transient failures are negatively cached
 *    for a short window so a flaky network doesn't trigger a search on every scroll.
 */
object BrowseTileImages {

    private data class Entry(val url: String, val ts: Long)

    private val cache = ConcurrentHashMap<String, Entry>()

    // Successful covers refresh once a day so Explore keeps updating its imagery.
    private const val SUCCESS_TTL_MS = 24L * 60 * 60 * 1000
    // Failures are remembered briefly so we don't hammer the network on every
    // recomposition/scroll, but still retry reasonably soon.
    private const val FAILURE_TTL_MS = 2L * 60 * 1000

    private fun fresh(entry: Entry?): Boolean {
        if (entry == null) return false
        val age = System.currentTimeMillis() - entry.ts
        return if (entry.url.isNotBlank()) age < SUCCESS_TTL_MS else age < FAILURE_TTL_MS
    }

    /** Already resolved cover, so a tile scrolled back into view draws it immediately. */
    fun cachedFor(genre: String): String = cache[genre]?.takeIf { it.url.isNotBlank() }?.url.orEmpty()

    suspend fun coverFor(genre: String): String {
        cache[genre]?.let { if (fresh(it)) return it.url }

        val url = withContext(Dispatchers.IO) {
            runCatching {
                // Only use ALBUM and PLAYLIST art — these are square cover images.
                // We deliberately do NOT fall back to video/song thumbnails: those
                // are 16:9 music-video stills / "wallpaper" frames that look wrong
                // stretched across a tile.
                suspend fun albumArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()?.items?.filterIsInstance<AlbumItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail

                suspend fun playlistArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()
                    ?.firstOrNull { !it.thumbnail.isNullOrBlank() }?.thumbnail

                // Album cover first (cleanest), then a playlist cover. No song/video
                // thumbnail fallback, so no wallpaper-style stills leak in.
                val raw = albumArt(genre) ?: playlistArt(genre)

                // Extra large so the full-bleed tile art stays crisp on high-density
                // screens (tiles are image-forward, not small thumbnails).
                hiResThumbnail(raw, size = 720)
            }.getOrDefault("")
        }

        // Cache success (day-long) or failure (short) so scrolling is cheap and the
        // grid still refreshes its imagery over time.
        cache[genre] = Entry(url, System.currentTimeMillis())
        return url
    }
}
