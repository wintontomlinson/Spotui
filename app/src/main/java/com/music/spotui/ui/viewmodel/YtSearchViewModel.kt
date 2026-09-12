package com.music.spotui.ui.viewmodel

import android.content.Context
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.metrolist.innertube.YouTube
import com.metrolist.innertube.models.AlbumItem
import com.metrolist.innertube.models.ArtistItem
import com.metrolist.innertube.models.PlaylistItem
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

    /** Which kind of result the user is looking at. */
    private val _tab = mutableStateOf(SearchTab.SONGS)
    val tab: State<SearchTab> get() = _tab

    private val _artists = mutableStateOf<List<ArtistResult>>(emptyList())
    val artists: State<List<ArtistResult>> get() = _artists

    private val _albums = mutableStateOf<List<AlbumResult>>(emptyList())
    val albums: State<List<AlbumResult>> get() = _albums

    private val _playlists = mutableStateOf<List<PlaylistResult>>(emptyList())
    val playlists: State<List<PlaylistResult>> get() = _playlists

    /**
     * Tabs already fetched for the current query, so flicking between them does not
     * re-hit the network. Cleared whenever the query changes.
     */
    private val loadedTabs = mutableSetOf<SearchTab>()

    private var searchJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
    }

    /** Switches result kind, fetching that kind on first visit for the current query. */
    fun selectTab(next: SearchTab) {
        if (_tab.value == next) return
        _tab.value = next
        val q = _query.value.trim()
        if (q.isNotBlank() && next !in loadedTabs) search(q, remember = false)
    }

    /** Debounced live search as the user types. */
    fun onQueryChangeDebounced(text: String) {
        _query.value = text
        searchJob?.cancel()
        if (text.isBlank()) {
            clearResults()
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

    private var lastQuery: String = ""

    private fun clearResults() {
        _results.value = emptyList()
        _artists.value = emptyList()
        _albums.value = emptyList()
        _playlists.value = emptyList()
        loadedTabs.clear()
    }

    fun search(text: String = _query.value, remember: Boolean = true) {
        val q = text.trim()
        if (q.isBlank()) return
        _query.value = q
        // A new query invalidates every tab, an unchanged query only needs the tabs
        // that have not been fetched yet.
        if (q != lastQuery) {
            clearResults()
            lastQuery = q
        }
        val activeTab = _tab.value
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            _isLoading.value = true
            _error.value = null
            _hasSearched.value = true

            val items = withContext(Dispatchers.IO) {
                runCatching {
                    YouTube.search(q, activeTab.filter).getOrNull()?.items.orEmpty()
                }.getOrElse { emptyList() }
            }

            var found = 0
            when (activeTab) {
                SearchTab.SONGS -> {
                    // Songs only. This is an audio player, so video results would
                    // promise something the app cannot deliver.
                    val songs = items.filterIsInstance<SongItem>().distinctBy { it.id }
                    _results.value = songs.map { it.toSongsModel() }
                    found = songs.size
                }
                SearchTab.ARTISTS -> {
                    val list = items.filterIsInstance<ArtistItem>()
                        .distinctBy { it.id }
                        .map {
                            ArtistResult(
                                id = it.id,
                                name = it.title,
                                thumbnail = hiResThumbnail(it.thumbnail),
                            )
                        }
                    _artists.value = list
                    found = list.size
                }
                SearchTab.ALBUMS -> {
                    val list = items.filterIsInstance<AlbumItem>()
                        .distinctBy { it.browseId }
                        .map {
                            AlbumResult(
                                browseId = it.browseId,
                                title = it.title,
                                artist = it.artists?.joinToString(", ") { a -> a.name }.orEmpty(),
                                year = it.year,
                                thumbnail = hiResThumbnail(it.thumbnail),
                            )
                        }
                    _albums.value = list
                    found = list.size
                }
                SearchTab.PLAYLISTS -> {
                    val list = items.filterIsInstance<PlaylistItem>()
                        .distinctBy { it.id }
                        .map {
                            PlaylistResult(
                                id = it.id,
                                title = it.title,
                                author = it.author?.name.orEmpty(),
                                songCount = it.songCountText.orEmpty(),
                                thumbnail = hiResThumbnail(it.thumbnail),
                            )
                        }
                    _playlists.value = list
                    found = list.size
                }
            }

            if (found == 0) {
                // A network failure and a genuinely empty result look the same here,
                // so surface a soft, actionable message either way.
                _error.value = "No results found. Please check your connection and try again."
            } else {
                _error.value = null
                loadedTabs += activeTab
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
        lastQuery = ""
        clearResults()
        _hasSearched.value = false
        _error.value = null
        _isLoading.value = false
        _recent.value = getRecentSearches(context)
    }
}

/** The kinds of result Explore can show, each backed by its own YouTube Music filter. */
enum class SearchTab(val label: String, val filter: YouTube.SearchFilter) {
    SONGS("Songs", YouTube.SearchFilter.FILTER_SONG),
    ARTISTS("Artists", YouTube.SearchFilter.FILTER_ARTIST),
    ALBUMS("Albums", YouTube.SearchFilter.FILTER_ALBUM),
    PLAYLISTS("Playlists", YouTube.SearchFilter.FILTER_COMMUNITY_PLAYLIST),
}

data class ArtistResult(
    val id: String,
    val name: String,
    val thumbnail: String,
)

data class AlbumResult(
    val browseId: String,
    val title: String,
    val artist: String,
    val year: Int?,
    val thumbnail: String,
)

data class PlaylistResult(
    val id: String,
    val title: String,
    val author: String,
    val songCount: String,
    val thumbnail: String,
)

/**
 * Stable, non-negative [SongsModel.id] for a YouTube video id. Used everywhere a
 * YouTube-sourced track is turned into a SongsModel (search, autoplay radio,
 * login-free Home) so the SAME video always maps to the SAME id. Previously some
 * paths used raw `hashCode()` (which can be negative) and others masked it, so the
 * same track got two different ids — breaking queue de-dup and "current track"
 * matching (next/prev could jump to the wrong song). The mask matches Api.stableId.
 */
fun youtubeStableId(videoId: String): Int = videoId.hashCode() and 0x7fffffff

/**
 * Maps a YouTube [SongItem] to a [SongsModel]. The [SongsModel.url] is set to the
 * raw YouTube video id, so [com.music.spotui.di.SongPlayer.playSong] accepts an
 * 11 character id directly and resolves the stream anonymously. [spotifyTrackId] is
 * left blank so the Spotify and FLAC fast paths are skipped and playback goes
 * straight to YouTube.
 */
fun SongItem.toSongsModel(): SongsModel {
    // The artist is resolved first because the title cleanup needs it: a leading
    // "Something - " may only be dropped once we know it really is the artist.
    val artist = resolveArtist(artists.map { it.name }, title)
    return SongsModel(
        id = youtubeStableId(id),
        title = cleanTrackTitle(title, artist),
        album = album?.name.orEmpty(),
        singer = artist,
        coverUri = hiResThumbnail(thumbnail),
        url = id,
        spotifyTrackId = "",
        explicit = explicit,
        durationMs = (duration ?: 0) * 1000,
    )
}

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
internal fun cleanTrackTitle(title: String, knownArtist: String = ""): String {
    var t = title
        .replace(Regex("""\s*[(\[][^)\]]*(official|lyric|lyrics|audio|visualizer|video|hd|4k|hq|mv)[^)\]]*[)\]]""", RegexOption.IGNORE_CASE), "")
        .replace(Regex("""\s*\|.*$"""), "")
        .trim()

    // Only cut at " - " when what comes before it really is the artist. YouTube uploads
    // are often named "Artist - Song", but plenty of genuine track titles contain a dash
    // too, as in "Tum Hi Ho - Aashiqui 2", and cutting those blindly renamed the track to
    // its album, which is why album tracklists showed the wrong names.
    val dash = t.indexOf(" - ")
    if (dash > 0 && isArtistPrefix(t.substring(0, dash), knownArtist)) {
        t = t.substring(dash + 3).trim()
    }
    return t.ifBlank { title }
}

/** True when [prefix] names the same act as [artist], ignoring case and punctuation. */
private fun isArtistPrefix(prefix: String, artist: String): Boolean {
    fun key(value: String) = value.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()
    val p = key(prefix)
    if (p.isBlank()) return false
    val a = key(artist)
    if (a.isBlank()) return false
    if (a == p || a.contains(p) || p.contains(a)) return true
    // "A.R. Rahman, Arijit Singh" should still match a "Arijit Singh - " prefix.
    return artist.split(",").any { key(it).let { part -> part.isNotBlank() && part == p } }
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
