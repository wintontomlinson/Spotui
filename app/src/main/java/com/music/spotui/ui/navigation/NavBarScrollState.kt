package com.music.spotui.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.geometry.Offset

/**
 * A tiny shared controller that lets any scrolling screen tell the bottom nav bar
 * how "compressed" it should be. 0f = fully expanded (idle / at top / scrolling
 * up), 1f = fully compressed (scrolling down through content).
 *
 * The navbar reads [compression] and animates its size/alpha off it; screens push
 * updates by attaching [Modifier.navBarScroll]. Kept as a process-level singleton
 * (not a ViewModel) because the bar and the screens live in different composition
 * subtrees under the app Scaffold, and this is cheaper than threading a callback
 * through every screen.
 */
object NavBarScrollState {

    /** 0f = expanded, 1f = compressed. Observed by the navbar. */
    var compression by mutableFloatStateOf(0f)
        private set

    /** How many pixels of downward scroll fully compress the bar. */
    private const val COMPRESS_DISTANCE_PX = 180f

    /** Called by the nested-scroll connection with the vertical delta. */
    fun onScrollDelta(dy: Float) {
        // dy < 0 → content moving up (user scrolling down) → compress.
        // dy > 0 → content moving down (user scrolling up) → expand.
        val next = (compression - dy / COMPRESS_DISTANCE_PX).coerceIn(0f, 1f)
        compression = next
    }

    /** Reset to expanded, e.g. when leaving a scrolling screen. */
    fun reset() {
        compression = 0f
    }
}

/**
 * Attach to a scrollable container (LazyColumn/Column) so its vertical scrolling
 * drives [NavBarScrollState]. The navbar compresses as the user scrolls down into
 * the content and expands again as they scroll back up.
 */
fun Modifier.navBarScroll(): Modifier = this.nestedScroll(
    object : NestedScrollConnection {
        override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
            NavBarScrollState.onScrollDelta(available.y)
            return Offset.Zero
        }
    }
)
