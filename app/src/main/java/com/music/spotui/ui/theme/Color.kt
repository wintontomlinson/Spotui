package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
/**
 * Royal Edition — premium dark surface ramp on a deep royal-purple base. Instead of one
 * flat background these give depth: the base is a deep royal purple, cards sit one step
 * lighter, and elevated sheets one step lighter again, which is what makes the dark UI
 * read as layered and premium rather than muddy.
 */
val AppBackground = Color(0xFF130824)
val SurfaceElevated = Color(0xFF1A0F30)
val SurfaceCard = Color(0xFF241540)
val Hairline = Color(0xFF3A2A5C)

val GridBackground = Color(0xFF241540)

/** Legacy alias kept so existing references compile; points at the real accent now. */
val AppPalette = Color(0xFFD4AF37)

/**
 * The single app accent — royal gold. Gold reads as premium/luxury on the deep royal
 * purple surfaces, and unlike red it never gets confused with an error state.
 *
 * Gold is a light colour, so anything drawn ON TOP of an accent filled surface must use
 * [OnAccent] rather than white, otherwise the contrast is too low.
 */
val Accent = Color(0xFFD4AF37)

/** A deeper gold for pressed states and gradients. */
val AccentDark = Color(0xFFB8892B)

/** Content colour for text and icons placed on top of [Accent]. */
val OnAccent = Color(0xFF241540)

/** Royal-purple ramp for gradients / highlights. */
val RoyalPurpleLight = Color(0xFFC4B5FD)
val RoyalPurple = Color(0xFF7C3AED)
val RoyalPurpleDeep = Color(0xFF4C1D95)