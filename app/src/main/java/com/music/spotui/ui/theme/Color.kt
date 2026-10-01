package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/*
 * Solo design tokens: Aurora Noir.
 *
 * A warm obsidian canvas, a layered surface ramp (each step one visible notch lighter)
 * and one electric lime-citron "Volt" signature accent. Volt is premium and high-energy,
 * and it stays clear of every competitor's brand hue: it sits in a different hue family
 * (~75deg lime-citron) from Spotify's grass green (~141deg) and avoids red, orange, pink,
 * purple and teal/cyan, so it reads as its own brand rather than a green fill.
 *
 * Contrast (WCAG): Accent #D8FF3E on Canvas #0A0A0C 17.23:1; OnAccent #0C1400 on Accent 16.40:1,
 * on AccentDeep 12.38:1; TextTertiary #7F837A on Canvas 5.11:1.
 *
 * Screens read colours from here instead of hard-coding hex values, so the whole app stays
 * on one palette.
 */

// Canvas and surface ramp
val Canvas = Color(0xFF0A0A0C)     // app canvas
val Surface1 = Color(0xFF121214)   // bars, grouped rows
val Surface2 = Color(0xFF17181C)   // cards, sheets, dialogs
val Surface3 = Color(0xFF202126)   // inputs, chips, icon wells
val Surface4 = Color(0xFF2B2D34)   // pressed / selected
val Elevated = Color(0xFF1E2416)   // hero plate, brand crown

// Volt accent ramp (lime -> volt -> spring)
val AccentSoft = Color(0xFFE8FF8F)
val Accent = Color(0xFFD8FF3E)     // signature Volt: text, icons and fills
val AccentDeep = Color(0xFF9BE64B)
val OnAccent = Color(0xFF0C1400)   // content drawn on accent fills

// Text
val TextPrimary = Color(0xFFF4F5F2)
val TextSecondary = Color(0xFFADB0A8)
val TextTertiary = Color(0xFF7F837A)
val TextDisabled = Color(0xFF4C4F49)

// Lines
val Hairline = Color(0x14F4F5F2)        // 8% white: dividers, card edges
val HairlineAccent = Color(0x38D8FF3E)  // 22% Volt: premium edges on hero surfaces

// Status
val Danger = Color(0xFFFF6B6B)
val DangerSurface = Color(0xFF3A1A1A)
val Success = Color(0xFF5BD6A0)
val Warning = Color(0xFFFFCB5C)

// Scrim behind sheets and dialogs (70% black)
val Scrim = Color(0xB3000000)

// Shadow for floating chrome (mini player, nav): 40% black
val Shadow = Color(0x66000000)

// Glass (translucent chrome over scrolling content)
val GlassFill = Color(0xD9121214)
val GlassFillStrong = Color(0xF2121214)

/** Shared screen background: a faint warm glow at the top that settles into the obsidian canvas. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF141A0E),
    0.4f to Canvas,
    1f to Color(0xFF060607),
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
