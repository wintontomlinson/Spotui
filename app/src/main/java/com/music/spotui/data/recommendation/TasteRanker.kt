package com.music.spotui.data.recommendation

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HistoryEntry

/**
 * Re-ranks any candidate list (Spotify recommendations, YouTube radio, Home mix)
 * against the on-device [TasteProfile], then picks a diverse running order.
 *
 * score = 1.0·artistAffinity + 0.8·seedArtistMatch + 0.5·sourceWeight
 *       + 0.4·trackAffinity − 1.5·(often skipped, never finished)
 *       + 0.25·novelty + a small per-day deterministic jitter
 *
 * Hard excludes: anything played in the last 3 hours or among the last 40 history
 * entries, and tracks whose skips outnumber completes by 3 or more.
 * Diversity: never the same primary artist twice in a row, at most 2 per artist in
 * any 10 picks, and every 5th slot goes to the best new artist (exploration).
 */
object TasteRanker {

    private const val RECENT_WINDOW_MS = 3L * 60 * 60 * 1000
    private const val RECENT_ENTRIES = 40

    private fun norm(s: String) = s.lowercase().filter { it.isLetterOrDigit() }

    private fun artistsOf(singer: String): Set<String> =
        singer.split(",").map { it.trim().removeSuffix(" - Topic").trim().lowercase() }
            .filter { it.isNotBlank() }.toSet()

    fun rank(
        ctx: Context,
        candidates: List<SongsModel>,
        seed: SongsModel?,
        recent: List<HistoryEntry>,
        sourceWeight: Map<String, Float> = emptyMap(),
        limit: Int = 25,
    ): List<SongsModel> {
        if (candidates.isEmpty()) return emptyList()
        val now = System.currentTimeMillis()
        val affinity = TasteProfile.artistAffinity(ctx)
        val known = affinity.keys

        val recentEntries = recent.filterIndexed { i, e -> i < RECENT_ENTRIES || now - e.ts < RECENT_WINDOW_MS }
        val recentUrls = recentEntries.mapNotNull { it.url.ifBlank { null } }.toSet()
        val recentTitles = recentEntries.map { norm(it.title) + "|" + norm(TasteProfile.artistKey(it.singer)) }.toSet()

        val seedPrimary = seed?.let { TasteProfile.artistKey(it.singer) }
        val seedArtists = seed?.let { artistsOf(it.singer) }.orEmpty()
        val day = now / 86_400_000L

        data class Scored(val song: SongsModel, val artist: String, val novel: Boolean, val score: Double)

        val scored = candidates
            .distinctBy { it.url.ifBlank { it.id.toString() } }
            .filter { it.title.isNotBlank() }
            .filterNot { it.url in recentUrls }
            .filterNot { (norm(it.title) + "|" + norm(TasteProfile.artistKey(it.singer))) in recentTitles }
            .mapNotNull { song ->
                val stats = TasteProfile.trackStats(ctx, song.url)
                if (stats != null && stats.skips - stats.completes >= 3) return@mapNotNull null
                val artist = TasteProfile.artistKey(song.singer)
                val seedMatch = when {
                    seedPrimary.isNullOrBlank() -> 0.0
                    artist == seedPrimary -> 1.0
                    artistsOf(song.singer).any { it in seedArtists } -> 0.5
                    else -> 0.0
                }
                val novel = artist.isNotBlank() && artist !in known
                val oftenSkipped = stats != null && stats.skips >= 2 && stats.completes == 0
                val jitter = ((song.url + day).hashCode().toLong().let { if (it < 0) -it else it } % 100) / 1000.0
                val score = 1.0 * (affinity[artist] ?: 0f) +
                    0.8 * seedMatch +
                    0.5 * (sourceWeight[song.url] ?: 0.5f) +
                    0.4 * TasteProfile.trackAffinity(stats) -
                    (if (oftenSkipped) 1.5 else 0.0) +
                    (if (novel) 0.25 else 0.0) +
                    jitter
                Scored(song, artist, novel, score)
            }
            .sortedByDescending { it.score }
            .toMutableList()

        val picks = ArrayList<Scored>()
        fun fits(c: Scored): Boolean {
            if (c.artist.isBlank()) return true
            if (picks.lastOrNull()?.artist == c.artist) return false
            return picks.takeLast(9).count { it.artist == c.artist } < 2
        }
        while (picks.size < limit && scored.isNotEmpty()) {
            val explore = (picks.size + 1) % 5 == 0
            val next = (if (explore) scored.firstOrNull { it.novel && fits(it) } else null)
                ?: scored.firstOrNull { fits(it) }
                ?: break
            scored.remove(next)
            picks += next
        }
        return picks.map { it.song }
    }
}
