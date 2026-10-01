package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/*
 * Sonvra design tokens: Midnight Azure.
 *
 * A graphite-midnight canvas, a layered surface ramp (each step one visible notch lighter)
 * and one cool, vivid azure signature accent. The azure is premium and calm, and it stays
 * clear of every competitor's brand hue (green, red, orange, pink-red, purple, teal/cyan).
 *
 * Contrast (WCAG): Accent on Canvas 7.2:1, on Surface2 6.3:1; OnAccent on Accent 6.7:1,
 * on AccentDeep 4.9:1; TextTertiary on Canvas 5.4:1, on Surface2 4.8:1.
 *
 * Screens read colours from here instead of hard-coding hex values, so the whole app stays
 * on one palette.
 */

// Canvas and surface ramp
val Canvas = Color(0xFF07090D)     // app canvas
val Surface1 = Color(0xFF0E1117)   // bars, grouped rows
val Surface2 = Color(0xFF151922)   // cards, sheets, dialogs
val Surface3 = Color(0xFF1C212C)   // inputs, chips, icon wells
val Surface4 = Color(0xFF262C3A)   // pressed / selected
val Midnight = Color(0xFF0F1A33)   // brand plate, gradient crown

// Azure accent ramp
val AccentSoft = Color(0xFF9CCBFF)
val Accent = Color(0xFF5B9BFF)     // signature azure: text, icons and fills
val AccentDeep = Color(0xFF3F7BFF)
val OnAccent = Color(0xFF06122A)   // content drawn on accent fills

// Text
val TextPrimary = Color(0xFFF2F5FA)
val TextSecondary = Color(0xFFA6AFBF)
val TextTertiary = Color(0xFF7D8698)
val TextDisabled = Color(0xFF4A5263)

// Lines
val Hairline = Color(0x14F2F5FA)        // 8% white: dividers, card edges
val HairlineAccent = Color(0x335B9BFF)  // 20% azure: premium edges on hero surfaces

// Status
val Danger = Color(0xFFFF6B7A)
val DangerSurface = Color(0xFF3A1720)
val Success = Color(0xFF4FD1A1)
val Warning = Color(0xFFFFC15C)

// Scrim behind sheets and dialogs (70% black)
val Scrim = Color(0xB3000000)

// Shadow for floating chrome (mini player, nav): 40% black
val Shadow = Color(0x66000000)

// Glass (translucent chrome over scrolling content)
val GlassFill = Color(0xD90E1117)
val GlassFillStrong = Color(0xF20E1117)

/** Shared screen background: a faint midnight-blue glow at the top that settles into the canvas. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF0E1A33),
    0.42f to Canvas,
    1f to Color(0xFF05070A),
)

/** The accent gradient used for primary actions (play buttons, the Liked tile). */
val AccentBrush: Brush = Brush.linearGradient(listOf(AccentSoft, Accent, AccentDeep))

/**
 * Keeps an artwork-derived colour rich but dark enough for light text on top
 * (saturation <= 0.55, lightness 0.14..0.26), so tinted player and album backgrounds
 * never wash out or clash with the accent.
 */
fun artworkTone(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtMost(0.55f)
    hsl[2] = hsl[2].coerceIn(0.14f, 0.26f)
    return Color(ColorUtils.HSLToColor(hsl))
}
