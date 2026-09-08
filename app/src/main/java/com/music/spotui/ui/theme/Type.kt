package com.music.spotui.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.music.spotui.R

/**
 * The genuine Spotify fonts, extracted from the official app.
 * SpotifyMixUI, body / UI text. SpotifyMixUITitle, headings (heavier display cut).
 * Medium/SemiBold don't exist as cuts, so they map to the nearest available weight.
 */
val SpotifyMix = FontFamily(
    Font(R.font.spotify_mix_ui_regular, weight = FontWeight.Normal),
    Font(R.font.spotify_mix_ui_regular, weight = FontWeight.Medium),
    Font(R.font.spotify_mix_ui_bold, weight = FontWeight.SemiBold),
    Font(R.font.spotify_mix_ui_bold, weight = FontWeight.Bold),
)

val SpotifyMixTitle = FontFamily(
    Font(R.font.spotify_mix_ui_title_bold, weight = FontWeight.Bold),
    Font(R.font.spotify_mix_ui_title_extrabold, weight = FontWeight.Black),
)

// Kept as an alias so existing references (Theme.kt etc.) don't break.
val Montserrat = SpotifyMix

private val base = TextStyle(fontFamily = SpotifyMix)
private val title = TextStyle(fontFamily = SpotifyMixTitle, fontWeight = FontWeight.Bold)

// Tighter tracking on the big display/title cuts, which is what makes large headings
// look intentional and premium rather than loose. Body text keeps a hair of negative
// tracking for density without hurting legibility.
val Typography = Typography(
    displayLarge = title.copy(letterSpacing = (-0.5).sp),
    displayMedium = title.copy(letterSpacing = (-0.5).sp),
    displaySmall = title.copy(letterSpacing = (-0.4).sp),
    headlineLarge = title.copy(letterSpacing = (-0.4).sp),
    headlineMedium = title.copy(letterSpacing = (-0.3).sp),
    headlineSmall = title.copy(letterSpacing = (-0.3).sp),
    titleLarge = base.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.3).sp),
    titleMedium = base.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    titleSmall = base.copy(fontWeight = FontWeight.Bold, letterSpacing = (-0.2).sp),
    bodyLarge = base.copy(fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = (-0.2).sp),
    bodyMedium = base.copy(letterSpacing = (-0.1).sp),
    bodySmall = base,
    labelLarge = base, labelMedium = base, labelSmall = base,
)
