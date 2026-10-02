package com.music.spotui.ui.theme

/**
 * SOLO corner-radius / shape tokens, exposed as the Material 3 [Shapes] set and the `SoloShape`
 * helpers so cards, sheets and buttons share a consistent rounding language.
 */

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii. xs 4 (small art), sm 8 (list art, chips, menus), md 12 (cards, inputs,
 * mini player), lg 20 (hero cards, detail art), xl 24 (player artwork, dialogs).
 * Pills use 50%; sheets use [sheetTop].
 */
object SoloShape {
    val xs = RoundedCornerShape(5.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(14.dp)
    val lg = RoundedCornerShape(20.dp)
    val xl = RoundedCornerShape(26.dp)
    val pill = RoundedCornerShape(50)
    val sheetTop = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp)
}

val SoloShapes = Shapes(
    extraSmall = SoloShape.xs,
    small = SoloShape.sm,
    medium = SoloShape.md,
    large = SoloShape.lg,
    extraLarge = SoloShape.xl,
)

/** Spacing scale. Screen gutter is 20dp; list rows are at least 64dp tall. */
object SoloSpacing {
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
