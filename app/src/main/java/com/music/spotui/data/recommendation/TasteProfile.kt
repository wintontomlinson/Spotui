package com.music.spotui.data.recommendation

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.json.JSONObject
import kotlin.math.exp
import kotlin.math.tanh

/**
 * On-device taste model. Every finished play is classified as a skip, a play or a
 * complete and folded into per-track counters; artist affinity is derived from
 * those counters (plus likes) with a three-week recency decay.
 *
 * Stored as one JSON map in the `TasteProfile` prefs file, capped at [MAX_TRACKS]
 * most recently touched tracks. Reads iterate a snapshot taken under the lock; writes go to IO.
 */
object TasteProfile {

    data class Stats(
        val title: String,
        val artist: String,
        val plays: Int = 0,
        val completes: Int = 0,
        val skips: Int = 0,
        val lastTs: Long = 0L,
        /**
         * Per-track play counts split by time-of-day bucket
         * (0=night 0–6, 1=morning 6–12, 2=afternoon 12–18, 3=evening 18–24).
         * Additive and optional: old JSON without it loads as four zeros.
         */
        val hourBuckets: IntArray = IntArray(HOUR_BUCKETS),
        /**
         * Coarse mood/genre cluster tags derived once from the title/artist
         * keywords (see [clustersFor]); empty for tracks that match no keyword.
         */
        val clusters: Set<String> = emptySet(),
    )

    private const val PREF = "TasteProfile"
    private const val KEY = "tracks"
    private const val MAX_TRACKS = 800
    private const val DECAY_DAYS = 21.0
    private const val DAY_MS = 86_400_000.0

    /** Number of time-of-day buckets (night / morning / afternoon / evening). */
    const val HOUR_BUCKETS = 4

    /** Affinity bonus for an artist whose plays fall in the current time bucket. */
    private const val BUCKET_BONUS = 0.3f

    /** Last ~15 artistKeys played this session; in-memory only, never persisted. */
    private const val SESSION_RING = 15
    private val sessionRing = ArrayDeque<String>()
    private val sessionLock = Any()

    /**
     * Coarse keyword → cluster table for lightweight, network-free mood/genre
     * signal. Each entry maps a cluster tag to the lowercase substrings that
     * imply it; a track can match several clusters. Pure string work.
     */
    private val CLUSTER_KEYWORDS: Map<String, List<String>> = mapOf(
        "chill" to listOf("lofi", "lo-fi", "lo fi", "chill", "chilled", "relax", "sleep", "study", "ambient", "calm"),
        "edm" to listOf("remix", "edm", "house", "techno", "trance", "dubstep", "electro", "club", "bass", "drop"),
        "acoustic" to listOf("acoustic", "unplugged", "stripped", "piano", "guitar version"),
        "live" to listOf("live", "concert", "mtv", "session", "tour"),
        "slowed" to listOf("slowed", "reverb", "slowed + reverb", "slow reverb", "nightcore", "sped up", "spedup"),
        "hype" to listOf("workout", "gym", "party", "dance", "banger", "hype", "pump"),
        "sad" to listOf("sad", "heartbreak", "breakup", "lonely", "cry", "emotional"),
        "romance" to listOf("love", "romantic", "romance", "valentine"),
    )

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    @Volatile private var cache: LinkedHashMap<String, Stats>? = null
    @Volatile private var affinityCache: Pair<Long, Map<String, Float>>? = null

    /** Lower-cased first artist without the YouTube " - Topic" suffix. */
    fun artistKey(singer: String): String =
        singer.substringBefore(",").trim().removeSuffix(" - Topic").trim().lowercase()

    /** Time-of-day bucket for an epoch-ms timestamp (0 night, 1 morning, 2 afternoon, 3 evening). */
    fun hourBucketOf(ts: Long): Int {
        val hour = ((ts / 3_600_000L) % 24L).toInt()
        return when {
            hour < 6 -> 0
            hour < 12 -> 1
            hour < 18 -> 2
            else -> 3
        }
    }

    /** The current time-of-day bucket. */
    fun currentHourBucket(): Int = hourBucketOf(System.currentTimeMillis())

    /** Coarse mood/genre cluster tags for a track from its title + artist keywords. */
    fun clustersFor(title: String, artist: String): Set<String> {
        val hay = (title + " " + artist).lowercase()
        if (hay.isBlank()) return emptySet()
        val out = LinkedHashSet<String>()
        CLUSTER_KEYWORDS.forEach { (cluster, keywords) ->
            if (keywords.any { hay.contains(it) }) out += cluster
        }
        return out
    }

    /** Push an artistKey onto the in-memory session ring (dropping the oldest past [SESSION_RING]). */
    private fun pushSessionArtist(artist: String) {
        if (artist.isBlank()) return
        synchronized(sessionLock) {
            sessionRing.remove(artist)
            sessionRing.addLast(artist)
            while (sessionRing.size > SESSION_RING) sessionRing.removeFirst()
        }
    }

    /** Last ~15 distinct artistKeys played this session (most recent last); never persisted. */
    fun recentSessionArtists(): List<String> =
        synchronized(sessionLock) { sessionRing.toList() }

    private fun load(ctx: Context): LinkedHashMap<String, Stats> {
        cache?.let { return it }
        synchronized(lock) {
            cache?.let { return it }
            val map = LinkedHashMap<String, Stats>()
            runCatching {
                val raw = ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).getString(KEY, null)
                if (raw != null) {
                    val json = JSONObject(raw)
                    for (k in json.keys()) {
                        val o = json.getJSONObject(k)
                        val buckets = IntArray(HOUR_BUCKETS)
                        o.optJSONArray("hb")?.let { arr ->
                            for (i in 0 until minOf(arr.length(), HOUR_BUCKETS)) buckets[i] = arr.optInt(i)
                        }
                        val clusters = LinkedHashSet<String>()
                        o.optJSONArray("cl")?.let { arr ->
                            for (i in 0 until arr.length()) arr.optString(i).takeIf { it.isNotBlank() }?.let { clusters += it }
                        }
                        map[k] = Stats(
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            plays = o.optInt("plays"),
                            completes = o.optInt("completes"),
                            skips = o.optInt("skips"),
                            lastTs = o.optLong("lastTs"),
                            hourBuckets = buckets,
                            clusters = clusters,
                        )
                    }
                }
            }
            cache = map
            return map
        }
    }

    private fun persist(ctx: Context, snapshot: Map<String, Stats>) {
        val json = JSONObject()
        snapshot.forEach { (k, s) ->
            json.put(k, JSONObject().apply {
                put("title", s.title); put("artist", s.artist)
                put("plays", s.plays); put("completes", s.completes)
                put("skips", s.skips); put("lastTs", s.lastTs)
                // Additive fields: only written when non-trivial so old-shaped
                // entries (and old readers, which ignore unknown keys) stay compatible.
                if (s.hourBuckets.any { it != 0 }) {
                    put("hb", org.json.JSONArray().apply { s.hourBuckets.forEach { put(it) } })
                }
                if (s.clusters.isNotEmpty()) {
                    put("cl", org.json.JSONArray().apply { s.clusters.forEach { put(it) } })
                }
            })
        }
        ctx.getSharedPreferences(PREF, Context.MODE_PRIVATE).edit().putString(KEY, json.toString()).apply()
    }

    /**
     * Records how a play ended. A stop before 30 s of a track longer than 45 s is a
     * skip; reaching 60 % of the track (or 4 minutes) is a complete; anything else
     * is a plain play.
     */
    fun recordOutcome(ctx: Context, key: String, title: String, artist: String, playedMs: Long, durationMs: Long) {
        if (key.isBlank() || title.isBlank()) return
        // Feed the in-memory session anti-fatigue ring on the caller's thread so it
        // reflects play order immediately, independent of the async persist below.
        pushSessionArtist(artistKey(artist.ifBlank { title }))
        val appCtx = ctx.applicationContext
        io.launch {
            val snapshot: Map<String, Stats>
            synchronized(lock) {
                val map = load(appCtx)
                val old = map.remove(key) ?: Stats(title, artist)
                val skip = playedMs < 30_000 && durationMs > 45_000
                val complete = durationMs > 0 && playedMs >= minOf((durationMs * 0.6).toLong(), 240_000L)
                val now = System.currentTimeMillis()
                // Count this play into the current time-of-day bucket (skips excluded,
                // since a skip is not a listen).
                val buckets = old.hourBuckets.copyOf(HOUR_BUCKETS)
                if (!skip) buckets[hourBucketOf(now)] += 1
                val clusters = old.clusters.ifEmpty { clustersFor(title, artist.ifBlank { old.artist }) }
                map[key] = old.copy(
                    title = title,
                    artist = artist.ifBlank { old.artist },
                    plays = old.plays + if (!skip && !complete) 1 else 0,
                    completes = old.completes + if (complete) 1 else 0,
                    skips = old.skips + if (skip) 1 else 0,
                    lastTs = now,
                    hourBuckets = buckets,
                    clusters = clusters,
                )
                // Insertion order == recency (removed and re-added above), so the
                // oldest entries are at the front.
                while (map.size > MAX_TRACKS) map.remove(map.keys.first())
                affinityCache = null
                snapshot = LinkedHashMap(map)
            }
            persist(appCtx, snapshot)
        }
    }

    /** Immutable copy of the stats taken under [lock], safe to iterate while IO writes. */
    private fun snapshot(ctx: Context): List<Stats> {
        val map = load(ctx)
        return synchronized(lock) { map.values.toList() }
    }

    /**
     * READ-ONLY aggregate for the Listening-stats screen (FEAT-003), derived from the existing
     * taste data with no new persistence and no change to [recordOutcome] or the JSON schema.
     * Everything here is computed from the same per-track counters the model already stores.
     */
    data class ListeningSummary(
        /** Distinct tracks the model has seen. */
        val trackCount: Int,
        /** Distinct artists seen. */
        val artistCount: Int,
        /** Total plays + completes across every track (a listen count, skips excluded). */
        val totalListens: Int,
        /** Total skips recorded. */
        val totalSkips: Int,
        /** Rough minutes listened: completes count ~3.5 min, plays ~2 min (no duration is stored). */
        val estimatedMinutes: Int,
        /** Current day streak of consecutive days (ending today) with at least one listen. */
        val dayStreak: Int,
        /** Top artists by affinity, strongest first, display spelling. */
        val topArtists: List<String>,
        /** Top tracks (title to artist) by that track's own affinity, strongest first. */
        val topTracks: List<Pair<String, String>>,
        /** The strongest mood/genre cluster right now, or null. */
        val strongestCluster: String?,
    )

    /** Builds a read-only [ListeningSummary] from the current snapshot. */
    fun listeningSummary(ctx: Context, topLimit: Int = 5): ListeningSummary {
        val tracks = snapshot(ctx)
        val listens = tracks.sumOf { it.plays + it.completes }
        val skips = tracks.sumOf { it.skips }
        val minutes = tracks.sumOf { (it.completes * 3.5 + it.plays * 2.0) }.toInt()
        val artists = tracks.mapNotNull { artistKey(it.artist).takeIf { k -> k.isNotBlank() } }.distinct().size
        val topTracks = tracks
            .filter { it.title.isNotBlank() }
            .sortedByDescending { trackAffinity(it) }
            .filter { trackAffinity(it) > 0f }
            .take(topLimit)
            .map { it.title to it.artist.substringBefore(",").removeSuffix(" - Topic").trim() }
        return ListeningSummary(
            trackCount = tracks.size,
            artistCount = artists,
            totalListens = listens,
            totalSkips = skips,
            estimatedMinutes = minutes,
            dayStreak = dayStreak(tracks),
            topArtists = topArtists(ctx, topLimit),
            topTracks = topTracks,
            strongestCluster = strongestCluster(ctx),
        )
    }

    /** Consecutive days (ending today) that have at least one recorded listen, from each track's [Stats.lastTs]. */
    private fun dayStreak(tracks: List<Stats>): Int {
        if (tracks.isEmpty()) return 0
        val days = tracks.map { (it.lastTs / DAY_MS.toLong()) }.filter { it > 0 }.toHashSet()
        if (days.isEmpty()) return 0
        var day = System.currentTimeMillis() / DAY_MS.toLong()
        var streak = 0
        while (days.contains(day)) {
            streak++
            day--
        }
        return streak
    }

    fun trackStats(ctx: Context, key: String): Stats? {
        val map = load(ctx)
        return synchronized(lock) { map[key] }
    }

    /** Whether any play of this artist has been recorded. */
    fun knowsArtist(ctx: Context, artist: String): Boolean {
        val a = artistKey(artist)
        return snapshot(ctx).any { artistKey(it.artist) == a }
    }

    /** Per-track preference in [-1, 1] from that track's own counters. */
    fun trackAffinity(stats: Stats?): Float {
        if (stats == null) return 0f
        val raw = 1.5 * stats.completes + 0.6 * stats.plays - 2.0 * stats.skips
        return tanh(raw / 4.0).toFloat()
    }

    /**
     * Artist affinity in [-1, 1]: complete +1.5, play +0.6, skip −2.0, liked track
     * +3.0, each decayed by exp(−age/21 days), squashed with tanh(sum / 6).
     * Cached for a minute since Home and radio ask for it repeatedly.
     */
    fun artistAffinity(ctx: Context): Map<String, Float> {
        val now = System.currentTimeMillis()
        affinityCache?.let { (at, map) -> if (now - at < 60_000) return map }
        val sums = HashMap<String, Double>()
        snapshot(ctx).forEach { s ->
            val a = artistKey(s.artist)
            if (a.isBlank()) return@forEach
            val decay = exp(-((now - s.lastTs).coerceAtLeast(0) / DAY_MS) / DECAY_DAYS)
            val w = 1.5 * s.completes + 0.6 * s.plays - 2.0 * s.skips
            sums[a] = (sums[a] ?: 0.0) + w * decay
        }
        runCatching { com.music.spotui.data.preferences.getLikedSongs(ctx) }.getOrDefault(emptyList())
            .forEach { song ->
                val a = artistKey(song.singer)
                if (a.isNotBlank()) sums[a] = (sums[a] ?: 0.0) + 3.0
            }
        val result = sums.mapValues { (_, v) -> tanh(v / 6.0).toFloat() }
        affinityCache = now to result
        return result
    }

    /**
     * Context-aware artist affinity: the base [artistAffinity] with a [BUCKET_BONUS]
     * added for artists whose plays fall in the given time-of-day [hourBucket], so a
     * morning mix leans to artists the user plays in the morning. Clamped to [-1, 1].
     * Falls back to the no-arg result when [hourBucket] is out of range.
     */
    fun artistAffinity(ctx: Context, hourBucket: Int): Map<String, Float> {
        val base = artistAffinity(ctx)
        if (hourBucket !in 0 until HOUR_BUCKETS) return base
        // Which artists have plays recorded in this bucket (weighted by share).
        val bucketShare = HashMap<String, Float>()
        snapshot(ctx).forEach { s ->
            val a = artistKey(s.artist)
            if (a.isBlank()) return@forEach
            val total = s.hourBuckets.sum()
            if (total <= 0) return@forEach
            val share = s.hourBuckets[hourBucket].toFloat() / total.toFloat()
            if (share > 0f) bucketShare[a] = maxOf(bucketShare[a] ?: 0f, share)
        }
        if (bucketShare.isEmpty()) return base
        val keys = base.keys + bucketShare.keys
        return keys.associateWith { a ->
            val v = (base[a] ?: 0f) + BUCKET_BONUS * (bucketShare[a] ?: 0f)
            v.coerceIn(-1f, 1f)
        }
    }

    /**
     * Mood/genre cluster affinity in [-1, 1] per cluster tag, from per-cluster
     * play/complete/skip counts aggregated across tracks with the 21-day recency
     * decay and tanh squash, mirroring [artistAffinity]'s weighting.
     */
    fun clusterAffinity(ctx: Context): Map<String, Float> {
        val now = System.currentTimeMillis()
        val sums = HashMap<String, Double>()
        snapshot(ctx).forEach { s ->
            if (s.clusters.isEmpty()) return@forEach
            val decay = exp(-((now - s.lastTs).coerceAtLeast(0) / DAY_MS) / DECAY_DAYS)
            val w = 1.5 * s.completes + 0.6 * s.plays - 2.0 * s.skips
            s.clusters.forEach { c -> sums[c] = (sums[c] ?: 0.0) + w * decay }
        }
        return sums.mapValues { (_, v) -> tanh(v / 6.0).toFloat() }
    }

    /** The cluster tag the user leans toward most right now, or null if none. */
    fun strongestCluster(ctx: Context): String? =
        clusterAffinity(ctx).entries.filter { it.value > 0f }.maxByOrNull { it.value }?.key

    /**
     * Artists with positive affinity, strongest first, in their display spelling.
     * When [hourBucket] is in range, uses the context-aware per-bucket affinity so
     * the result leans to artists the user plays at that time of day.
     */
    fun topArtists(ctx: Context, limit: Int, hourBucket: Int = -1): List<String> {
        val aff = if (hourBucket in 0 until HOUR_BUCKETS) artistAffinity(ctx, hourBucket) else artistAffinity(ctx)
        val display = HashMap<String, String>()
        snapshot(ctx).forEach { s ->
            val name = s.artist.substringBefore(",").removeSuffix(" - Topic").trim()
            if (name.isNotBlank()) display.putIfAbsent(artistKey(s.artist), name)
        }
        runCatching { com.music.spotui.data.preferences.getLikedSongs(ctx) }.getOrDefault(emptyList())
            .forEach { s ->
                val name = s.singer.substringBefore(",").removeSuffix(" - Topic").trim()
                if (name.isNotBlank()) display.putIfAbsent(artistKey(s.singer), name)
            }
        return aff.entries.filter { it.value > 0f }
            .sortedByDescending { it.value }
            .mapNotNull { display[it.key] }
            .take(limit)
    }
}
