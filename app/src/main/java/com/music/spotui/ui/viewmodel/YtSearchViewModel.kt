package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.SongItem
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.addRecentSearch
import com.music.spotui.data.preferences.clearRecentSearches
import com.music.spotui.data.preferences.getRecentSearches
import com.music.spotui.data.preferences.removeRecentSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

/**
 * Login free search. Queries YouTube Music anonymously (no Spotify session, no
 * YouTube cookie) via [YouTube.search] and maps the results to the app's
 * [SongsModel] so they play through the normal player and queue exactly like
 * Spotify backed tracks. The audio engine already resolves YouTube streams
 * without any login.
 */
@HiltViewModel
class YtSearchViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _query = mutableStateOf("")
    val query: State<String> get() = _query

    private val _results = mutableStateOf<List<SongsModel>>(emptyList())
    val results: State<List<SongsModel>> get() = _results

    private val _isLoading = mutableStateOf(false)
    val isLoading: State<Boolean> get() = _isLoading

    private val _error = mutableStateOf<String?>(null)
    val error: State<String?> get() = _error

    /** True once the user has actually run a search, to tell "empty" from "no results". */
    private val _hasSearched = mutableStateOf(false)
    val hasSearched: State<Boolean> get() = _hasSearched

    private val _recent = mutableStateOf(getRecentSearches(context))
    val recent: State<List<String>> get() = _recent

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
            search(text, remember = false)
        }
    }

    fun search(text: String = _query.value, remember: Boolean = true) {
        val q = text.trim()
        if (q.isBlank()) return
        _query.value = q
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _hasSearched.value = true
            val songs = withContext(Dispatchers.IO) {
                runCatching {
                    // Songs only. This is an audio player, so video results would
                    // promise something the app cannot deliver.
                    YouTube.search(q, YouTube.SearchFilter.FILTER_SONG)
                        .getOrNull()
                        ?.items
                        ?.filterIsInstance<SongItem>()
                        .orEmpty()
                }.getOrElse { emptyList() }
            }
            if (songs.isEmpty()) {
                // A network failure and a genuinely empty result look the same here,
                // so surface a soft, actionable message either way.
                _results.value = emptyList()
                _error.value = "No results found. Please check your connection and try again."
            } else {
                // Drop duplicate videos so the list and the queue stay clean.
                _results.value = songs.distinctBy { it.id }.map { it.toSongsModel() }
                _error.value = null
                if (remember) rememberQuery(q)
            }
            _isLoading.value = false
        }
    }

    /** Called when the user commits a search, so history reflects intent not keystrokes. */
    fun rememberQuery(q: String) {
        addRecentSearch(context, q)
        _recent.value = getRecentSearches(context)
    }

    fun removeRecent(q: String) {
        removeRecentSearch(context, q)
        _recent.value = getRecentSearches(context)
    }

    fun clearRecent() {
        clearRecentSearches(context)
        _recent.value = emptyList()
    }

    fun clear() {
        searchJob?.cancel()
        _query.value = ""
        _results.value = emptyList()
        _hasSearched.value = false
        _error.value = null
        _isLoading.value = false
        _recent.value = getRecentSearches(context)
    }
}

/**
 * Maps a YouTube [SongItem] to a [SongsModel]. The [SongsModel.url] is set to the
 * raw YouTube video id, so [com.music.spotui.di.SongPlayer.playSong] accepts an
 * 11 character id directly and resolves the stream anonymously. [spotifyTrackId] is
 * left blank so the Spotify and FLAC fast paths are skipped and playback goes
 * straight to YouTube.
 */
fun SongItem.toSongsModel(): SongsModel = SongsModel(
    id = id.hashCode(),
    title = cleanTrackTitle(title),
    album = album?.name.orEmpty(),
    singer = resolveArtist(artists.map { it.name }, title),
    coverUri = hiResThumbnail(thumbnail),
    url = id,
    spotifyTrackId = "",
    explicit = explicit,
    durationMs = (duration ?: 0) * 1000,
)

// Values YouTube puts in the subtitle line that are not artists. The subtitle can
// be "Song · Artist · Album · 3:45" or "Video · Channel · 1.2M views · 3:45", so
// depending on the response shape the first entry is sometimes a type label, a
// view count or a duration rather than the artist.
private val NON_ARTIST_LABELS = setOf(
    "song", "songs", "video", "videos", "episode", "album", "single", "ep",
    "playlist", "artist", "podcast",
)
private val VIEW_COUNT = Regex("""^[\d.,]+\s*[kmb]?\s*(views|view|plays|play)$""", RegexOption.IGNORE_CASE)
private val TIMESTAMP = Regex("""^\d{1,2}:\d{2}(:\d{2})?$""")

private fun isUsableArtist(name: String): Boolean {
    val n = name.trim()
    if (n.isBlank()) return false
    if (n.lowercase() in NON_ARTIST_LABELS) return false
    if (VIEW_COUNT.matches(n)) return false
    if (TIMESTAMP.matches(n)) return false
    return true
}

/**
 * Picks a sensible artist for a YouTube result.
 *
 * The subtitle line is not always the artist, so junk entries are dropped first.
 * When nothing usable is left, a title in the common "Artist - Song" shape is used
 * to recover the artist. As a last resort the field is left empty so the UI can
 * simply omit it instead of printing a placeholder.
 */
internal fun resolveArtist(names: List<String>, title: String): String {
    val usable = names.map { it.trim() }
        .filter { isUsableArtist(it) }
        .map { it.removeSuffix(" - Topic").trim() }
        .distinct()
    if (usable.isNotEmpty()) return usable.joinToString(", ")

    // "Artist - Song Name" is the most common YouTube upload convention.
    val dash = title.indexOf(" - ")
    if (dash > 0) {
        val candidate = title.substring(0, dash).trim()
        if (isUsableArtist(candidate)) return candidate
    }
    return ""
}

/**
 * Trims the promotional noise YouTube uploaders add to titles, and drops a leading
 * "Artist - " prefix once the artist is shown separately.
 */
internal fun cleanTrackTitle(title: String): String {
    var t = title
        .replace(Regex("""\s*[(\[][^)\]]*(official|lyric|lyrics|audio|visualizer|video|hd|4k|hq|mv)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\|.*$"""), "")
        .trim()
    val dash = t.indexOf(" - ")
    if (dash > 0) t = t.substring(dash + 3).trim()
    return t.ifBlank { title }
}

private val SIZE_OPTION = Regex("""^[swh]\d""")
private val WIDTH_TOKEN = Regex("""\bw\d+""")
private val HEIGHT_TOKEN = Regex("""\bh\d+""")
private val SQUARE_TOKEN = Regex("""\bs\d+""")
private val PATH_SIZE = Regex("""/w\d+-h\d+""")

/**
 * Google's image CDN bakes the requested size into the URL, and the sizes served for
 * list thumbnails look blurry when shown large. This raises the requested size while
 * leaving everything else about the URL untouched.
 *
 * Only the size tokens are swapped. An earlier version replaced the whole option
 * string after the "=" with a fixed "w544-h544-l90-rj", which broke any URL using a
 * different option set. Artist and channel avatars are served as
 * "...=s176-c-k-c0x00ffffff-no-rj", so rewriting the options dropped the crop and
 * background flags and the image failed to load, which is why artist photos went
 * missing. URLs with no size option, such as i.ytimg.com thumbnails, are returned
 * unchanged.
 */
fun hiResThumbnail(url: String?, size: Int = 544): String {
    val u = url.orEmpty()
    if (u.isBlank()) return u

    val eq = u.lastIndexOf('=')
    if (eq != -1 && eq < u.length - 1) {
        val base = u.substring(0, eq)
        val options = u.substring(eq + 1)
        // Only touch it when the part after "=" really is a size option, so query
        // strings that happen to contain "=" are left alone.
        if (SIZE_OPTION.containsMatchIn(options)) {
            var updated = WIDTH_TOKEN.replace(options, "w$size")
            updated = HEIGHT_TOKEN.replace(updated, "h$size")
            updated = SQUARE_TOKEN.replace(updated, "s$size")
            return "$base=$updated"
        }
    }

    // Some URLs carry the size as a path segment such as ".../w120-h120/...".
    val replaced = PATH_SIZE.replace(u, "/w$size-h$size")
    if (replaced != u) return replaced
    return u
}

/** Formats a track length in milliseconds as m:ss, or blank when unknown. */
fun formatDurationMs(durationMs: Int): String {
    if (durationMs <= 0) return ""
    val totalSeconds = durationMs / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%d:%02d".format(minutes, seconds)
}
