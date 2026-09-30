package com.music.spotui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.spotui.R

/**
 * Display face: Fraunces (SIL OFL 1.1), a soft-terminal editorial serif used for the
 * wordmark, screen titles and section headings. Shipped as a static SemiBold instance
 * (opsz 72, SOFT 50, WONK 0) cut from the variable font; see licenses/fonts.
 */
val SoloDisplay = FontFamily(
    Font(R.font.fraunces_semibold, weight = FontWeight.SemiBold),
)

/** UI face: Manrope (SIL OFL 1.1), a calm geometric sans for everything else. */
val SoloSans = FontFamily(
    Font(R.font.manrope_regular, weight = FontWeight.Normal),
    Font(R.font.manrope_medium, weight = FontWeight.Medium),
    Font(R.font.manrope_semibold, weight = FontWeight.SemiBold),
    Font(R.font.manrope_bold, weight = FontWeight.Bold),
    Font(R.font.manrope_extrabold, weight = FontWeight.ExtraBold),
)

private val display = TextStyle(fontFamily = SoloDisplay, fontWeight = FontWeight.SemiBold)
private val sans = TextStyle(fontFamily = SoloSans)

val Typography = Typography(
    displayLarge = display.copy(fontSize = 44.sp, lineHeight = 50.sp, letterSpacing = (-0.6).sp),
    displayMedium = display.copy(fontSize = 36.sp, lineHeight = 42.sp, letterSpacing = (-0.5).sp),
    displaySmall = display.copy(fontSize = 30.sp, lineHeight = 36.sp, letterSpacing = (-0.4).sp),
    headlineLarge = display.copy(fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.4).sp),
    headlineMedium = display.copy(fontSize = 24.sp, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    headlineSmall = display.copy(fontSize = 22.sp, lineHeight = 28.sp, letterSpacing = (-0.2).sp),
    titleLarge = sans.copy(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    titleMedium = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = (-0.1).sp),
    titleSmall = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = sans.copy(fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = sans.copy(fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = sans.copy(fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = sans.copy(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.2.sp),
    labelSmall = sans.copy(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 14.sp, letterSpacing = 0.6.sp),
)
