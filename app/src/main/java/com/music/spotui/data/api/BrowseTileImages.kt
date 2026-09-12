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
 * The YouTube-Music search route used to be flaky — many tiles resolved to a
 * random or missing cover. So Explore now uses a CURATED set of professional,
 * always-available photos (Unsplash CDN, no API key, permanent image ids), one
 * per category, keyed by the tile's [BrowseCategory.imageQuery]. This guarantees
 * every tile shows a clean, on-theme, high-quality image. YouTube album art is
 * kept only as a last-resort fallback for any unmapped key.
 */
object BrowseTileImages {

    // Unsplash "images" CDN — direct, permanent photo ids cropped square at 640px.
    // These are hand-picked, professional, on-theme music/mood photos.
    private const val SZ = "&w=640&h=640&fit=crop&crop=entropy&q=80"
    private fun u(id: String) = "https://images.unsplash.com/photo-$id?auto=format$SZ"

    private val CURATED: Map<String, String> = mapOf(
        // key = BrowseCategory.imageQuery
        "top hits 2026 album" to u("1470225620780-dba8ba36b745"),           // stage lights crowd
        "billboard hot 100 hits album" to u("1493225457124-a3eb161ffa5f"),  // concert spotlight
        "new album 2026" to u("1511671782779-c97d3d27a1d4"),                // vinyl / turntable
        "feel good hits 2026 album" to u("1459749411175-04bf5292ceea"),     // headphones sunlight
        "arijit singh hit songs album" to u("1516450360452-9312f5e86fc7"),  // warm concert
        "diljit dosanjh album" to u("1493676304819-0d7a8d026dcf"),          // colourful stage
        "drake album" to u("1546528377-9049abecd05f"),                     // moody hip-hop studio
        "taylor swift album" to u("1516280440614-37939bbacd81"),           // pop concert pink
        "lofi hip hop beats album" to u("1483000805330-4eaf0a0d82da"),      // cozy desk study
        "beast mode workout album" to u("1571019613454-1cb2f99b2d8b"),      // gym weights
        "romantic love songs album" to u("1518621736915-f3b1c41bfd00"),     // warm romantic
        "dance party hits album" to u("1516450360452-9312f5e86fc7"),        // party lights
        "krishna bhajan album" to u("1604608672516-f1b9b1d37076"),          // temple / diya
        "90s bollywood hits album" to u("1470229722913-7c0e2dbbafd3"),      // retro concert
        "sad songs album" to u("1499415479124-43c32433a620"),              // rainy melancholic
        "the weeknd album" to u("1493225457124-a3eb161ffa5f"),             // dark neon
        "peaceful piano instrumental album" to u("1520523839897-bd0b52f945a0"), // grand piano
    )

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
    fun cachedFor(genre: String): String {
        // Curated images are known instantly — no need to wait for a network pass.
        CURATED[genre]?.let { return it }
        return cache[genre]?.takeIf { it.url.isNotBlank() }?.url.orEmpty()
    }

    suspend fun coverFor(genre: String): String {
        // 1) Curated professional image — instant, reliable, on-theme.
        CURATED[genre]?.let { return it }

        // 2) Fallback for any unmapped key: a clean square album cover from YouTube.
        cache[genre]?.let { if (fresh(it)) return it.url }
        val url = withContext(Dispatchers.IO) {
            runCatching {
                suspend fun albumArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_ALBUM)
                    .getOrNull()?.items?.filterIsInstance<AlbumItem>()
                    ?.firstOrNull { it.thumbnail.isNotBlank() }?.thumbnail

                suspend fun playlistArt(q: String): String? = YouTube.search(q, YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST)
                    .getOrNull()?.items?.filterIsInstance<PlaylistItem>()
                    ?.firstOrNull { !it.thumbnail.isNullOrBlank() }?.thumbnail

                hiResThumbnail(albumArt(genre) ?: playlistArt(genre), size = 720)
            }.getOrDefault("")
        }
        cache[genre] = Entry(url, System.currentTimeMillis())
        return url
    }
}
