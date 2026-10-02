package com.music.spotui.data.update

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * App level, Compose observable source of truth for whether an update is available.
 *
 * [UpdateChecker.check] is still the detector, but its result used to live only inside the
 * update dialog and the Settings onClick scope, so nothing else could react to it. This holder
 * hoists the resolved [UpdateChecker.UpdateInfo] once so any screen can show the same state: the
 * update dialog renders it, and a small red dot appears on the Home settings button and the
 * Settings "Updates" row the moment an update is found, clearing again when the user installs or
 * dismisses it. Pure in memory state, no new dependency.
 */
object UpdateState {
    /** The latest available update, or null when up to date / not yet checked. */
    var available by mutableStateOf<UpdateChecker.UpdateInfo?>(null)
        private set

    /** True when a newer release is waiting, used to gate the red dot badge. */
    val hasUpdate: Boolean get() = available != null

    /**
     * Incremented when the user explicitly asks to see the update dialog (Settings > Updates), so
     * the dialog re-opens even after a "Later" dismissal for the same release.
     */
    var showRequest by mutableStateOf(0)
        private set

    fun set(info: UpdateChecker.UpdateInfo?) {
        available = info
    }

    /** Make the update dialog appear now for the known update (used by Settings > Updates). */
    fun requestShow() {
        showRequest += 1
    }

    fun clear() {
        available = null
    }
}
