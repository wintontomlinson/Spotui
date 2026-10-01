package com.music.spotui.data.preferences

import android.content.Context

/**
 * Additive appearance / first-run preferences for SOLO v8 (Phase C features).
 *
 * All flags are additive with safe defaults so existing installs keep behaving exactly
 * as before: [hasOnboarded] defaults to false (so the one-time onboarding shows once),
 * and the accent choice defaults to [AccentChoice.AZURE] — the FEAT-002 Graphite & Azure
 * signature — so a fresh or upgraded install looks identical until the user picks another.
 *
 * These live in their own prefs file (not the audio `settings_prefs`) so appearance state
 * is self-contained and never collides with the audio/update keys.
 */

private const val PREF = "appearance_prefs"
private const val KEY_ONBOARDED = "has_onboarded"
private const val KEY_ACCENT = "accent_choice"

private fun prefs(c: Context) = c.getSharedPreferences(PREF, Context.MODE_PRIVATE)

/** True once the user has finished (or skipped) the first-run onboarding. */
fun hasOnboarded(c: Context): Boolean = prefs(c).getBoolean(KEY_ONBOARDED, false)

fun setOnboarded(c: Context, value: Boolean) =
    prefs(c).edit().putBoolean(KEY_ONBOARDED, value).apply()

/**
 * The user-chosen accent. Only the restrained azure/teal/indigo/steel family is offered —
 * the accent recolours the app's azure signalling (buttons, highlights, selected chips)
 * without touching the graphite canvas, so the design system stays disciplined.
 */
enum class AccentChoice(val label: String) {
    AZURE("Azure"),
    TEAL("Teal"),
    INDIGO("Indigo"),
    STEEL("Steel"),
}

/** Reads the saved accent, defaulting to Azure (the FEAT-002 signature). */
fun getAccentChoice(c: Context): AccentChoice =
    runCatching { AccentChoice.valueOf(prefs(c).getString(KEY_ACCENT, AccentChoice.AZURE.name)!!) }
        .getOrDefault(AccentChoice.AZURE)

fun setAccentChoice(c: Context, choice: AccentChoice) =
    prefs(c).edit().putString(KEY_ACCENT, choice.name).apply()

/**
 * The (soft, accent, deep) ramp each choice maps to. All four are cool, restrained hues in the
 * same family as the Graphite & Azure signature so the app stays disciplined whichever is picked.
 * Azure is the FEAT-002 default; Teal leans cyan-green, Indigo a cooler violet-blue, Steel a
 * near-monochrome slate for the most understated look.
 */
fun AccentChoice.ramp(): Triple<androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color, androidx.compose.ui.graphics.Color> {
    fun c(v: Long) = androidx.compose.ui.graphics.Color(v)
    return when (this) {
        AccentChoice.AZURE -> Triple(c(0xFF93C5FD), c(0xFF3B82F6), c(0xFF1D4ED8))
        AccentChoice.TEAL -> Triple(c(0xFF7FE0D4), c(0xFF2DBDAE), c(0xFF14807A))
        AccentChoice.INDIGO -> Triple(c(0xFFB0A5FB), c(0xFF7C6CF0), c(0xFF4C3FC2))
        AccentChoice.STEEL -> Triple(c(0xFFCBD5E1), c(0xFF8C9AAC), c(0xFF55606E))
    }
}

/** Pushes [choice] into the live theme accent so the whole app recolours. */
fun applyAccent(choice: AccentChoice) {
    val (soft, accent, deep) = choice.ramp()
    com.music.spotui.ui.theme.AccentState.set(soft, accent, deep)
}

/** Reads the saved accent and applies it to the live theme (call once at app start). */
fun applySavedAccent(c: Context) = applyAccent(getAccentChoice(c))
