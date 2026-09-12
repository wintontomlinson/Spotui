package com.music.spotui.ui.navigation

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

/**
 * Shared, app-wide signal for whether the bottom nav bar should be in its
 * COMPRESSED state. Any scrollable screen can feed it via [navBarScrollConnection]:
 * scrolling down (consuming content upward) compresses the bar, scrolling up
 * expands it again. Kept as a simple process-level holder so we don't have to
 * thread scroll state through every screen and the shared navbar can read it.
 */
object NavBarScrollState {
    /** true = compressed (user is scrolling down through content). */
    val compressed = mutableStateOf(false)

    fun reset() {
        compressed.value = false
    }
}

/**
 * Attach this to a scrollable's Modifier.nestedScroll(...) to drive the nav bar
 * compression. A small threshold avoids flicker on tiny finger jitters.
 */
val navBarScrollConnection: NestedScrollConnection = object : NestedScrollConnection {
    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val dy = available.y
        if (dy < -6f) {
            // Content moving up (scrolling down) → compress.
            NavBarScrollState.compressed.value = true
        } else if (dy > 6f) {
            // Content moving down (scrolling up / at top) → expand.
            NavBarScrollState.compressed.value = false
        }
        return Offset.Zero
    }
}
