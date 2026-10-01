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
 * Solo design tokens: Graphite & Azure.
 *
 * A near-black graphite canvas with a quietly layered surface ramp (each step one visible
 * notch lighter, drifting from ink-graphite into cool slate-steel) and ONE restrained,
 * refined-azure accent. Where the retired Lumen Indigo scheme fanned a violet beam into
 * cyan and gold, Graphite & Azure is deliberately disciplined: a single confident blue
 * (azure ~217deg) does all the signalling, a cool steel highlight edges only the hero
 * surfaces, and everything else is graphite and high-contrast off-white. Restraint is the
 * brand statement — one voice, one accent, pure sound — and it reads as premium precisely
 * because nothing competes with the music.
 *
 * Competitor avoidance: a cool refined azure centred around hue ~217deg sits clear of
 * Spotify grass green (~141deg), YouTube Music / Netflix red, SoundCloud orange,
 * Apple Music pink-magenta, Tidal/Deezer near-monochrome blue-greys, Amazon Music
 * teal-on-navy, JioSaavn / Gaana / Wynk reds and greens. Pairing that azure with a pure
 * graphite canvas and a sparse steel highlight keeps the brand its own hue family rather
 * than borrowing any named competitor's signature.
 *
 * Contrast (WCAG, approx): Accent #3B82F6 on Canvas #0E1013 ~4.9:1; AccentSoft #93C5FD
 * on Canvas ~9.6:1; TextPrimary #F4F6F8 ~17:1; TextSecondary #AEB6C2 ~8.9:1;
 * TextTertiary #7C8794 ~4.6:1 (>=4.5:1 on Canvas).
 *
 * Screens read colours from here instead of hard-coding hex values, so the whole app stays
 * on one palette.
 */

// Canvas and surface ramp (ink graphite -> cool slate-steel)
val Canvas = Color(0xFF0E1013)     // app canvas
val Surface1 = Color(0xFF15181C)   // bars, grouped rows
val Surface2 = Color(0xFF1B1F24)   // cards, sheets, dialogs
val Surface3 = Color(0xFF242931)   // inputs, chips, icon wells
val Surface4 = Color(0xFF2F353E)   // pressed / selected
val Elevated = Color(0xFF171B20)   // hero plate, brand crown

// Azure accent ramp (soft sky -> signature azure -> deep cobalt)
val AccentSoft = Color(0xFF93C5FD)
val Accent = Color(0xFF3B82F6)     // signature refined azure: text, icons and fills
val AccentDeep = Color(0xFF1D4ED8) // deep cobalt for gradient depth
val OnAccent = Color(0xFF0A0E14)   // content drawn on accent fills

// Cool steel highlight (hero edges only, used sparingly)
val Gold = Color(0xFFCBD5E1)

// Text
val TextPrimary = Color(0xFFF4F6F8)
val TextSecondary = Color(0xFFAEB6C2)
val TextTertiary = Color(0xFF7C8794)
val TextDisabled = Color(0xFF4A525C)

// Lines
val Hairline = Color(0x14F4F6F8)        // 8% off-white: dividers, card edges
val HairlineAccent = Color(0x383B82F6)  // 22% azure: premium edges on hero surfaces

// Status
val Danger = Color(0xFFF26D6D)
val DangerSurface = Color(0xFF3A1A1A)
val Success = Color(0xFF3DBE8B)
val Warning = Color(0xFFE2B15C)

// Scrim behind sheets and dialogs (70% black)
val Scrim = Color(0xB3000000)

// Shadow for floating chrome (mini player, nav): 40% black
val Shadow = Color(0x66000000)

// Glass (translucent chrome over scrolling content)
val GlassFill = Color(0xD915181C)
val GlassFillStrong = Color(0xF215181C)

/** Shared screen background: a faint azure glow at the top that settles into the graphite canvas. */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    0f to Color(0xFF13213A),
    0.4f to Canvas,
    1f to Color(0xFF0A0C0F),
)

/** The accent gradient used for primary actions (play buttons, the Liked tile): soft sky -> azure -> cobalt. */
val AccentBrush: Brush = Brush.linearGradient(listOf(AccentSoft, Accent, AccentDeep))

/**
 * Keeps an artwork-derived colour rich but dark enough for light text on top
 * (saturation <= 0.55, lightness 0.10..0.22), so tinted player and album backgrounds
 * never wash out or clash with the azure accent on the darker graphite canvas.
 */
fun artworkTone(color: Color): Color {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(color.toArgb(), hsl)
    hsl[1] = hsl[1].coerceAtMost(0.55f)
    hsl[2] = hsl[2].coerceIn(0.10f, 0.22f)
    return Color(ColorUtils.HSLToColor(hsl))
}
