package com.music.spotui.data.recommendation

import android.content.Context
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HistoryEntry

/**
 * Re-ranks any candidate list (Spotify recommendations, YouTube radio, Home mix)
 * against the on-device [TasteProfile], then picks a diverse running order. Both
 * login modes share this one policy: Spotify recs are routed through here too, so
 * the context/anti-repeat/exploration behaviour is identical with or without login.
 *
 * score = W_ARTIST·artistAffinity(current time bucket) + W_SEED·seedArtistMatch
 *       + W_SOURCE·sourceWeight + W_TRACK·trackAffinity + W_CLUSTER·clusterFit
 *       − SKIP_PENALTY·(often skipped, never finished) + NOVELTY_BONUS·novelty
 *       + a small per-day deterministic jitter
 *
 * Hard excludes: anything played in the last 3 hours or among the last 40 history
 * entries, and tracks whose skips outnumber completes by 3 or more.
 * Diversity: never the same primary artist within 3 slots, at most 2 per artist in
 * any 12 picks, same-session artists are penalised, three-in-a-row of the same
 * cluster is penalised, and an exploration slot (rate scaled to profile maturity)
 * goes to the best new artist.
 */
object TasteRanker {

    private const val RECENT_WINDOW_MS = 3L * 60 * 60 * 1000
    private const val RECENT_ENTRIES = 40

    // Scoring weights (tunable).
    private const val W_ARTIST = 1.0        // artist affinity (time-bucket aware)
    private const val W_SEED = 0.8          // seed-artist match
    private const val W_SOURCE = 0.5        // candidate source relevance
    private const val W_TRACK = 0.4         // per-track affinity
    private const val W_CLUSTER = 0.5       // current-context mood/genre cluster fit
    private const val SKIP_PENALTY = 1.5    // often-skipped-never-finished
    private const val NOVELTY_BONUS = 0.25  // unseen artist
    private const val SESSION_REPEAT_PENALTY = 0.6  // artist already played this session
    private const val CLUSTER_STREAK_PENALTY = 0.4  // same cluster 3rd time in a row

    // Diversity constraints.
    private const val ARTIST_CAP = 2            // max per artist within...
    private const val ARTIST_CAP_WINDOW = 12    // ...this many recent picks
    private const val SAME_ARTIST_GAP = 3       // no same primary artist within N slots
    private const val CLUSTER_STREAK = 3        // penalise this many identical clusters in a row

    // Dynamic exploration: pick an "explore" slot every N picks, N chosen from
    // profile maturity (small/stale -> explore more, rich -> explore less).
    private const val EXPLORE_EVERY_SMALL = 3
    private const val EXPLORE_EVERY_RICH = 6
    private const val MATURE_ARTIST_COUNT = 25  // >= this many known artists == "rich"

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
        val hourBucket = TasteProfile.currentHourBucket()
        // Context-aware affinity: lean to what the user plays at this time of day.
        val affinity = TasteProfile.artistAffinity(ctx, hourBucket)
        val known = TasteProfile.artistAffinity(ctx).keys
        val clusterAff = TasteProfile.clusterAffinity(ctx)
        // Same-session artists get a penalty on top of the persisted recency window.
        val sessionArtists = TasteProfile.recentSessionArtists().toSet()

        // Dynamic exploration rate from profile maturity.
        val exploreEvery = if (known.size >= MATURE_ARTIST_COUNT) EXPLORE_EVERY_RICH else EXPLORE_EVERY_SMALL

        val recentEntries = recent.filterIndexed { i, e -> i < RECENT_ENTRIES || now - e.ts < RECENT_WINDOW_MS }
        val recentUrls = recentEntries.mapNotNull { it.url.ifBlank { null } }.toSet()
        val recentTitles = recentEntries.map { norm(it.title) + "|" + norm(TasteProfile.artistKey(it.singer)) }.toSet()

        val seedPrimary = seed?.let { TasteProfile.artistKey(it.singer) }
        val seedArtists = seed?.let { artistsOf(it.singer) }.orEmpty()
        val day = now / 86_400_000L

        data class Scored(
            val song: SongsModel,
            val artist: String,
            val clusters: Set<String>,
            val novel: Boolean,
            val score: Double,
        )

        val scored = candidates
            .distinctBy { it.url.ifBlank { it.id.toString() } }
            .filter { it.title.isNotBlank() }
            .filterNot { it.url in recentUrls }
            .filterNot { (norm(it.title) + "|" + norm(TasteProfile.artistKey(it.singer))) in recentTitles }
            .mapNotNull { song ->
                val stats = TasteProfile.trackStats(ctx, song.url)
                if (stats != null && stats.skips - stats.completes >= 3) return@mapNotNull null
                val artist = TasteProfile.artistKey(song.singer)
                val clusters = TasteProfile.clustersFor(song.title, song.singer)
                val seedMatch = when {
                    seedPrimary.isNullOrBlank() -> 0.0
                    artist == seedPrimary -> 1.0
                    artistsOf(song.singer).any { it in seedArtists } -> 0.5
                    else -> 0.0
                }
                val novel = artist.isNotBlank() && artist !in known
                val oftenSkipped = stats != null && stats.skips >= 2 && stats.completes == 0
                // Mood/genre fit: how well this track's clusters match the current context.
                val clusterFit = clusters.maxOfOrNull { clusterAff[it] ?: 0f }?.toDouble() ?: 0.0
                val sessionRepeat = artist.isNotBlank() && artist in sessionArtists
                val jitter = ((song.url + day).hashCode().toLong().let { if (it < 0) -it else it } % 100) / 1000.0
                val score = W_ARTIST * (affinity[artist] ?: 0f) +
                    W_SEED * seedMatch +
                    W_SOURCE * (sourceWeight[song.url] ?: 0.5f) +
                    W_TRACK * TasteProfile.trackAffinity(stats) +
                    W_CLUSTER * clusterFit -
                    (if (oftenSkipped) SKIP_PENALTY else 0.0) -
                    (if (sessionRepeat) SESSION_REPEAT_PENALTY else 0.0) +
                    (if (novel) NOVELTY_BONUS else 0.0) +
                    jitter
                Scored(song, artist, clusters, novel, score)
            }
            .sortedByDescending { it.score }
            .toMutableList()

        val picks = ArrayList<Scored>()

        // Soft cluster-streak penalty: if the last CLUSTER_STREAK-1 picks all share a
        // cluster, prefer a candidate that breaks it. Applied as a tie-breaker on top
        // of the hard diversity constraints below.
        fun breaksClusterStreak(c: Scored): Boolean {
            if (picks.size < CLUSTER_STREAK - 1) return true
            val tail = picks.takeLast(CLUSTER_STREAK - 1)
            val shared = tail.map { it.clusters }.reduceOrNull { a, b -> a intersect b }.orEmpty()
            if (shared.isEmpty()) return true
            return c.clusters.none { it in shared }
        }

        fun fits(c: Scored): Boolean {
            if (c.artist.isBlank()) return true
            // No same primary artist within SAME_ARTIST_GAP slots.
            if (picks.takeLast(SAME_ARTIST_GAP).any { it.artist == c.artist }) return false
            // At most ARTIST_CAP per artist within the last ARTIST_CAP_WINDOW picks.
            return picks.takeLast(ARTIST_CAP_WINDOW - 1).count { it.artist == c.artist } < ARTIST_CAP
        }

        while (picks.size < limit && scored.isNotEmpty()) {
            val explore = (picks.size + 1) % exploreEvery == 0
            val next = (if (explore) scored.firstOrNull { it.novel && fits(it) && breaksClusterStreak(it) } else null)
                ?: scored.firstOrNull { fits(it) && breaksClusterStreak(it) }
                ?: (if (explore) scored.firstOrNull { it.novel && fits(it) } else null)
                ?: scored.firstOrNull { fits(it) }
                ?: break
            scored.remove(next)
            picks += next
        }
        return picks.map { it.song }
    }
}
