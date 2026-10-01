package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/*
 * Solo design tokens: "Obsidian Aurora".
 *
 * An obsidian canvas, a layered surface ramp (each step one visible notch lighter), a
 * single apricot-to-rose "aurora" accent and ivory text. Token names are kept from 3.0
 * (Gold*, Velvet, …) so every screen restyles from here. Screens read colours from here
 * instead of hard-coding hex values, so the whole app stays on one palette.
 */

// Canvas and surface ramp
val Ink = Color(0xFF08070C)        // app canvas
val Night = Color(0xFF110F18)      // surface 1: bars, grouped rows
val Dusk = Color(0xFF181522)       // surface 2: cards, sheets, dialogs
val Velvet = Color(0xFF211D2E)     // surface 3: inputs, chips, icon wells
val Amethyst = Color(0xFF2B2640)   // surface 4: selected / pressed
val RoyalPlum = Color(0xFF1D1636)  // brand plate, gradient crown
val RoyalViolet = Color(0xFF5B4BD6)
val Lilac = Color(0xFFAEB6FF)      // secondary accent (status, info)

// Aurora accent ramp (names kept from the gold era): light apricot, apricot, rose
val GoldLight = Color(0xFFFFE3C2)
val Gold = Color(0xFFFFAE70)
val GoldDeep = Color(0xFFE2566E)
val OnGold = Color(0xFF1A0D06)     // content drawn on top of gold

// Text (all pass WCAG AA on Ink through Velvet)
val Ivory = Color(0xFFF6F2EC)
val TextSecondary = Color(0xFFB9B0C4)
val TextTertiary = Color(0xFF908799)
val TextDisabled = Color(0xFF5A5366)

// Lines
val HairlineSoft = Color(0x14F5F0E6)   // 8% ivory: dividers, card edges
val HairlineGold = Color(0x2EFFAE70)   // 18% apricot: premium edges on hero surfaces

// Status
val Danger = Color(0xFFF07B7B)
val DangerSurface = Color(0xFF4A1F2A)
val Success = Color(0xFF7FD1A3)
val Warning = Color(0xFFF2C46B)

// Scrim behind sheets and dialogs (70% black)
val Scrim = Color(0xB3000000)

// Shadow for floating chrome (mini player, nav): 35% deep ink
val ShadowInk = Color(0x5905030A)

// Glass (translucent chrome over scrolling content)
val GlassFill = Color(0xD9110F18)
val GlassFillStrong = Color(0xF2110F18)

/** Shared screen background: a soft violet glow at the top that settles into obsidian. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF1D1636),
    0.42f to Ink,
    1f to Color(0xFF060509),
)

/** The accent gradient used for primary actions (play buttons, the Liked tile). */
val AuroraBrush: Brush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep))
val GoldBrush: Brush = AuroraBrush

/**
 * Keeps an artwork-derived colour rich but dark enough for ivory text on top
 * (saturation <= 0.55, lightness 0.14..0.26), so tinted player and album backgrounds
 * never wash out or clash with the gold accent.
 */
fun artworkTone(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtMost(0.55f)
    hsl[2] = hsl[2].coerceIn(0.14f, 0.26f)
    return Color(ColorUtils.HSLToColor(hsl))
}

// Compatibility names used across older screens. They all resolve to Solo tokens.
val AppBackground = Ink
val AppPalette = Gold
val Accent = Gold
val OnAccent = OnGold
