package com.music.spotui.ui.theme

/**
 * Central palette for the SOLO design system.
 *
 * Every colour the app uses is defined here as a named token (e.g. [Canvas], [Surface1],
 * [Accent], [TextPrimary]) and consumed by ~28 UI files plus [Theme]'s Material scheme. Changing
 * a token *body* while keeping its *name* re-skins the whole app in one place. Also exposes a few
 * ready-made brushes ([AppBackgroundBrush], [AccentBrush]) and the [artworkTone] helper used to
 * derive a surface tint from album art.
 */

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/*
 * Solo design tokens: Lumen Indigo.
 *
 * A deep indigo-blue canvas with a layered surface ramp (each step one visible notch
 * lighter, drifting from near-black indigo into cool slate) and a two-tone signature
 * accent: a vivid "Lumen" violet that refracts into a cyan-teal highlight, with a warm
 * gold reserved for premium beam-tips and highlights. The pairing reads as a prism of
 * light — a single beam fanning into a spectrum — which ties to the SOLO "one voice,
 * pure sound" idea and keeps the brand clearly its own.
 *
 * Competitor avoidance: violet (~255deg) + cyan (~175deg) + gold sits well clear of
 * Spotify grass green (~141deg), YouTube Music / Netflix red, SoundCloud orange,
 * Apple Music pink-magenta, Tidal/Deezer near-monochrome, Amazon Music teal-on-navy,
 * JioSaavn/Gaana/Wynk reds and greens. The indigo-violet-cyan prism is a distinct
 * hue family rather than a borrowed brand hue.
 *
 * Contrast (WCAG, approx): Accent #7C5CFF on Canvas #0B0B14 ~5.0:1; AccentSoft #B79CFF
 * on Canvas ~8.1:1; TextPrimary #F3F2FA ~17:1; TextSecondary #AEADC2 ~8.6:1;
 * TextTertiary #8181A0 ~4.6:1 (>=4.5:1 on Canvas).
 *
 * Screens read colours from here instead of hard-coding hex values, so the whole app stays
 * on one palette.
 */

// Canvas and surface ramp (near-black indigo -> cool slate)
val Canvas = Color(0xFF0B0B14)     // app canvas
val Surface1 = Color(0xFF13131F)   // bars, grouped rows
val Surface2 = Color(0xFF191926)   // cards, sheets, dialogs
val Surface3 = Color(0xFF222235)   // inputs, chips, icon wells
val Surface4 = Color(0xFF2D2D45)   // pressed / selected
val Elevated = Color(0xFF1A1630)   // hero plate, brand crown

// Lumen accent ramp (violet -> deep refract into cyan)
val AccentSoft = Color(0xFFB79CFF)
val Accent = Color(0xFF7C5CFF)     // signature Lumen violet: text, icons and fills
val AccentDeep = Color(0xFF3FE0D0) // cyan-teal refraction
val OnAccent = Color(0xFF0A0714)   // content drawn on accent fills

// Premium gold (beam-tip highlights, special surfaces)
val Gold = Color(0xFFF5C97A)

// Text
val TextPrimary = Color(0xFFF3F2FA)
val TextSecondary = Color(0xFFAEADC2)
val TextTertiary = Color(0xFF8181A0)
val TextDisabled = Color(0xFF4D4D63)

// Lines
val Hairline = Color(0x14F3F2FA)        // 8% white: dividers, card edges
val HairlineAccent = Color(0x387C5CFF)  // 22% Lumen violet: premium edges on hero surfaces

// Status
val Danger = Color(0xFFFF6B6B)
val DangerSurface = Color(0xFF3A1A1A)
val Success = Color(0xFF48D7A4)
val Warning = Color(0xFFF5C97A)

// Scrim behind sheets and dialogs (70% black)
val Scrim = Color(0xB3000000)

// Shadow for floating chrome (mini player, nav): 40% black
val Shadow = Color(0x66000000)

// Glass (translucent chrome over scrolling content)
val GlassFill = Color(0xD913131F)
val GlassFillStrong = Color(0xF213131F)

/** Shared screen background: a faint violet glow at the top that settles into the indigo canvas. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF171231),
    0.4f to Canvas,
    1f to Color(0xFF07070E),
)

/** The accent gradient used for primary actions (play buttons, the Liked tile): violet -> cyan. */
val AccentBrush: Brush = Brush.linearGradient(listOf(AccentSoft, Accent, AccentDeep))

/**
 * Keeps an artwork-derived colour rich but dark enough for light text on top
 * (saturation <= 0.6, lightness 0.12..0.24), so tinted player and album backgrounds
 * never wash out or clash with the accent on the darker indigo canvas.
 */
fun artworkTone(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtMost(0.60f)
    hsl[2] = hsl[2].coerceIn(0.12f, 0.24f)
    return Color(ColorUtils.HSLToColor(hsl))
}
