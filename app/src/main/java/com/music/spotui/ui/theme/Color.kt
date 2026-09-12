package com.music.spotui.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ── Royal Edition palette ──
// Material seeds lean royal purple, with gold reserved for accents.
val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// Deep royal-purple canvas (base of the app gradient).
val AppBackground = Color(0xFF130824)
// A slightly lifted mid-tone so the gradient reads as depth, not a flat wash.
val AppBackgroundMid = Color(0xFF1E0F3A)
// Near-black bottom so content/lists settle into a calm, premium base.
val AppBackgroundDeep = Color(0xFF0B0416)
// Tile / grid placeholder surface with a purple tint.
val GridBackground = Color(0xFF2B1D4A)
// Primary accent is now royal gold (replaces the old blue #618DFF).
val AppPalette = Color(0xFFD4AF37)

// Royal-gold ramp for gradients / highlights.
val RoyalGoldLight = Color(0xFFF5E09A)
val RoyalGold = Color(0xFFD4AF37)
val RoyalGoldDeep = Color(0xFFB8892B)

// Royal-purple ramp for surfaces / schemes.
val RoyalPurpleLight = Color(0xFFC4B5FD)
val RoyalPurple = Color(0xFF7C3AED)
val RoyalPurpleDeep = Color(0xFF4C1D95)

/**
 * The shared app background — a top-to-bottom royal gradient (purple canvas →
 * lifted violet mid → near-black base). Using a single brush across every
 * screen root gives the whole app a cohesive, premium depth instead of the old
 * flat #130824 wash. Prefer this over `.background(AppBackground)` on screen
 * root containers.
 */
val AppBackgroundBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        AppBackground,
        AppBackgroundMid,
        AppBackgroundDeep,
    ),
)

/**
 * A subtler variant used for sheets / overlays where a full gradient would
 * fight with the content behind it. Fades from the mid tone into the deep base.
 */
val AppSurfaceBrush: Brush = Brush.verticalGradient(
    colors = listOf(
        AppBackgroundMid,
        AppBackgroundDeep,
    ),
)
