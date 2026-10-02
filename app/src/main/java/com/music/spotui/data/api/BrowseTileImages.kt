package com.music.spotui.data.api

import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.PlaylistItem
import com.music.spotui.ui.viewmodel.hiResThumbnail
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

/**
 * Artwork for the Explore tiles.
 *
 * Explore art is resolved from the app's OWN music source (the YouTube/innertube search the
 * rest of the app already uses) so a tile shows a real, on-theme album or playlist cover,
 * loaded through the same Glide path as every other cover. There is NO dependency on an
 * external stock-photo CDN, so the look is identical to where covers already appear in the app.
 *
 * Resolution is off the main thread, crash-safe, and cached. Whenever a cover is missing or
 * blank (offline, blocked, or no match) the caller keeps its premium code-generated gradient
 * mesh, so a tile is never an empty box.
 */
object BrowseTileImages {

    private data class Entry(val url: String, val ts: Long)

    private val cache = ConcurrentHashMap<String, Entry>()

    private const val SUCCESS_TTL_MS = 24L * 60 * 60 * 1000
    private const val FAILURE_TTL_MS = 2L * 60 * 1000

    private fun fresh(entry: Entry?): Boolean {
        if (entry == null) return false
        val age = System.currentTimeMillis() - entry.ts
        return if (entry.url.isNotBlank()) age < SUCCESS_TTL_MS else age < FAILURE_TTL_MS
    }

    /** Already resolved cover, so a tile scrolled back into view draws it immediately. */
    fun cachedFor(key: String): String {
        return cache[key]?.takeIf { it.url.isNotBlank() }?.url.orEmpty()
    }

    /**
     * Resolve a square cover for [key] (a music search string) from the app's YouTube source.
     * Runs off the main thread, is wrapped in [runCatching] so it can never throw into the
     * composition, and caches the result (even a blank one, briefly) so scrolling is smooth.
     */
    suspend fun coverFor(key: String): String {
        cache[key]?.let { if (fresh(it)) return it.url }
        val url = withContext(Dispatchers.IO) {
            runCatching {
                suspend fun albumArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()?.items?.filterIsInstance<AlbumItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail

                suspend fun playlistArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()
                    ?.firstOrNull { !it.thumbnail.isNullOrBlank() }?.thumbnail

                hiResThumbnail(albumArt(key) ?: playlistArt(key), size = 720)
            }.getOrDefault("")
        }
        cache[key] = Entry(url, System.currentTimeMillis())
        return url
    }
}
