package com.music.spotui.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.ArtistsModel
import com.music.spotui.data.entity.HomeFeedModel
import com.music.spotui.ui.repository.AppRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(private val repository: AppRepository)  : ViewModel() {


    private val _albums : MutableStateFlow<Response<List<AlbumsModel>>> = MutableStateFlow(Response.Loading())
    val albums : StateFlow<Response<List<AlbumsModel>>> = _albums

    private val _artists : MutableStateFlow<Response<List<ArtistsModel>>> = MutableStateFlow(Response.Loading())
    val artists : StateFlow<Response<List<ArtistsModel>>> = _artists

    private val _home : MutableStateFlow<Response<HomeFeedModel>> = MutableStateFlow(Response.Loading())
    val home : StateFlow<Response<HomeFeedModel>> = _home

    private val _songs : MutableStateFlow<Response<List<com.music.spotui.data.entity.SongsModel>>> = MutableStateFlow(Response.Loading())
    val songs : StateFlow<Response<List<com.music.spotui.data.entity.SongsModel>>> = _songs

    private val _podcasts : MutableStateFlow<Response<com.music.spotui.data.entity.SearchResults>> = MutableStateFlow(Response.Loading())
    val podcasts : StateFlow<Response<com.music.spotui.data.entity.SearchResults>> = _podcasts

    private val _audiobooks : MutableStateFlow<Response<com.music.spotui.data.entity.SearchResults>> = MutableStateFlow(Response.Loading())
    val audiobooks : StateFlow<Response<com.music.spotui.data.entity.SearchResults>> = _audiobooks

    private val _followedArtists : MutableStateFlow<List<ArtistsModel>> = MutableStateFlow(emptyList())
    val followedArtists : StateFlow<List<ArtistsModel>> = _followedArtists

    private val _selectedFilter = MutableStateFlow("All")
    val selectedFilter: StateFlow<String> = _selectedFilter

    private val _isFollowingOnly = MutableStateFlow(false)
    val isFollowingOnly: StateFlow<Boolean> = _isFollowingOnly

    init {
        fetchHome()
        fetchArtists()
        fetchAlbums()
        fetchSongs()
        fetchPodcasts()
        fetchAudiobooks()
        fetchFollowedArtists()
        startAutoRefresh()
    }

    /**
     * Keep Home feeling alive: every [AUTO_REFRESH_INTERVAL_MS] silently re-pull
     * the personalized/trending feed and the top-tracks row in the background so
     * the songs surfaced track what's currently trending and match the user's
     * interests over time — without the user having to pull-to-refresh. This is
     * a *silent* refresh (it does not toggle the pull-to-refresh spinner) so it
     * never interrupts scrolling. The loop lives on [viewModelScope] and so is
     * cancelled automatically when the ViewModel is cleared.
     */
    private fun startAutoRefresh() = viewModelScope.launch(Dispatchers.IO) {
        while (isActive) {
            delay(AUTO_REFRESH_INTERVAL_MS)
            // Bypass the process cache so the feed reorders (trending rotation)
            // and picks up freshly-personalized content.
            repository.provideHomeFeed(forceRefresh = true).collect { feed ->
                _home.value = feed
            }
            fetchSongs()
            fetchAlbums()
        }
    }

    private companion object {
        // 15 minutes: fresh enough to feel live and interest-aware, infrequent
        // enough to be gentle on battery and the Spotify endpoints.
        const val AUTO_REFRESH_INTERVAL_MS = 15L * 60 * 1000
    }

    fun setSelectedFilter(filter: String) {
        if (_selectedFilter.value != filter) {
            _selectedFilter.value = filter
            _isFollowingOnly.value = false
        }
    }

    fun toggleFollowing() {
        _isFollowingOnly.value = !_isFollowingOnly.value
    }

    fun resetFilters() {
        _selectedFilter.value = "All"
        _isFollowingOnly.value = false
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing

    private fun fetchHome(forceRefresh: Boolean = false) = viewModelScope.launch(Dispatchers.IO) {
        repository.provideHomeFeed(forceRefresh).collect { feed ->
            _home.value = feed
        }
    }

    /**
     * Pull-to-refresh: bypass the home cache and re-fetch, so the trending feed
     * reorders and picks up fresh content. Also re-pulls the supporting rows.
     */
    fun refresh() = viewModelScope.launch(Dispatchers.IO) {
        _isRefreshing.value = true
        try {
            repository.provideHomeFeed(forceRefresh = true).collect { feed ->
                _home.value = feed
            }
        } finally {
            _isRefreshing.value = false
        }
        fetchArtists()
        fetchAlbums()
        fetchSongs()
    }

    private fun fetchAlbums() = viewModelScope.launch(Dispatchers.IO) {
        repository.provideAlbums().collect{ album ->
            _albums.value = album as Response<List<AlbumsModel>>
        }
    }

    private fun fetchArtists() = viewModelScope.launch(Dispatchers.IO) {
        repository.provideArtists().collect { artist ->
            _artists.value = artist as Response<List<ArtistsModel>>
        }
    }

    private fun fetchSongs() = viewModelScope.launch(Dispatchers.IO) {
        repository.provideSongs().collect { songRes ->
            _songs.value = songRes
        }
    }

    private fun fetchPodcasts() = viewModelScope.launch(Dispatchers.IO) {
        repository.searchEverything("podcast").collect { podRes ->
            if (podRes is Response.Success && (podRes.data.shows.isNotEmpty() || podRes.data.episodes.isNotEmpty())) {
                _podcasts.value = podRes
            } else if (podRes is Response.Error || (podRes is Response.Success && podRes.data.shows.isEmpty())) {
                repository.searchEverything("show").collect { fallbackRes ->
                    _podcasts.value = fallbackRes
                }
            } else {
                _podcasts.value = podRes
            }
        }
    }

    private fun fetchAudiobooks() = viewModelScope.launch(Dispatchers.IO) {
        repository.searchEverything("audiobook").collect { abRes ->
            if (abRes is Response.Success && (abRes.data.shows.isNotEmpty() || abRes.data.episodes.isNotEmpty() || abRes.data.albums.isNotEmpty())) {
                _audiobooks.value = abRes
            } else if (abRes is Response.Error || (abRes is Response.Success && abRes.data.albums.isEmpty())) {
                repository.searchEverything("story").collect { fallbackRes ->
                    _audiobooks.value = fallbackRes
                }
            } else {
                _audiobooks.value = abRes
            }
        }
    }

    private fun fetchFollowedArtists() = viewModelScope.launch(Dispatchers.IO) {
        _followedArtists.value = repository.provideFollowedArtists()
    }

    suspend fun getAlbumSongs(albumName: String, artist: String = "") = repository.provideAlbumSongs(albumName, artist)
}