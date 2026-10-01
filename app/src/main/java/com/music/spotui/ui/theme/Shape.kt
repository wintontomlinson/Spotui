package com.music.spotui.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

/**
 * Corner radii. xs 6 (small art), sm 10 (list art, tiles), md 16 (cards, mini player),
 * lg 24 (sheets, dialogs), xl 28 (player artwork). Pills use 50%.
 */
object SoloShape {
    val xs = RoundedCornerShape(6.dp)
    val sm = RoundedCornerShape(10.dp)
    val md = RoundedCornerShape(16.dp)
    val lg = RoundedCornerShape(24.dp)
    val xl = RoundedCornerShape(28.dp)
    val pill = RoundedCornerShape(50)
    val sheetTop = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
}

val SoloShapes = Shapes(
    extraSmall = SoloShape.xs,
    small = SoloShape.sm,
    medium = SoloShape.md,
    large = SoloShape.lg,
    extraLarge = SoloShape.xl,
)

/** Spacing scale. Screen gutter is 18dp; list rows are at least 64dp tall. */
object SoloSpacing {
    val xxs = 4.dp
    val xs = 8.dp
    val sm = 12.dp
    val md = 16.dp
    val gutter = 18.dp
    val lg = 20.dp
    val xl = 24.dp
    val xxl = 32.dp
    val rowHeight = 64.dp
    val touchTarget = 48.dp
}

/** Shared motion specs: emphasized for screen-level changes, quick for icons, spring for touch. */
object Motion {
    fun <T> emphasized() = androidx.compose.animation.core.tween<T>(
        durationMillis = 420,
        easing = androidx.compose.animation.core.FastOutSlowInEasing,
    )
    fun <T> quick() = androidx.compose.animation.core.tween<T>(durationMillis = 180)
    fun <T> spring() = androidx.compose.animation.core.spring<T>(dampingRatio = 0.8f, stiffness = 380f)
}
