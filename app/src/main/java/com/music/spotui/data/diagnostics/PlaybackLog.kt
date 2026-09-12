package com.music.spotui.data.diagnostics

import android.os.SystemClock
import java.util.ArrayDeque

/**
 * A small in memory record of what playback actually did.
 *
 * When a song stops partway through there is no way to tell from the outside whether the
 * stream was cut short, the host rejected a request, the player reported a wrong length,
 * or the queue was advanced deliberately. Every one of those looks identical to the
 * listener. This keeps the last few hundred events so the answer can be read out of
 * Settings instead of guessed at.
 *
 * Entries are kept in memory only. Playback writes to this from the loading thread, so
 * nothing here is allowed to touch storage.
 */
object PlaybackLog {
    private const val MAX_ENTRIES = 400

    private val entries = ArrayDeque<String>()
    private var startedAt = 0L

    /** Records one event. [tag] groups related lines, [message] should carry the numbers. */
    @Synchronized
    fun add(tag: String, message: String) {
        if (startedAt == 0L) startedAt = SystemClock.elapsedRealtime()
        val seconds = (SystemClock.elapsedRealtime() - startedAt) / 1000.0
        if (entries.size >= MAX_ENTRIES) entries.removeFirst()
        entries.addLast(String.format("[%7.2fs] %-9s %s", seconds, tag, message))
    }

    /** Newest first, which is what matters when reading this on a phone. */
    @Synchronized
    fun lines(): List<String> = entries.toList().asReversed()

    @Synchronized
    fun asText(): String = lines().joinToString("\n")

    @Synchronized
    fun isEmpty(): Boolean = entries.isEmpty()

    @Synchronized
    fun clear() {
        entries.clear()
        startedAt = 0L
    }
}
