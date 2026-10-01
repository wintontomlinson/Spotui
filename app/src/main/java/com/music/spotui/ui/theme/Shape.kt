package com.music.spotui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii. xs 4 (small art), sm 8 (list art, chips, menus), md 12 (cards, inputs,
 * mini player), lg 20 (hero cards, detail art), xl 24 (player artwork, dialogs).
 * Pills use 50%; sheets use [sheetTop].
 */
object SonvraShape {
    val xs = RoundedCornerShape(4.dp)
    val sm = RoundedCornerShape(8.dp)
    val md = RoundedCornerShape(12.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(24.dp)
    val pill = RoundedCornerShape(50)
    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

val SonvraShapes = Shapes(
    extraSmall = SonvraShape.xs,
    small = SonvraShape.sm,
    medium = SonvraShape.md,
    large = SonvraShape.lg,
    extraLarge = SonvraShape.xl,
)

/** Spacing scale. Screen gutter is 20dp; list rows are at least 64dp tall. */
object SonvraSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val gutter = 20.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val rowHeight = 64.dp
    val touchTarget = 48.dp
}
