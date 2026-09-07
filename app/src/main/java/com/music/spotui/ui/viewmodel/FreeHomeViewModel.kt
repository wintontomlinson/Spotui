package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.getListeningHistory
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/** A titled row of tracks on the login-free Home. */
data class HomeRow(
    val title: String,
    val query: String,
    val tracks: List<SongsModel> = emptyList(),
    val loading: Boolean = true,
)

/**
 * Powers the login-free Home. Each section is backed by a YouTube Music search
 * query (no Spotify session, no login) and mapped to [SongsModel] via the same
 * [toSongsModel] used by search, so tapping a track plays it through the normal
 * queue/player.
 *
 * The top of Home is personalized from the user's listening history (recent
 * artists / songs), so it updates according to what they actually play. Curated
 * evergreen sections follow (and are the only content on a fresh install).
 */
@HiltViewModel
class FreeHomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    // Evergreen fallback sections (also shown when there's no history yet).
    private val curated = listOf(
        "Today's biggest hits" to "top hits this week",
        "Trending now" to "trending songs",
        "Bollywood hits" to "latest bollywood songs",
        "Chill & Lo-Fi" to "lofi chill beats",
        "Workout energy" to "workout gym music",
        "Throwback classics" to "throwback hits playlist",
    )

    private val _rows = mutableStateOf<List<HomeRow>>(emptyList())
    val rows: State<List<HomeRow>> get() = _rows

    private var loaded = false
    // Timestamp of the newest history entry the current Home was built from, so we
    // can rebuild only when the user has actually played something new.
    private var builtFromHistoryTs = 0L

    fun loadOnce() {
        if (loaded) return
        loaded = true
        rebuild()
    }

    /**
     * Called when Home becomes visible again. Rebuilds only if new tracks were
     * played since the last build, so Home stays fresh with the user's interest
     * without reloading on every navigation.
     */
    fun maybeRefresh() {
        if (!loaded) { loadOnce(); return }
        val newestTs = runCatching { getListeningHistory(context).firstOrNull()?.ts ?: 0L }.getOrDefault(0L)
        if (newestTs > builtFromHistoryTs) rebuild()
    }

    /** Force a rebuild from the latest history. */
    fun refresh() {
        loaded = true
        rebuild()
    }

    fun retry() = refresh()

    private fun rebuild() {
        val history = runCatching { getListeningHistory(context) }.getOrDefault(emptyList())
        builtFromHistoryTs = history.firstOrNull()?.ts ?: 0L
        val sections = buildSections(history)
        _rows.value = sections.map { (title, query) -> HomeRow(title, query) }
        _rows.value.forEachIndexed { index, row -> fetchRow(index, row.query) }
    }

    /**
     * Builds the ordered (title, query) list: personalized rows derived from
     * recent listening first, then curated evergreen rows. Personalized rows are
     * de-duplicated and capped so Home stays focused.
     */
    private fun buildSections(
        history: List<com.music.spotui.data.preferences.HistoryEntry>,
    ): List<Pair<String, String>> {
        val personalized = mutableListOf<Pair<String, String>>()
        val usedTitles = LinkedHashSet<String>()

        if (history.isNotEmpty()) {
            // "More like <artist>" for the most recent distinct artists.
            val artists = history
                .mapNotNull { it.singer.substringBefore(",").trim().ifBlank { null } }
                .map { it.removeSuffix(" - Topic").trim() }
                .distinct()
                .take(3)
            for (artist in artists) {
                personalized += "More like $artist" to "$artist songs"
                usedTitles += "More like $artist"
            }

            // "Because you played <song>" for the single most recent track.
            history.firstOrNull()?.let { recent ->
                if (recent.title.isNotBlank()) {
                    val artist = recent.singer.substringBefore(",").trim()
                    val q = listOf(recent.title, artist, "mix").filter { it.isNotBlank() }.joinToString(" ")
                    val title = "Because you played ${recent.title}"
                    if (usedTitles.add(title)) personalized += title to q
                }
            }
        }

        // Curated rows always follow (and are the whole list on first launch).
        return personalized + curated
    }

    private fun fetchRow(index: Int, query: String) {
        viewModelScope.launch {
            val songs = withContext(Dispatchers.IO) {
                runCatching {
                    YouTube.search(query, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<SongItem>()
                        ?.take(12)
                        ?.map { it.toSongsModel() }
                        .orEmpty()
                }.getOrElse { emptyList() }
            }
            val current = _rows.value.toMutableList()
            if (index in current.indices) {
                current[index] = current[index].copy(tracks = songs, loading = false)
                _rows.value = current
            }
        }
    }
}
