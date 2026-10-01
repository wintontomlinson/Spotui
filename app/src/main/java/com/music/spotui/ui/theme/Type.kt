package com.music.spotui.ui.theme

/**
 * SOLO typography scale.
 *
 * Defines the font families (Space Grotesk for display/headings, Plus Jakarta Sans for body) and
 * the Material 3 [Typography] text styles used across the app.
 */

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.spotui.R

/**
 * Display face: Space Grotesk (SIL OFL 1.1), a distinctive geometric grotesk used for display,
 * headline and titleLarge styles, the wordmark and the player title. Static Medium, SemiBold
 * and Bold instances cut from the variable font; see licenses/fonts.
 */
val SoloDisplay = FontFamily(
    Font(R.font.space_grotesk_medium, weight = FontWeight.Medium),
    Font(R.font.space_grotesk_semibold, weight = FontWeight.SemiBold),
    Font(R.font.space_grotesk_bold, weight = FontWeight.Bold),
)

/**
 * UI face: Plus Jakarta Sans (SIL OFL 1.1), a warm humanist sans used for everything else.
 * Static Regular, Medium, SemiBold and Bold instances cut from the variable font.
 */
val SoloSans = FontFamily(
    Font(R.font.jakarta_regular, weight = FontWeight.Normal),
    Font(R.font.jakarta_medium, weight = FontWeight.Medium),
    Font(R.font.jakarta_semibold, weight = FontWeight.SemiBold),
    Font(R.font.jakarta_bold, weight = FontWeight.Bold),
)

private val display = TextStyle(fontFamily = SoloDisplay, fontWeight = FontWeight.SemiBold)
private val sans = TextStyle(fontFamily = SoloSans)

val Typography = Typography(
    displayLarge = display.copy(fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.8).sp),
    displayMedium = display.copy(fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.6).sp),
    displaySmall = display.copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineLarge = display.copy(fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = display.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    headlineSmall = display.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.3).sp),
    titleLarge = display.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = sans.copy(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = sans.copy(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = sans.copy(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = sans.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)
