package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.graphics.ColorUtils

/*
 * Solo design tokens: "Midnight Velvet & Gold".
 *
 * A plum-black canvas, a layered velvet surface ramp (each step one visible notch
 * lighter), a single warm-gold accent and ivory text. Screens read colours from here
 * instead of hard-coding hex values, so the whole app stays on one palette.
 */

// Canvas and surface ramp
val Ink = Color(0xFF0C0912)        // app canvas
val Night = Color(0xFF141020)      // surface 1: bars, grouped rows
val Dusk = Color(0xFF1B1529)       // surface 2: cards, sheets, dialogs
val Velvet = Color(0xFF241C35)     // surface 3: inputs, chips, icon wells
val Amethyst = Color(0xFF2E2442)   // surface 4: selected / pressed
val RoyalPlum = Color(0xFF2B1A4D)  // brand plate, gradient crown
val RoyalViolet = Color(0xFF6B4BB8)
val Lilac = Color(0xFFBFA8E8)      // secondary accent (status, info)

// Gold accent ramp
val GoldLight = Color(0xFFFBEFD0)
val Gold = Color(0xFFE6C27A)
val GoldDeep = Color(0xFFB8893A)
val OnGold = Color(0xFF1A1206)     // content drawn on top of gold

// Text (all pass WCAG AA on Ink through Velvet)
val Ivory = Color(0xFFF5F0E6)
val TextSecondary = Color(0xFFB9B0C4)
val TextTertiary = Color(0xFF908799)
val TextDisabled = Color(0xFF5A5366)

// Lines
val HairlineSoft = Color(0x14F5F0E6)   // 8% ivory: dividers, card edges
val HairlineGold = Color(0x2EE6C27A)   // 18% gold: premium edges on hero surfaces

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
val GlassFill = Color(0xD9141020)
val GlassFillStrong = Color(0xF2141020)

/** Shared screen background: a royal-plum crown that settles into the ink canvas. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF1E1433),
    0.42f to Ink,
    1f to Color(0xFF08060C),
)

/** The gold used for primary actions (play buttons, the Liked tile). */
val GoldBrush: Brush = Brush.linearGradient(listOf(GoldLight, Gold, GoldDeep))

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
