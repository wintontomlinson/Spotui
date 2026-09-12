package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

/**
 * Cyan + Gold Edition — a premium dark surface ramp on a deep teal/cyan base.
 * Instead of one flat background these give depth: the base is a deep dark cyan,
 * cards sit one step lighter, and elevated sheets one step lighter again, which
 * makes the dark UI read as layered and premium rather than muddy. Gold is the
 * single accent so the cyan surfaces stay calm and the accent pops.
 */
val AppBackground = Color(0xFF041418)
val SurfaceElevated = Color(0xFF072A31)
val SurfaceCard = Color(0xFF0B3A44)
val Hairline = Color(0xFF12525E)

val GridBackground = Color(0xFF0B3A44)

/** Legacy alias kept so existing references compile; points at the real accent now. */
val AppPalette = Color(0xFFE8C24A)

/**
 * The single app accent — golden. Gold reads as premium/luxury on the deep cyan
 * surfaces, and unlike red it never gets confused with an error state.
 *
 * Gold is a light colour, so anything drawn ON TOP of an accent filled surface must
 * use [OnAccent] rather than white, otherwise the contrast is too low.
 */
val Accent = Color(0xFFE8C24A)

/** A deeper gold for pressed states and gradients. */
val AccentDark = Color(0xFFC79A2B)

/** Content colour for text and icons placed on top of [Accent]. */
val OnAccent = Color(0xFF042027)

/**
 * Cyan ramp for gradients / highlights (the surface family). Kept under the old
 * RoyalPurple* names so existing references keep compiling, but the values are
 * now cyan so any gradient built from them matches the new palette.
 */
val RoyalPurpleLight = Color(0xFF7FE7F0)
val RoyalPurple = Color(0xFF17B0C4)
val RoyalPurpleDeep = Color(0xFF0A5866)

/** Bright cyan highlight for glows / selected accents that shouldn't be gold. */
val CyanBright = Color(0xFF2CD4E6)
val CyanDeep = Color(0xFF0A5866)


/**
 * Shared app background — a top-to-bottom deep-cyan gradient (dark teal canvas →
 * near-black base). Used on the Home and Search roots so the cyan palette reads
 * as depth rather than a flat black wash.
 */
val AppBackgroundBrush: androidx.compose.ui.graphics.Brush =
    androidx.compose.ui.graphics.Brush.verticalGradient(
        colors = listOf(
            Color(0xFF08333B), // lifted dark cyan at the top (under the status bar)
            AppBackground,     // deep cyan base
            Color(0xFF020A0C), // near-black settle at the bottom
        ),
    )
