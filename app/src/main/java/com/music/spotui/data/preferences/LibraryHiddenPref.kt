package com.music.spotui.data.preferences

import android.content.Context

/**
 * Library entries the user has removed.
 *
 * Some rows are not stored anywhere that can simply be deleted: album entries are derived
 * from the listening history, so deleting the row would only bring it back on the next
 * rebuild. Recording the removal instead makes "Remove from library" stick for every kind
 * of row, whatever it is backed by.
 */
object LibraryHiddenPref {

    private const val PREF = "HiddenLibraryEntries"

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREF, Context.MODE_PRIVATE)

    fun hide(context: Context, id: String) {
        if (id.isBlank()) return
        prefs(context).edit().putBoolean(id, true).apply()
    }

    fun unhide(context: Context, id: String) {
        prefs(context).edit().remove(id).apply()
    }

    fun hiddenIds(context: Context): Set<String> = prefs(context).all.keys

    fun isHidden(context: Context, id: String): Boolean = prefs(context).contains(id)

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }
}
