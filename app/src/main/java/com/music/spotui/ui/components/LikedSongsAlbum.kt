package com.music.spotui.ui.components

import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.layout.ContentScale
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
import com.music.spotui.data.entity.AlbumsModel
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.addLikedSongId
import com.music.spotui.data.preferences.getLikedSongIds
import com.music.spotui.data.preferences.getSongsByIds
import com.music.spotui.data.preferences.isSongLiked
import com.music.spotui.data.preferences.removeLikedSongId
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.AlbumViewModel
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
// SwipeToPlayNextWrapper is in the same package and automatically visible

@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun LikedSongsScreen(
    albums: List<AlbumsModel>,
    songs: List<SongsModel>,
    navController: NavController,
    context: Context
) {
    val albumViewModel : AlbumViewModel =  hiltViewModel()
    val playerViewModel : PlayerViewModel = hiltViewModel()


    // There is no guarantee a "Liked Songs" entry exists in the album list, and
    // indexing it blindly crashed this screen whenever the list was empty.
    val likedAlbumCover = albums.firstOrNull { it.name == "Liked Songs" }?.coverUri
    var likedSongs by remember { mutableStateOf(emptyList<SongsModel>()) }

    var dominentColor by remember {
        mutableStateOf(com.music.spotui.ui.theme.Canvas)
    }
    // Once per cover, not on every recomposition.
    LaunchedEffect(likedAlbumCover) {
        if (!likedAlbumCover.isNullOrBlank()) {
            Palette().extractSecondColorFromCoverUrl(context = context, likedAlbumCover) { color ->
                dominentColor = color
            }
        }
    }
    val likeState = albumViewModel.likeState.value
    LaunchedEffect(likeState, songs) {
        // Resolve from the catalogue when it is available, and fall back to the copies
        // saved at like time so this works with no account at all.
        likedSongs = com.music.spotui.data.preferences.resolveLikedSongs(context, songs)
            .sortedBy { it.title }
    }


    val scrollState = rememberScrollState()
    // The top bar turns to strong glass once the header has scrolled away.
    val barCollapsed by remember { androidx.compose.runtime.derivedStateOf { scrollState.value > 400 } }
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
                        text = "Liked Songs",
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
            )
        }
    ){


        Column(modifier = Modifier
            .fillMaxSize()
            .background(Canvas)
            .verticalScroll(scrollState)
        ) {

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
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
                        // Fall back to the first liked track's art when the library has
                        // no Liked Songs entry to take a cover from.
                        model = likedAlbumCover ?: likedSongs.firstOrNull()?.coverUri,
                        failure = placeholder(R.drawable.placeholder),
                        contentDescription = "",
                    )
                }
                Spacer(modifier = Modifier.padding(20.dp))



                Row(horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(25.dp, 0.dp)
                ){

                    Column(
                        modifier = Modifier
                            .width(200.dp)
                    ) {
                        Text(modifier = Modifier,
                            text = "Liked Songs",
                            color = TextPrimary,
                            style = MaterialTheme.typography.headlineLarge,
                            fontFamily = com.music.spotui.ui.theme.SoloDisplay)
                        Text(modifier = Modifier,
                            text = " ${likedSongs.size} songs",
                            color = TextTertiary,
                            letterSpacing = 0.sp,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium)
                    }


                    Row(
                        horizontalArrangement = Arrangement.spacedBy(0.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (likedSongs.isNotEmpty()) {
                            Icon(
                                painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.PlaylistAdd),
                                tint = TextPrimary,
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .clickable {
                                        playerViewModel.addAllToQueue(likedSongs)
                                        android.widget.Toast.makeText(
                                            context,
                                            "${likedSongs.size} track(s) added to queue",
                                            android.widget.Toast.LENGTH_SHORT,
                                        ).show()
                                    }
                                    .padding(12.dp),
                                contentDescription = "Add to queue",
                            )
                        }

                        if (!albumViewModel.currentSongPlayingState.value && likedSongs.isNotEmpty()) {
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
                                        // Nothing liked yet means nothing to play, and
                                        // indexing the empty list here crashed the screen.
                                        val first = likedSongs.firstOrNull()
                                        if (first != null) {
                                            albumViewModel.updateQueue(likedSongs)
                                            albumViewModel.updateSongState(
                                                first.coverUri,
                                                first.title,
                                                first.singer,
                                                true,
                                                first.id,
                                                0,
                                                "Liked Songs"
                                            )
                                            SongPlayer.playSong(first.url, context, "song/${first.id}")
                                        }
                                    }
                            ) {
                                Icon(
                                    modifier = Modifier.size(25.dp),
                                    tint = com.music.spotui.ui.theme.OnAccent,
                                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.PlayArrow),
                                    contentDescription = ""
                                )
                            }
                        }
                    }


                }

            }


            if(likedSongs.isNotEmpty()){
                repeat(likedSongs.size) {song ->

                    // Key the per row state to the track id, not the loop slot. Without a
                    // key, remember binds to the composition position, so when the liked
                    // list changes (a song removed or re-sorted) a row kept the previous
                    // track's liked heart and could open the saved-in sheet for the wrong
                    // song. Keying on the id moves the state with the track.
                    val rowSongId = likedSongs[song].id
                    var isLiked by remember(rowSongId) {
                        mutableStateOf(isSongLiked(context, rowSongId.toString()))
                    }
                    var showSavedIn by remember(rowSongId) { mutableStateOf(false) }
                    if (showSavedIn) {
                        SavedInSheet(
                            song = likedSongs[song],
                            context = context,
                            onDismiss = { showSavedIn = false },
                            onLikedChanged = { isLiked = it },
                        )
                    }
                    val songId = likedSongs[song].id
                    val currentPlayingIndicatorColor = if(songId == albumViewModel.currentSongId.value) com.music.spotui.ui.theme.Accent else TextPrimary

                    SwipeToPlayNextWrapper(
                        onPlayNext = {
                            playerViewModel.playNext(likedSongs[song])
                            android.widget.Toast.makeText(
                                context,
                                "${likedSongs[song].title} will play next",
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
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    albumViewModel.updateQueue(likedSongs)
                                    albumViewModel.updateSongState(
                                        likedSongs[song].coverUri,
                                        likedSongs[song].title,
                                        likedSongs[song].singer,
                                        true,
                                        likedSongs[song].id,
                                        song,
                                        "Liked Songs"
                                    )
                                    SongPlayer.playSong(likedSongs[song].url, context, "song/${likedSongs[song].id}")
                                }
                                .padding(20.dp, 8.dp)
                        ) {

                            Row(
                                horizontalArrangement = Arrangement.Start,
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.width(280.dp)
                            ) {
                                GlideImage(
                                    modifier = Modifier
                                        .padding(0.dp, 0.dp, 10.dp, 0.dp)
                                        .size(50.dp),
                                    model = likedSongs[song].coverUri,
                                    contentScale = ContentScale.Crop,
                                    failure = placeholder(R.drawable.placeholder),
                                    loading = placeholder(R.drawable.placeholder),
                                    contentDescription = ""
                                )
                                Column {
                                    Text(
                                        text = likedSongs[song].title,
                                        color = currentPlayingIndicatorColor,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = likedSongs[song].singer,
                                        color = TextTertiary,
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
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
                                            albumViewModel.updateLikeState(!albumViewModel.likeState.value)
                                        },
                                        onLongClick = { showSavedIn = true },
                                    )
                                    .padding(14.dp),
                                painter = if (isLiked){
                                    androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.CheckCircle)
                                }
                                else{
                                    androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.AddCircleOutline)
                                }
                                ,
                                tint = TextSecondary,
                                contentDescription = ""
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.padding(80.dp))
        }

    }

}