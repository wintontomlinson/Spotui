package com.music.spotui.ui.screens

import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Person
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
import androidx.compose.ui.text.TextStyle
import com.music.spotui.data.preferences.AlbumSortOption
import com.music.spotui.data.preferences.getAlbumSortOption
import com.music.spotui.data.preferences.isAlbumSortDescending
import com.music.spotui.data.preferences.setAlbumSortOption
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
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
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.addLikedAlbumId
import com.music.spotui.data.preferences.addLikedSongId
import com.music.spotui.data.preferences.isAlbumLiked
import com.music.spotui.data.preferences.isSongLiked
import com.music.spotui.data.preferences.removeLikedAlbumId
import com.music.spotui.data.preferences.removeLikedSongId
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.AppSearchBar
import com.music.spotui.ui.components.LikedSongsScreen
import com.music.spotui.ui.components.Loader
import com.music.spotui.ui.components.SavedInSheet
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.AlbumViewModel
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.components.SwipeToPlayNextWrapper
import kotlinx.coroutines.delay
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3


@Composable
fun AlbumScreen(
    navController: NavController,
    albumName: String,
    artist: String = "",
    albumBrowseId: String = "",
) {


    val albumViewModel : AlbumViewModel = hiltViewModel()
    val songs by albumViewModel.songs.collectAsState()
    val albums by albumViewModel.albums.collectAsState()

    // Load this album's real tracklist. When the exact album id is known it is used
    // directly, otherwise the name and artist are resolved against YouTube Music.
    LaunchedEffect(albumName, artist, albumBrowseId) {
        albumViewModel.loadAlbumSongs(albumName, artist, albumBrowseId)
    }

    val context = LocalContext.current





    Surface(
        modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
    ) {
        val albumsResponse = (albums as? Response.Success)?.data.orEmpty()
        val songsResponse = (songs as? Response.Success)?.data.orEmpty()

        when {
            albums is Response.Loading && songs is Response.Loading -> {
                Loader()
            }

            else -> {
                if (albumName == "Liked Songs"){
                    LikedSongsScreen(albumsResponse, songsResponse, navController, context)
                }
                else{
                    SumUpAlbumScreen(navController = navController,albumViewModel, albumsResponse, songsResponse, albumName, context, artist)
                }
            }
        }
    }

}
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun SumUpAlbumScreen(
    navController: NavController,
    albumViewModel: AlbumViewModel,
    albums: List<AlbumsModel>,
    songs: List<SongsModel>,
    albumName: String,
    context: Context,
    artist: String = ""
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    // `songs` is already this album's track list (loaded by AlbumViewModel).
    val albumSongs: List<SongsModel> = songs

    // Warm the stream cache for the first few tracks so the first tap plays
    // (near-)instantly instead of resolving YouTube on the tap.
    LaunchedEffect(albumSongs) {
        if (albumSongs.isNotEmpty()) {
            SongPlayer.prefetchList(albumSongs.map { it.url }, context)
        }
    }

    val albumByName : Map<String, List<AlbumsModel>> = albums.groupBy { it.name }
    // The album may not be in the cached new-releases list (e.g. opened from
    // search), fall back to a model built from the album's first track.
    val album : List<AlbumsModel> = albumByName[albumName]
        ?: listOf(
            AlbumsModel(
                id = albumName.hashCode() and 0x7fffffff,
                artists = albumSongs.firstOrNull()?.singer ?: artist,
                coverUri = albumSongs.firstOrNull()?.coverUri ?: "",
                name = albumName,
                time = "",
            )
        )

    val currentAlbum = album.firstOrNull()
    LaunchedEffect(albumSongs, currentAlbum) {
        if (albumSongs.isNotEmpty() && currentAlbum != null) {
            com.music.spotui.data.preferences.OfflineCollectionsPref.saveCollection(
                context = context,
                id = "album:$albumName|$artist",
                name = albumName,
                coverUri = currentAlbum.coverUri,
                artists = currentAlbum.artists,
                isPlaylist = false,
                songs = albumSongs
            )
        }
    }

    var dominentColor by remember {
        mutableStateOf(com.music.spotui.ui.theme.Canvas)
    }
    // Once per cover, not on every recomposition.
    val albumCover = album[0].coverUri
    LaunchedEffect(albumCover) {
        if (albumCover.isNotBlank()) {
            Palette().extractSecondColorFromCoverUrl(context = context, albumCover) { color ->
                dominentColor = color
            }
        }
    }

    var isAlbumLiked by remember { mutableStateOf( isAlbumLiked(context, album[0].id.toString())) }

    var snackbarMessage by remember {
        mutableStateOf("")
    }
    var snackbarVisible by remember {
        mutableStateOf(false)
    }

    var searchQuery by remember(albumName) { mutableStateOf("") }
    var currentSort by remember(albumName) { mutableStateOf(getAlbumSortOption(context, albumName)) }
    var isDescending by remember(albumName) { mutableStateOf(isAlbumSortDescending(context, albumName)) }
    var showSortSheet by remember { mutableStateOf(false) }

    val filteredSongs = remember(albumSongs, searchQuery, currentSort, isDescending) {
        val filtered = if (searchQuery.isBlank()) {
            albumSongs
        } else {
            albumSongs.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.singer.contains(searchQuery, ignoreCase = true)
            }
        }
        when (currentSort) {
            AlbumSortOption.DEFAULT -> if (isDescending) filtered.reversed() else filtered
            AlbumSortOption.TITLE -> if (isDescending) filtered.sortedByDescending { it.title.lowercase() } else filtered.sortedBy { it.title.lowercase() }
            AlbumSortOption.ARTIST -> if (isDescending) filtered.sortedByDescending { it.singer.lowercase() } else filtered.sortedBy { it.singer.lowercase() }
            AlbumSortOption.DURATION -> if (isDescending) filtered.sortedByDescending { it.durationMs } else filtered.sortedBy { it.durationMs }
        }
    }

    val albumArtists = album[0].artists.ifBlank { albumSongs.firstOrNull()?.singer ?: artist }
    val albumArtistList = remember(albumArtists) {
        albumArtists.split(",").map { it.trim() }.filter { it.isNotBlank() }
    }
    var showArtistSheet by remember { mutableStateOf(false) }
    if (showArtistSheet) {
        ArtistsSheet(
            artistNames = albumArtistList,
            context = context,
            onDismiss = { showArtistSheet = false },
            navController = navController,
        )
    }

    var menuSong by remember { mutableStateOf<SongsModel?>(null) }
    menuSong?.let { sel ->
        com.music.spotui.ui.components.SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = { menuSong = null },
        )
    }
    LaunchedEffect(snackbarVisible) {
        delay(1500)
        snackbarVisible = false
    }



    val scrollState = androidx.compose.foundation.rememberScrollState()
    // The top bar turns to strong glass once the hero has scrolled away.
    val barCollapsed by remember { androidx.compose.runtime.derivedStateOf { scrollState.value > 600 } }
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
                        text = albumName,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            )
        }
    ){

        Box(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier
                .fillMaxSize()
                .background(Canvas)
                .verticalScroll(scrollState)
            ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(460.dp)
                    .background(
                        brush = Brush.verticalGradient(
                            colors = listOf(com.music.spotui.ui.theme.artworkTone(dominentColor), Canvas),
                            startY = -100f,

                            ),

                        )
                ,
                verticalArrangement = Arrangement.Center,
            ) {
                Spacer(modifier = Modifier.padding(25.dp))

                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    GlideImage(
                        modifier = Modifier
                            .size(200.dp)
                            .shadow(18.dp, SoloShape.lg, ambientColor = com.music.spotui.ui.theme.Shadow, spotColor = com.music.spotui.ui.theme.Shadow)
                            .clip(SoloShape.lg),
                        contentScale = ContentScale.Crop,
                        model = album[0].coverUri,
                        failure = placeholder(R.drawable.placeholder),
                        contentDescription = "",
                    )
                }
                Spacer(modifier = Modifier.padding(5.dp))
                Text(modifier = Modifier
                    .padding(20.dp, 5.dp, 0.dp, 0.dp),
                    text = albumName,
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = com.music.spotui.ui.theme.SoloDisplay)
                if (albumArtists.isNotBlank()) {
                    Text(
                        modifier = Modifier
                            .padding(20.dp, 0.dp, 0.dp, 0.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) {
                                if (albumArtistList.size == 1) {
                                    navController.navigate(artistRoute(albumArtistList[0]))
                                } else if (albumArtistList.size > 1) {
                                    showArtistSheet = true
                                }
                            },
                        text = albumArtists,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Medium
                    )
                }
                Text(modifier = Modifier
                    .padding(20.dp, 0.dp, 0.dp, 0.dp),
                    text = "Album : ${album[0].time}",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Medium
                )

                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .padding(20.dp, 0.dp)
                ){

                    if (snackbarVisible){
                            Snackbar(showMessage = snackbarMessage)
                        }
                    else{
                        // Let the action icons take their natural width, a fixed
                        // 75dp squeezed the add + download buttons together.
                        Row(horizontalArrangement = Arrangement.spacedBy(0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {

                            GlideImage(
                                modifier = Modifier
                                    .height(60.dp)
                                    .width(32.dp)
                                    .padding(0.dp, 5.dp)
                                    .clip(SoloShape.xs)
                                ,
                                model = album[0].coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                contentScale = ContentScale.Crop,
                                contentDescription = "",
                            )
                            Icon(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (isAlbumLiked) {
                                            removeLikedAlbumId(context, album[0].id.toString())
                                            snackbarMessage = "Removed from Library"
                                        } else {
                                            addLikedAlbumId(context, album[0].id.toString())
                                            snackbarMessage = "Added to Library"
                                        }
                                        isAlbumLiked = isAlbumLiked(context, album[0].id.toString())
                                        snackbarVisible = true

                                    }
                                    .padding(12.5.dp),
                                painter = if (isAlbumLiked){
                                    androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.CheckCircle)
                                }
                                else{
                                    androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.AddCircleOutline)
                                }
                                ,
                                tint = if (isAlbumLiked){
                                    Accent
                                }
                                else{
                                    TextPrimary
                                },
                                contentDescription = ""
                            )
                            // Download the whole album (all tracks) for offline playback.
                            var albumDownloaded by remember(albumSongs) {
                                mutableStateOf(SongPlayer.allDownloaded(albumSongs, context))
                            }
                            Icon(
                                imageVector = if (albumDownloaded)
                                    Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Rounded.Download,
                                tint = if (albumDownloaded) Accent else TextPrimary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        if (albumSongs.isNotEmpty()) {
                                            val currentAlbum = album.firstOrNull()
                                            com.music.spotui.data.preferences.OfflineCollectionsPref.saveCollection(
                                                context = context,
                                                id = "album:$albumName|$artist",
                                                name = albumName,
                                                coverUri = currentAlbum?.coverUri ?: albumSongs.firstOrNull()?.coverUri ?: "",
                                                artists = currentAlbum?.artists ?: albumSongs.firstOrNull()?.singer ?: artist,
                                                isPlaylist = false,
                                                songs = albumSongs
                                            )
                                            if (!albumDownloaded) {
                                                SongPlayer.downloadAll(albumSongs, context)
                                                snackbarMessage = "Downloading ${albumSongs.size} tracks…"
                                                snackbarVisible = true
                                            } else {
                                                snackbarMessage = "Album added to offline library"
                                                snackbarVisible = true
                                            }
                                        }
                                    }
                                    .padding(12.dp),
                                contentDescription = "Download album",
                            )
                            if (albumSongs.isNotEmpty()) {
                                Icon(
                                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.PlaylistAdd),
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .clickable {
                                            playerViewModel.addAllToQueue(albumSongs)
                                            android.widget.Toast.makeText(
                                                context,
                                                "${albumSongs.size} track(s) added to queue",
                                                android.widget.Toast.LENGTH_SHORT,
                                            ).show()
                                        }
                                        .padding(12.dp),
                                    contentDescription = "Add to queue",
                                )
                            }
                            // Shuffle-play: start the album in random order.
                            Icon(
                                painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Shuffle),
                                tint = TextPrimary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        albumViewModel.startShuffled(albumSongs)?.let { first ->
                                            albumViewModel.updateSongState(
                                                first.coverUri,
                                                first.title,
                                                first.singer,
                                                true,
                                                first.id,
                                                0,
                                                albumName,
                                            )
                                            SongPlayer.playSong(first.url, context, "song/${first.id}")
                                        }
                                    }
                                    .padding(12.dp),
                                contentDescription = "Shuffle play",
                            )
                        }


                        // Always visible: pause when playing, resume when this
                        // album's track is paused, otherwise start from the top.
                        if (albumSongs.isNotEmpty()) {
                            val playing = albumViewModel.currentSongPlayingState.value
                            val currentInList = albumSongs.any { it.id == albumViewModel.currentSongId.value }
                            androidx.compose.foundation.layout.Box(
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
                                            currentInList -> albumViewModel.setPlaying(!playing)
                                            else -> {
                                                albumViewModel.updateQueue(albumSongs)
                                                albumViewModel.updateSongState(
                                                    albumSongs[0].coverUri,
                                                    albumSongs[0].title,
                                                    albumSongs[0].singer,
                                                    true,
                                                    albumSongs[0].id,
                                                    0,
                                                    albumName
                                                )
                                                SongPlayer.playSong(albumSongs[0].url, context, "song/${albumSongs[0].id}")
                                            }
                                        }
                                    }
                            ) {
                                Icon(
                                    modifier = Modifier
                                        .size(25.dp),
                                    tint = com.music.spotui.ui.theme.OnAccent,
                                    painter = if (currentInList && playing) painterResource(id = R.drawable.ic_playing) else androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.PlayArrow),
                                    contentDescription = if (currentInList && playing) "Pause" else "Play")
                            }
                        }
                    }




                }

            }


            if (albumSongs.isNotEmpty()) {
                // Search Bar
                AppSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it },
                    modifier = Modifier.padding(20.dp, 8.dp),
                    placeholder = "Search in album",
                )

                // Sort action button
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

                if (filteredSongs.isEmpty() && searchQuery.isNotBlank()) {
                    com.music.spotui.ui.components.SoloEmptyState(
                        icon = androidx.compose.material.icons.Icons.Rounded.SearchOff,
                        title = "No matches",
                        message = "Nothing here matches \"$searchQuery\".",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    repeat(filteredSongs.size) { songIdx ->
                        val targetSong = filteredSongs[songIdx]

                        var isLiked by remember(targetSong.id) {
                            mutableStateOf(isSongLiked(context, targetSong.id.toString()))
                        }
                        var showSavedIn by remember { mutableStateOf(false) }
                        if (showSavedIn) {
                            SavedInSheet(
                                song = targetSong,
                                context = context,
                                onDismiss = { showSavedIn = false },
                                onLikedChanged = { isLiked = it },
                            )
                        }
                        val likeState = albumViewModel.likeState.value
                        LaunchedEffect(likeState) {
                            isLiked = isSongLiked(context, targetSong.id.toString())
                        }
                        val songId = targetSong.id

                        val currentPlayingIndicatorColor = if (songId == albumViewModel.currentSongId.value) Accent else TextPrimary

                        SwipeToPlayNextWrapper(
                            onPlayNext = {
                                playerViewModel.playNext(targetSong)
                                android.widget.Toast.makeText(
                                    context,
                                    "${targetSong.title} will play next",
                                    android.widget.Toast.LENGTH_SHORT
                                ).show()
                            }
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Canvas)
                                    .combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = null,
                                        onLongClick = { menuSong = targetSong },
                                        onClick = {
                                            albumViewModel.updateQueue(filteredSongs)
                                            albumViewModel.updateSongState(
                                                targetSong.coverUri,
                                                targetSong.title,
                                                targetSong.singer,
                                                true,
                                                targetSong.id,
                                                songIdx,
                                                albumName
                                            )
                                            SongPlayer.playSong(targetSong.url, context, "song/${targetSong.id}")
                                        },
                                    )
                                    .padding(20.dp, 8.dp)
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.Start,
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.width(200.dp)
                                ) {
                                    Column {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            if (targetSong.explicit) {
                                                com.music.spotui.ui.components.ExplicitBadge()
                                                Spacer(Modifier.width(4.dp))
                                            }
                                            Text(
                                                text = targetSong.title,
                                                color = currentPlayingIndicatorColor,
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                            )
                                        }
                                        Text(
                                            text = targetSong.singer,
                                            color = TextTertiary,
                                            style = MaterialTheme.typography.bodySmall,
                                            fontWeight = FontWeight.Medium,
                                            maxLines = 1,
                                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                Icon(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .combinedClickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                if (isLiked) {
                                                    removeLikedSongId(context, songId.toString())
                                                } else {
                                                    addLikedSongId(context, songId.toString())
                                                }
                                                isLiked = isSongLiked(context, songId.toString())
                                                albumViewModel.updateLikeState(!albumViewModel.likeState.value)
                                            },
                                            onLongClick = { showSavedIn = true },
                                        )
                                        .padding(14.dp),
                                    painter = if (isLiked) {
                                        androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.CheckCircle)
                                    } else {
                                        androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.AddCircleOutline)
                                    },
                                    tint = if (isLiked) {
                                        TextPrimary
                                    } else {
                                        TextTertiary
                                    },
                                    contentDescription = ""
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(160.dp))
        }
        com.music.spotui.ui.components.FastScrollbarForScrollState(
            state = scrollState,
            modifier = Modifier.align(androidx.compose.ui.Alignment.CenterEnd)
        )
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
                    androidx.compose.material3.HorizontalDivider(color = Surface3)
                    Spacer(modifier = Modifier.height(4.dp))
                    AlbumSortOption.entries.forEach { option ->
                        val isSelected = option == currentSort
                        val icon = when (option) {
                            AlbumSortOption.DEFAULT -> Icons.Default.DateRange
                            AlbumSortOption.TITLE -> Icons.AutoMirrored.Filled.List
                            AlbumSortOption.ARTIST -> Icons.Default.Person
                            AlbumSortOption.DURATION -> Icons.Default.Menu
                        }

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (currentSort == option) {
                                        isDescending = !isDescending
                                    } else {
                                        currentSort = option
                                        isDescending = (option != AlbumSortOption.DEFAULT)
                                    }
                                    setAlbumSortOption(context, albumName, currentSort, isDescending)
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
                                text = if (isSelected) option.getDescriptiveLabel(isDescending) else option.getDescriptiveLabel(option != AlbumSortOption.DEFAULT),
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
