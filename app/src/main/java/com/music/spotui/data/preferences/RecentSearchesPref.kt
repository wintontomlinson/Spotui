package com.music.spotui.data.preferences

import android.content.Context
import org.json.JSONArray

/**
 * Recent search queries, newest first. Backed by SharedPreferences so the list
 * survives process death, and capped so it never grows without bound.
 */
private const val PREF = "RecentSearches"
private const val KEY = "queries"
private const val MAX_ENTRIES = 12

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

fun getRecentSearches(context: Context): List<String> {
    val raw = prefs(context).getString(KEY, null) ?: return emptyList()
    return runCatching {
        val arr = JSONArray(raw)
        (0 until arr.length()).mapNotNull { arr.optString(it).takeIf { s -> s.isNotBlank() } }
    }.getOrDefault(emptyList())
}

private fun save(context: Context, queries: List<String>) {
    val arr = JSONArray().also { a -> queries.forEach { a.put(it) } }
    prefs(context).edit().putString(KEY, arr.toString()).apply()
}

/** Adds a query to the top, removing any earlier duplicate (case insensitive). */
fun addRecentSearch(context: Context, query: String) {
    val q = query.trim()
    if (q.isBlank()) return
    val existing = getRecentSearches(context).filterNot { it.equals(q, ignoreCase = true) }
    save(context, (listOf(q) + existing).take(MAX_ENTRIES))
}

fun removeRecentSearch(context: Context, query: String) {
    save(context, getRecentSearches(context).filterNot { it.equals(query, ignoreCase = true) })
}

fun clearRecentSearches(context: Context) {
    prefs(context).edit().remove(KEY).apply()
}
