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
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Login-free search. Queries YouTube Music anonymously (no Spotify session, no
 * YouTube cookie) via [YouTube.search] and maps the [SongItem] results to the
 * app's [SongsModel] so they play through the normal player/queue exactly like
 * Spotify-backed tracks — the audio engine already resolves YouTube streams
 * without any login.
 */
@HiltViewModel
class YtSearchViewModel @Inject constructor() : ViewModel() {

    private val _query = mutableStateOf("")
    val query: State<String> get() = _query

    private val _results = mutableStateOf<List<SongsModel>>(emptyList())
    val results: State<List<SongsModel>> get() = _results

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error

    /** True once the user has actually run a search (to distinguish "empty" from "no results"). */
    private val _hasSearched = mutableStateOf(false)
    val hasSearched: State<Boolean> get() = _hasSearched

    private var searchJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
    }

    /** Debounced live search as the user types. */
    fun onQueryChangeDebounced(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.isBlank()) {
            _results.value = emptyList()
            _hasSearched.value = false
            _isLoading.value = false
            _error.value = null
            return
        }
        searchJob = viewModelScope.launch {
            delay(350L)
            search(text)
        }
    }

    fun search(text: String = _query.value) {
        val q = text.trim()
        if (q.isBlank()) return
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _hasSearched.value = true
            val songs = withContext(Dispatchers.IO) {
                runCatching {
                    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<SongItem>()
                        .orEmpty()
                }.getOrElse { emptyList() }
            }
            if (songs.isEmpty()) {
                // A network/parse failure and a genuinely empty result look the same
                // here; surface a soft message either way.
                _results.value = emptyList()
                _error.value = "No results — check your connection and try again."
            } else {
                _results.value = songs.map { it.toSongsModel() }
                _error.value = null
            }
            _isLoading.value = false
        }
    }

    fun clear() {
        searchJob?.cancel()
        _query.value = ""
        _results.value = emptyList()
        _hasSearched.value = false
        _error.value = null
        _isLoading.value = false
    }
}

/**
 * Maps a YouTube [SongItem] to a [SongsModel]. The [SongsModel.url] is set to the
 * raw YouTube video id — [com.music.spotui.di.SongPlayer.playSong] accepts an
 * 11-char id directly and resolves the stream anonymously. [spotifyTrackId] is
 * left blank so the Spotify/FLAC fast-paths are skipped and playback goes straight
 * to YouTube.
 */
fun SongItem.toSongsModel(): SongsModel = SongsModel(
    id = id.hashCode(),
    title = title,
    album = album?.name.orEmpty(),
    singer = artists.joinToString(", ") { it.name }.ifBlank { "Unknown artist" },
    coverUri = thumbnail.orEmpty(),
    url = id,
    spotifyTrackId = "",
    explicit = explicit,
    durationMs = (duration ?: 0) * 1000,
)
