package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
/**
 * Royal Edition — premium dark surface ramp on a deep ROYAL BLUE base (navy, not
 * purple), with gold accents. Instead of one flat background these give depth: the
 * base is a deep royal navy, cards sit one step lighter, and elevated sheets one
 * step lighter again, so the dark UI reads as layered and premium rather than muddy.
 */
val AppBackground = Color(0xFF0A1128)   // deep royal navy
val SurfaceElevated = Color(0xFF101A3A)
val SurfaceCard = Color(0xFF16224A)
val Hairline = Color(0xFF243056)

val GridBackground = Color(0xFF16224A)

/** Legacy alias kept so existing references compile; points at the real accent now. */
val AppPalette = Color(0xFFE8C15A)

/**
 * The single app accent — royal gold. Gold reads as premium/luxury on the deep royal
 * navy surfaces, and unlike red it never gets confused with an error state.
 *
 * Gold is a light colour, so anything drawn ON TOP of an accent filled surface must use
 * [OnAccent] rather than white, otherwise the contrast is too low.
 */
val Accent = Color(0xFFE8C15A)          // warm royal gold

/** A deeper gold for pressed states and gradients. */
val AccentDark = Color(0xFFB8892B)

/** Content colour for text and icons placed on top of [Accent]. */
val OnAccent = Color(0xFF0A1128)

/** Royal-blue ramp for gradients / highlights (replaces the old purple ramp). */
val RoyalPurpleLight = Color(0xFF7FA0E0)   // light royal blue
val RoyalPurple = Color(0xFF1E3A8C)        // royal blue
val RoyalPurpleDeep = Color(0xFF12235C)    // deep royal blue