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
        // key = BrowseCategory.imageQuery. Each photo is chosen to visibly match
        // the category NAME, so the tile art reads at a glance.
        "trending"    to u("1470225620780-dba8ba36b745"),  // stage lights / crowd — trending
        "charts"      to u("1511671782779-c97d3d27a1d4"),  // vinyl records — charts
        "new"         to u("1493225457124-a3eb161ffa5f"),  // fresh studio spotlight — new
        "madeforyou"  to u("1459749411175-04bf5292ceea"),  // headphones in sun — made for you
        "bollywood"   to u("1516450360452-9312f5e86fc7"),  // warm colourful concert — bollywood
        "punjabi"     to u("1493676304819-0d7a8d026dcf"),  // dhol / vibrant stage — punjabi
        "hiphop"      to u("1546528377-9049abecd05f"),     // moody hip-hop studio mic
        "pop"         to u("1516280440614-37939bbacd81"),  // pink pop concert
        "lofi"        to u("1483000805330-4eaf0a0d82da"),  // cosy study desk — lo-fi
        "workout"     to u("1571019613454-1cb2f99b2d8b"),  // gym weights — workout
        "romance"     to u("1518621736915-f3b1c41bfd00"),  // warm candlelit — romance
        "party"       to u("1533174072545-7a4b6ad7a6c3"),  // club party lights
        "devotional"  to u("1604608672516-f1b9b1d37076"),  // temple diya — devotional
        "retro"       to u("1470229722913-7c0e2dbbafd3"),  // retro neon — 90s
        "sad"         to u("1499415479124-43c32433a620"),  // rainy melancholic — sad
        "english"     to u("1470225620780-dba8ba36b745"),  // stadium pop — english
        "instrumental" to u("1520523839897-bd0b52f945a0"), // grand piano — instrumental
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
