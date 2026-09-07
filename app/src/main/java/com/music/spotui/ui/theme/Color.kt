package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)
val AppBackground = Color(0xFF0B0B0F)
val GridBackground = Color(0xFF2A2A2A)
val AppPalette = Color(0xFF618DFF)

/**
 * The single app accent. Amber reads as premium on the near black surfaces, and
 * unlike red it never gets confused with an error state.
 *
 * Amber is a light colour, so anything drawn ON TOP of an accent filled surface
 * must use [OnAccent] rather than white, otherwise the contrast is too low.
 */
val Accent = Color(0xFFF5A524)

/** A deeper amber for pressed states and gradients. */
val AccentDark = Color(0xFFD98E0B)

/** Content colour for text and icons placed on top of [Accent]. */
val OnAccent = Color(0xFF1A1206)