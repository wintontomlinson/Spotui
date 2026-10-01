package com.music.spotui.ui.screens

import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.local.LocalImport
import com.music.spotui.data.preferences.addLocalTracks
import com.music.spotui.data.preferences.getLocalSongs
import com.music.spotui.data.preferences.removeLocalTrack
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun LocalFilesScreen(navController: NavController) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val accent = Accent

    var songs by remember { mutableStateOf(getLocalSongs(context)) }
    var importing by remember { mutableStateOf(false) }

    fun toast(msg: String) =
        android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_SHORT).show()

    val addSongs = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenMultipleDocuments(),
    ) { uris ->
        if (uris.isNullOrEmpty()) return@rememberLauncherForActivityResult
        importing = true
        scope.launch(Dispatchers.IO) {
            val tracks = LocalImport.importFiles(context, uris)
            addLocalTracks(context, tracks)
            val fresh = getLocalSongs(context)
            withContext(Dispatchers.Main) {
                songs = fresh
                importing = false
                toast("Imported ${tracks.size} song${if (tracks.size == 1) "" else "s"}")
            }
        }
    }

    val addFolder = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree(),
    ) { treeUri ->
        if (treeUri == null) return@rememberLauncherForActivityResult
        importing = true
        scope.launch(Dispatchers.IO) {
            val tracks = LocalImport.importFolder(context, treeUri)
            addLocalTracks(context, tracks)
            val fresh = getLocalSongs(context)
            withContext(Dispatchers.Main) {
                songs = fresh
                importing = false
                toast("Imported ${tracks.size} song${if (tracks.size == 1) "" else "s"} from folder")
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize().background(Canvas),
    ) {
        val scrollState = rememberScrollState()
        // The top bar turns to strong glass once the header has scrolled away.
        val barCollapsed by remember { androidx.compose.runtime.derivedStateOf { scrollState.value > 400 } }
        Scaffold(
            containerColor = Color.Transparent,
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
                    title = { Text("Local files", color = TextPrimary, fontWeight = FontWeight.Bold) },
                )
            },
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Canvas)
                    .consumeWindowInsets(innerPadding)
                    .padding(top = innerPadding.calculateTopPadding(), bottom = innerPadding.calculateBottomPadding())
                    .verticalScroll(scrollState),
            ) {
                Text(
                    text = "On this device",
                    color = TextPrimary,
                    style = MaterialTheme.typography.headlineLarge,
                    fontFamily = com.music.spotui.ui.theme.SoloDisplay,
                    modifier = Modifier.padding(20.dp, 8.dp, 20.dp, 2.dp),
                )
                Text(
                    text = if (songs.isEmpty()) "Import FLAC, MP3, WAV and more" else "${songs.size} songs",
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(20.dp, 0.dp, 20.dp, 12.dp),
                )

                // Import buttons
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp, 0.dp, 20.dp, 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ImportButton("Add songs", accent, Modifier.weight(1f), enabled = !importing) {
                        addSongs.launch(arrayOf("audio/*"))
                    }
                    ImportButton("Add folder", Surface3, Modifier.weight(1f), enabled = !importing) {
                        addFolder.launch(null)
                    }
                }
                if (importing) {
                    Text(
                        "Importing…",
                        color = accent,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp, 2.dp),
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (songs.isEmpty()) {
                    com.music.spotui.ui.components.SoloEmptyState(
                        icon = androidx.compose.material.icons.Icons.Rounded.LibraryMusic,
                        title = "No local music yet",
                        message = "Tap “Add songs” or “Add folder” to import music from your device.",
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    songs.forEachIndexed { index, song ->
                        val currentColor = if (song.id == playerViewModel.currentSongId.value)
                            Accent else TextPrimary
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(20.dp, 8.dp)
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onLongClick = {
                                        removeLocalTrack(context, song.url)
                                        songs = getLocalSongs(context)
                                        toast("Removed from library")
                                    },
                                    onClick = {
                                        playerViewModel.updateQueue(songs)
                                        playerViewModel.updateSongState(
                                            song.coverUri, song.title, song.singer,
                                            true, song.id, index, song.album,
                                        )
                                        SongPlayer.playSong(song.url, context, "song/${song.id}")
                                    },
                                ),
                        ) {
                            GlideImage(
                                modifier = Modifier.size(48.dp).clip(SoloShape.xs),
                                model = song.coverUri,
                                failure = placeholder(R.drawable.placeholder),
                                loading = placeholder(R.drawable.placeholder),
                                contentScale = ContentScale.Crop,
                                contentDescription = "",
                            )
                            Column(modifier = Modifier.padding(start = 12.dp).width(280.dp)) {
                                Text(song.title, color = currentColor, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1)
                                Text(song.singer, color = TextTertiary, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, maxLines = 1)
                            }
                        }
                    }
                    Text(
                        text = "Long-press a song to remove it from your library.",
                        color = TextTertiary,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(20.dp, 12.dp),
                    )
                }

                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun ImportButton(
    label: String,
    container: Color,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    Text(
        text = label,
        color = if (container == Surface3) TextPrimary else Color.Black,
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.Bold,
        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        modifier = modifier
            .clip(SoloShape.pill)
            .background(if (enabled) container else Surface3)
            .clickable(enabled = enabled) { onClick() }
            .padding(vertical = 12.dp),
    )
}
