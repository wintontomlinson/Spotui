package com.music.spotui.data.preferences

import android.content.Context

/**
 * Local storage for the Deezer account: the ARL cookie captured at login, the
 * detected account tier (for display), and whether Deezer is used as a source.
 */
private const val PREF = "Deezer"
private const val KEY_ARL = "arl"
private const val KEY_TIER = "tier"

private fun prefs(context: Context) =
    context.getSharedPreferences(PREF, Context.MODE_PRIVATE)


fun getDeezerArl(context: Context): String? =
    prefs(context).getString(KEY_ARL, null)?.takeIf { it.isNotBlank() }





fun setDeezerTier(context: Context, tier: String) {
    prefs(context).edit().putString(KEY_TIER, tier).apply()
}
