package com.music.spotui.ui.screens

import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import android.content.Context
import android.annotation.SuppressLint
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.AppSearchBar
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.components.SwipeToPlayNextWrapper
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.data.preferences.PlaylistSortOption
import com.music.spotui.data.preferences.getPlaylistSortOption
import com.music.spotui.data.preferences.isPlaylistSortDescending
import com.music.spotui.data.preferences.setPlaylistSort
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.viewmodel.PlaylistViewModel
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun PlaylistScreen(navController: NavController, playlistId: String, playlistName: String = "") {

    val playlistViewModel: PlaylistViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val songsResp by playlistViewModel.songs.collectAsState()
    val playlistResp by playlistViewModel.playlist.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(playlistId) {
        playlistViewModel.loadPlaylist(playlistId)
    }

    val songs = (songsResp as? Response.Success)?.data.orEmpty()
    val playlist = (playlistResp as? Response.Success)?.data
        ?: AlbumsModel(
            id = playlistId.hashCode() and 0x7fffffff,
            artists = "",
            coverUri = songs.firstOrNull()?.coverUri ?: "",
            name = playlistName,
            time = "",
        )

    LaunchedEffect(songs, playlist, songsResp, playlistResp) {
        if (songsResp is Response.Success && playlistResp is Response.Success && songs.isNotEmpty() && playlist != null) {
            com.music.spotui.data.preferences.OfflineCollectionsPref.saveCollection(
                context = context,
                id = playlistId,
                name = playlist.name,
                coverUri = playlist.coverUri,
                artists = playlist.artists,
                isPlaylist = true,
                songs = songs
            )
        }
    }

    LaunchedEffect(songs) {
        if (songs.isNotEmpty()) {
            SongPlayer.prefetchList(songs.map { it.url }, context)
        }
    }
    
    var searchQuery by remember(playlistId) { mutableStateOf("") }
    var currentSort by remember(playlistId) { mutableStateOf(getPlaylistSortOption(context, playlistId)) }
    var isDescending by remember(playlistId) { mutableStateOf(isPlaylistSortDescending(context, playlistId)) }
    var showSortSheet by remember { mutableStateOf(false) }

    val filteredSongs = remember(songs, searchQuery, currentSort, isDescending) {
        val filtered = if (searchQuery.isBlank()) {
            songs
        } else {
            songs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.singer.contains(searchQuery, ignoreCase = true)
            }
        }
        
        when (currentSort) {
            PlaylistSortOption.DATE -> if (isDescending) filtered.reversed() else filtered
            PlaylistSortOption.TITLE -> if (isDescending) filtered.sortedByDescending { it.title.lowercase() } else filtered.sortedBy { it.title.lowercase() }
            PlaylistSortOption.ARTIST -> if (isDescending) filtered.sortedByDescending { it.singer.lowercase() } else filtered.sortedBy { it.singer.lowercase() }
            PlaylistSortOption.ALBUM -> if (isDescending) filtered.sortedByDescending { it.album.lowercase() } else filtered.sortedBy { it.album.lowercase() }
        }
    }

    var menuSong by remember { mutableStateOf<com.music.spotui.data.entity.SongsModel?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }

    if (showRenameDialog) {
        val currentName = playlist.name.ifBlank { playlistName }
        var newPlaylistNameInput by remember(currentName) { mutableStateOf(currentName) }
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            containerColor = Surface2,
            title = { Text("Rename Playlist", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = {
                TextField(
                    value = newPlaylistNameInput,
                    onValueChange = { newPlaylistNameInput = it },
                    placeholder = { Text("Playlist name", color = TextTertiary) },
                    singleLine = true,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface4,
                        unfocusedContainerColor = Surface4,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                        cursorColor = Accent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(SoloShape.sm)
                )
            },
            confirmButton = {
                com.music.spotui.ui.components.SoloDialogConfirm(
                    text = "Save",
                    onClick = {
                            val name = newPlaylistNameInput.trim()
                            if (name.isNotBlank()) {
                                com.music.spotui.data.preferences.LocalPlaylistPref.renamePlaylist(context, playlistId, name)
                                com.music.spotui.data.api.Api.HomeCache.library = null
                                com.music.spotui.data.api.Api.HomeCache.invalidatePlaylist(playlistId)
                                playlistViewModel.reloadPlaylist(playlistId)
                            }
                            showRenameDialog = false
                        },
                )
            },
            dismissButton = {
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Cancel",
                    onClick = { showRenameDialog = false },
                )
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = Surface2,
            title = { Text("Delete Playlist", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
            text = { Text("Are you sure you want to delete this playlist?", color = TextSecondary) },
            confirmButton = {
                com.music.spotui.ui.components.SoloDialogConfirm(
                    text = "Delete",
                    danger = true,
                    onClick = {
                            com.music.spotui.data.preferences.LocalPlaylistPref.deletePlaylist(context, playlistId)
                            com.music.spotui.data.api.Api.HomeCache.library = null
                            com.music.spotui.data.api.Api.HomeCache.invalidatePlaylist(playlistId)
                            showDeleteDialog = false
                            navController.navigateUp()
                        },
                )
            },
            dismissButton = {
                com.music.spotui.ui.components.SoloDialogDismiss(
                    text = "Cancel",
                    onClick = { showDeleteDialog = false },
                )
            }
        )
    }

    val playlistArtists = playlist.artists
    val playlistArtistList = remember(playlistArtists) {
        playlistArtists.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    var showArtistSheet by remember { mutableStateOf(false) }
    if (showArtistSheet) {
        ArtistsSheet(
            artistNames = playlistArtistList,
            context = context,
            onDismiss = { showArtistSheet = false },
            navController = navController,
        )
    }

    menuSong?.let { sel ->
        com.music.spotui.ui.components.SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            currentPlaylistId = playlistId,
            onSongRemovedFromPlaylist = {
                com.music.spotui.data.api.Api.HomeCache.invalidatePlaylist(playlistId)
                playlistViewModel.reloadPlaylist(playlistId)
            },
            onDismiss = { menuSong = null },
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        if (songsResp is Response.Loading && playlistResp is Response.Loading) {
            Loader()
            return@Surface
        }

        var snackbarMessage by remember { mutableStateOf("") }
        var snackbarVisible by remember { mutableStateOf(false) }
        LaunchedEffect(snackbarVisible) {
            if (snackbarVisible) {
                kotlinx.coroutines.delay(1500)
                snackbarVisible = false
            }
        }

        var dominentColor by remember { mutableStateOf(com.music.spotui.ui.theme.Canvas) }
        // Once per cover, not on every recomposition.
        LaunchedEffect(playlist.coverUri) {
            if (playlist.coverUri.isNotBlank()) {
                Palette().extractSecondColorFromCoverUrl(context = context, playlist.coverUri) { color ->
                    dominentColor = color
                }
            }
        }

        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        // The top bar turns to strong glass once the hero has scrolled away.
        val barCollapsed by remember { androidx.compose.runtime.derivedStateOf { listState.firstVisibleItemIndex > 0 || listState.firstVisibleItemScrollOffset > 600 } }
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    modifier = Modifier.padding(horizontal = 4.dp),
                    navigationIcon = {
                    com.music.spotui.ui.components.SoloIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { navController.navigateUp() },
                        filled = true,
                    )
                },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = if (barCollapsed) com.music.spotui.ui.theme.GlassFillStrong else Color.Transparent,
                        scrolledContainerColor = com.music.spotui.ui.theme.GlassFillStrong,
                        titleContentColor = TextPrimary,
                    ),
                    title = {
                    if (barCollapsed) Text(
                        text = playlistName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                )
            }
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Canvas)
                ) {
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 440.dp)
                            .padding(bottom = 8.dp)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(com.music.spotui.ui.theme.artworkTone(dominentColor), Canvas),
                                    startY = -100f,
                                ),
                            ),
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Spacer(modifier = Modifier.padding(25.dp))

                        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                            GlideImage(
                                modifier = Modifier
                            .size(200.dp)
                            .shadow(18.dp, SoloShape.lg, ambientColor = com.music.spotui.ui.theme.Shadow, spotColor = com.music.spotui.ui.theme.Shadow)
                            .clip(SoloShape.lg),
                        contentScale = ContentScale.Crop,
                                model = playlist.coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                contentDescription = "",
                            )
                        }
                        Spacer(modifier = Modifier.padding(5.dp))
                        val isLocalPlaylist = playlistId.startsWith("local_pl_")
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(20.dp, 5.dp, 20.dp, 0.dp)
                                .then(
                                    if (isLocalPlaylist) {
                                        Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) { showRenameDialog = true }
                                    } else Modifier
                                )
                        ) {
                            Text(
                                text = playlist.name.ifBlank { playlistName },
                                color = TextPrimary,
                                style = MaterialTheme.typography.headlineLarge,
                                fontFamily = com.music.spotui.ui.theme.SoloDisplay)
                            if (isLocalPlaylist) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Playlist Name",
                                    tint = TextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                        if (playlist.time.isNotBlank()) {
                            Text(
                                modifier = Modifier.padding(20.dp, 4.dp, 20.dp, 0.dp),
                                text = playlist.time,
                                color = TextTertiary,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(20.dp, 4.dp, 0.dp, 0.dp)
                        ) {
                            if (playlistId.startsWith("local_pl_")) {
                                Icon(
                                    imageVector = Icons.Default.PhoneAndroid,
                                    contentDescription = "Local Playlist",
                                    tint = Accent,
                                    modifier = Modifier
                                        .size(14.dp)
                                        .padding(end = 4.dp)
                                )
                                Text(
                                    text = "Local Playlist",
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium
                                )
                            } else if (playlist.artists.isNotBlank()) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "Playlist • ",
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = playlist.artists,
                                        color = TextPrimary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                        ) {
                                            if (playlistArtistList.size == 1) {
                                                navController.navigate(artistRoute(playlistArtistList[0]))
                                            } else if (playlistArtistList.size > 1) {
                                                showArtistSheet = true
                                            }
                                        }
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .padding(20.dp, 0.dp)
                        ) {
                            var playlistDownloaded by remember(songs) {
                                mutableStateOf(songs.isNotEmpty() && SongPlayer.allDownloaded(songs, context))
                            }

                            if (snackbarVisible) {
                                Box(modifier = Modifier.weight(1f)) {
                                    Snackbar(showMessage = snackbarMessage)
                                }
                                Spacer(modifier = Modifier.width(16.dp))
                            } else {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    if (songs.isNotEmpty()) {
                                        // Add all playlist tracks to the queue.
                                        Icon(
                                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.PlaylistAdd),
                                            tint = TextPrimary,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    playlistViewModel.addAllToQueue(filteredSongs)
                                                    android.widget.Toast.makeText(
                                                        context,
                                                        "${filteredSongs.size} track(s) added to queue",
                                                        android.widget.Toast.LENGTH_SHORT,
                                                    ).show()
                                                }
                                                .padding(12.dp),
                                            contentDescription = "Add to queue",
                                        )
                                        Icon(
                                            imageVector = if (playlistDownloaded)
                                                Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Rounded.Download,
                                            tint = if (playlistDownloaded) Accent else TextPrimary,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    com.music.spotui.data.preferences.OfflineCollectionsPref.saveCollection(
                                                        context = context,
                                                        id = playlistId,
                                                        name = playlist.name,
                                                        coverUri = playlist.coverUri,
                                                        artists = playlist.artists,
                                                        isPlaylist = true,
                                                        songs = songs
                                                    )
                                                    if (!playlistDownloaded) {
                                                        SongPlayer.downloadAll(songs, context)
                                                        snackbarMessage = "Downloading ${songs.size} tracks…"
                                                        snackbarVisible = true
                                                    } else {
                                                        snackbarMessage = "Playlist added to offline library"
                                                        snackbarVisible = true
                                                    }
                                                }
                                                .padding(12.dp),
                                            contentDescription = "Download playlist",
                                        )
                                        // Shuffle-play: start the playlist in random order.
                                        Icon(
                                            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Shuffle),
                                            tint = TextPrimary,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .clickable {
                                                    playlistViewModel.startShuffled(songs)?.let { first ->
                                                        playlistViewModel.updateSongState(
                                                            first.coverUri,
                                                            first.title,
                                                            first.singer,
                                                            true,
                                                            first.id,
                                                            0,
                                                            playlist.name,
                                                        )
                                                        SongPlayer.playSong(first.url, context, "song/${first.id}")
                                                    }
                                                }
                                                .padding(12.dp),
                                            contentDescription = "Shuffle play",
                                        )
                                        if (playlistId.startsWith("local_pl_")) {
                                            Icon(
                                                imageVector = Icons.Default.Delete,
                                                contentDescription = "Delete Playlist",
                                                tint = TextPrimary,
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .clickable { showDeleteDialog = true }
                                                    .padding(12.dp)
                                            )
                                        }
                                    }
                                }
                            }
                            // Always visible: pause when playing, resume when this
                            // list's track is paused, otherwise start from the top.
                            if (songs.isNotEmpty()) {
                                val playing = playlistViewModel.currentSongPlayingState.value
                                val currentInList = songs.any { it.id == playlistViewModel.currentSongId.value }
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .size(56.dp)
                                        .shadow(12.dp, CircleShape, ambientColor = com.music.spotui.ui.theme.AccentDeep, spotColor = com.music.spotui.ui.theme.AccentDeep)
                                        .clip(CircleShape)
                                        .background(com.music.spotui.ui.theme.AccentBrush)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null
                                        ) {
                                            when {
                                                currentInList -> playlistViewModel.setPlaying(!playing)
                                                filteredSongs.isNotEmpty() -> {
                                                    playlistViewModel.updateQueue(filteredSongs)
                                                    playlistViewModel.updateSongState(
                                                        filteredSongs[0].coverUri,
                                                        filteredSongs[0].title,
                                                        filteredSongs[0].singer,
                                                        true,
                                                        filteredSongs[0].id,
                                                        0,
                                                        playlist.name
                                                    )
                                                    SongPlayer.playSong(filteredSongs[0].url, context, "song/${filteredSongs[0].id}")
                                                }
                                            }
                                        }
                                ) {
                                    Icon(
                                        modifier = Modifier.size(25.dp),
                                        tint = com.music.spotui.ui.theme.OnAccent,
                                        painter = if (currentInList && playing) painterResource(id = R.drawable.ic_playing) else androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.PlayArrow),
                                        contentDescription = if (currentInList && playing) "Pause" else "Play"
                                    )
                                }
                            }
                        }
                    }
                }

                // ── Search bar for playlist ──
                item {
                    AppSearchBar(
                        query = searchQuery,
                        onQueryChange = { searchQuery = it },
                        modifier = Modifier.padding(20.dp, 8.dp),
                        placeholder = "Search in playlist",
                    )
                }
                
                // ── Sort action ──
                item {
                    if (songs.isNotEmpty()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp, 0.dp, 20.dp, 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(SoloShape.pill)
                                    .background(Surface3)
                                    .clickable { showSortSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = currentSort.getDescriptiveLabel(isDescending),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.labelLarge)
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Sort Options",
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp)
                                )
                            }
                        }
                    }
                }

                itemsIndexed(filteredSongs, key = { _, song -> song.id }) { index, song ->
                    val currentColor = if (song.id == playlistViewModel.currentSongId.value)
                        Accent else TextPrimary

                    SwipeToPlayNextWrapper(
                        onPlayNext = {
                            playerViewModel.playNext(song)
                            android.widget.Toast.makeText(
                                context,
                                "${song.title} will play next",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Start,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Canvas)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onLongClick = { menuSong = song },
                                    onClick = {
                                        playlistViewModel.updateQueue(filteredSongs)
                                        playlistViewModel.updateSongState(
                                            song.coverUri,
                                            song.title,
                                            song.singer,
                                            true,
                                            song.id,
                                            index,
                                            playlist.name
                                        )
                                        SongPlayer.playSong(song.url, context, "song/${song.id}")
                                    },
                                )
                                .padding(20.dp, 8.dp)
                        ) {
                            GlideImage(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(SoloShape.xs),
                                model = song.coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                contentScale = ContentScale.Crop,
                                contentDescription = ""
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).width(280.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (song.explicit) {
                                        com.music.spotui.ui.components.ExplicitBadge()
                                        Spacer(Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = song.title,
                                        color = currentColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 1
                                    )
                                }
                                Text(
                                    text = song.singer,
                                    color = TextTertiary,
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1
                                )
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(160.dp)) }
            }
            com.music.spotui.ui.components.FastScrollbarForLazyList(
                state = listState,
                modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
            )
        }
        }
        if (showSortSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSortSheet = false },
                containerColor = Surface2,
                shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
                dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
                scrimColor = com.music.spotui.ui.theme.Scrim,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                ) {
                    Text(
                        text = "Sort by",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp)
                    )
                    HorizontalDivider(color = Surface3)
                    Spacer(modifier = Modifier.height(4.dp))
                    PlaylistSortOption.entries.forEach { option ->
                        val isSelected = option == currentSort
                        val icon = when (option) {
                            PlaylistSortOption.DATE -> Icons.Default.DateRange
                            PlaylistSortOption.TITLE -> Icons.AutoMirrored.Filled.List
                            PlaylistSortOption.ARTIST -> Icons.Default.Person
                            PlaylistSortOption.ALBUM -> Icons.Default.Menu
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val nextDesc = if (currentSort == option) {
                                        !isDescending
                                    } else {
                                        option == PlaylistSortOption.DATE
                                    }
                                    currentSort = option
                                    isDescending = nextDesc
                                    setPlaylistSort(context, playlistId, option, nextDesc)
                                    showSortSheet = false
                                }
                                .padding(16.dp, 14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = if (isSelected) Accent else TextPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                            Spacer(modifier = Modifier.width(18.dp))
                             Text(
                                 text = if (isSelected) option.getDescriptiveLabel(isDescending) else option.getDescriptiveLabel(option == PlaylistSortOption.DATE),
                                 color = if (isSelected) Accent else TextPrimary,
                                 style = MaterialTheme.typography.bodyLarge,
                                 modifier = Modifier.weight(1f)
                             )
                            if (isSelected) {
                                Icon(
                                    imageVector = if (isDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                    contentDescription = null,
                                    tint = Accent,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

