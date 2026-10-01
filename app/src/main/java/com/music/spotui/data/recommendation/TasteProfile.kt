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
 * most recently touched tracks. Reads use an in-memory copy; writes go to IO.
 */
object TasteProfile {

    data class Stats(
        val title: String,
        val artist: String,
        val plays: Int = 0,
        val completes: Int = 0,
        val skips: Int = 0,
        val lastTs: Long = 0L,
    )

    private const val PREF = "TasteProfile"
    private const val KEY = "tracks"
    private const val MAX_TRACKS = 800
    private const val DECAY_DAYS = 21.0
    private const val DAY_MS = 86_400_000.0

    private val io = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val lock = Any()
    @Volatile private var cache: LinkedHashMap<String, Stats>? = null
    @Volatile private var affinityCache: Pair<Long, Map<String, Float>>? = null

    /** Lower-cased first artist without the YouTube " - Topic" suffix. */
    fun artistKey(singer: String): String =
        singer.substringBefore(",").trim().removeSuffix(" - Topic").trim().lowercase()

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
                        map[k] = Stats(
                            title = o.optString("title"),
                            artist = o.optString("artist"),
                            plays = o.optInt("plays"),
                            completes = o.optInt("completes"),
                            skips = o.optInt("skips"),
                            lastTs = o.optLong("lastTs"),
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
        val appCtx = ctx.applicationContext
        io.launch {
            val snapshot: Map<String, Stats>
            synchronized(lock) {
                val map = load(appCtx)
                val old = map.remove(key) ?: Stats(title, artist)
                val skip = playedMs < 30_000 && durationMs > 45_000
                val complete = durationMs > 0 && playedMs >= minOf((durationMs * 0.6).toLong(), 240_000L)
                map[key] = old.copy(
                    title = title,
                    artist = artist.ifBlank { old.artist },
                    plays = old.plays + if (!skip && !complete) 1 else 0,
                    completes = old.completes + if (complete) 1 else 0,
                    skips = old.skips + if (skip) 1 else 0,
                    lastTs = System.currentTimeMillis(),
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

    fun trackStats(ctx: Context, key: String): Stats? = load(ctx)[key]

    /** Whether any play of this artist has been recorded. */
    fun knowsArtist(ctx: Context, artist: String): Boolean {
        val a = artistKey(artist)
        return load(ctx).values.any { artistKey(it.artist) == a }
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
        load(ctx).values.forEach { s ->
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

    /** Artists with positive affinity, strongest first, in their display spelling. */
    fun topArtists(ctx: Context, limit: Int): List<String> {
        val aff = artistAffinity(ctx)
        val display = HashMap<String, String>()
        load(ctx).values.forEach { s ->
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
