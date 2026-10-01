package com.music.spotui.ui.theme

/**
 * Shared motion tokens (`SoloMotion`): the easing curves and spring/tween specs that drive the
 * app's animations, so transitions feel consistent across screens.
 */

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.tween

/**
 * Shared motion tokens so every surface moves with the same rhythm: emphasized for
 * screen-level changes, standard for most transitions, quick for icons, spring for touch.
 */
object SoloMotion {
    const val PRESS_SCALE = 0.97f
    const val FADE_MS = 220
    const val NAV_MS = 200
    const val EXPAND_MS = 360
    const val ICON_MS = 150

    /** Pop a toggle (like / play) slightly past its resting size before it settles. */
    const val TOGGLE_POP_SCALE = 1.18f
    /** Fade a freshly inserted list item in over this long as it slides into place. */
    const val LIST_ENTER_MS = 260

    val Emphasized = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
    val Standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)

    val pressSpring: SpringSpec<Float> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 500f)

    fun <T> standard(): TweenSpec<T> = tween(durationMillis = 300, easing = Standard)
    fun <T> emphasized(): TweenSpec<T> = tween(durationMillis = 450, easing = Emphasized)
    fun <T> quick(): TweenSpec<T> = tween(durationMillis = 160)
    fun <T> fade(): TweenSpec<T> = tween(durationMillis = FADE_MS, easing = Standard)
    fun <T> spring(): SpringSpec<T> =
        androidx.compose.animation.core.spring(dampingRatio = 0.85f, stiffness = 420f)

    /** A bouncier spring for toggle pops (like / play) so the state change feels tactile. */
    fun <T> toggleSpring(): SpringSpec<T> =
        androidx.compose.animation.core.spring(dampingRatio = 0.45f, stiffness = 520f)
}
