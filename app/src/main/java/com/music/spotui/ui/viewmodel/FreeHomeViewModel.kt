package com.music.spotui.ui.viewmodel

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.music.spotui.data.entity.SongsModel
import dagger.hilt.android.lifecycle.HiltViewModel
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
 */
@HiltViewModel
class FreeHomeViewModel @Inject constructor() : ViewModel() {

    // The curated sections shown on Home. Titles are friendly; queries drive the fetch.
    private val sections = listOf(
        "Today's biggest hits" to "top hits this week",
        "Trending now" to "trending songs",
        "Bollywood hits" to "latest bollywood songs",
        "Chill & Lo-Fi" to "lofi chill beats",
        "Workout energy" to "workout gym music",
        "Throwback classics" to "throwback hits playlist",
    )

    private val _rows = mutableStateOf<List<HomeRow>>(
        sections.map { (title, query) -> HomeRow(title, query) }
    )
    val rows: State<List<HomeRow>> get() = _rows

    private var loaded = false

    fun loadOnce() {
        if (loaded) return
        loaded = true
        _rows.value.forEachIndexed { index, row -> fetchRow(index, row.query) }
    }

    fun retry() {
        loaded = false
        _rows.value = sections.map { (title, query) -> HomeRow(title, query) }
        loadOnce()
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
