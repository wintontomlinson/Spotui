package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.viewmodel.YtSearchViewModel

/**
 * Login-free search & play. Type a song name, get YouTube Music results, tap to
 * play — no Spotify session required. Results are mapped to [SongsModel] and go
 * through the exact same queue + player as the rest of the app.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun YtSearchScreen(navController: NavController, initialQuery: String = "") {
    val context = LocalContext.current
    val vm: YtSearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()

    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }

    // Pre-fill and run a search when opened from a Home mood chip; otherwise put
    // the cursor in the search box so typing starts immediately (YT behaviour).
    androidx.compose.runtime.LaunchedEffect(initialQuery) {
        if (initialQuery.isNotBlank() && vm.query.value != initialQuery) {
            vm.onQueryChange(initialQuery)
            vm.search(initialQuery)
        } else if (initialQuery.isBlank() && vm.query.value.isBlank()) {
            runCatching { searchFocus.requestFocus() }
        }
    }

    val query by vm.query
    val results by vm.results
    val isLoading by vm.isLoading
    val error by vm.error
    val hasSearched by vm.hasSearched

    val playResult: (SongsModel, Int) -> Unit = { song, index ->
        // Queue the whole result list so next/previous work through the results.
        playerViewModel.updateQueue(results)
        playerViewModel.updateSongState(
            song.coverUri, song.title, song.singer, true, song.id, index, song.album,
        )
        SongPlayer.playSong(song.url, context, "song/${song.id}")
        navController.navigate(Routes.Player.route)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        // Professional YouTube-style search bar: a rounded pill in a top bar.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFF1F1F24))
                .padding(start = 16.dp, end = 8.dp)
                .height(48.dp),
        ) {
            Icon(
                Icons.Default.Search,
                contentDescription = null,
                tint = Color(0xFFB3B3B3),
                modifier = Modifier.size(22.dp),
            )
            BasicTextField(
                value = query,
                onValueChange = { vm.onQueryChangeDebounced(it) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = Color.White,
                    fontSize = 16.sp,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(Color(0xFFFF0033)),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { vm.search() }),
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 12.dp)
                    .focusRequester(searchFocus),
                decorationBox = { inner ->
                    if (query.isEmpty()) {
                        Text(
                            "Search songs, artists, albums",
                            color = Color(0xFF808080),
                            fontSize = 16.sp,
                        )
                    }
                    inner()
                },
            )
            if (query.isNotEmpty()) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Clear",
                    tint = Color(0xFFB3B3B3),
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(50))
                        .clickable { vm.clear() }
                        .padding(9.dp),
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && results.isEmpty() -> {
                    CircularProgressIndicator(
                        color = Color(0xFFFF0033),
                        modifier = Modifier.align(Alignment.TopCenter).padding(top = 40.dp),
                    )
                }
                results.isNotEmpty() -> {
                    LazyColumn(contentPadding = PaddingValues(bottom = 160.dp)) {
                        items(results, key = { it.id }) { song ->
                            val index = results.indexOf(song)
                            YtResultRow(song = song, onClick = { playResult(song, index) })
                        }
                    }
                }
                hasSearched && !isLoading -> {
                    EmptyHint(
                        icon = false,
                        text = error ?: "No results found.",
                    )
                }
                else -> {
                    EmptyHint(
                        icon = true,
                        text = "Search for any song and play it instantly.",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun YtResultRow(song: SongsModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(6.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = "",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp, end = 8.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Text(
                text = "Song • ${song.singer}",
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EmptyHint(icon: Boolean, text: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        if (icon) {
            Icon(
                Icons.Default.MusicNote,
                contentDescription = null,
                tint = Color(0xFF404040),
                modifier = Modifier.size(56.dp),
            )
            Spacer(Modifier.height(12.dp))
        }
        Text(
            text = text,
            color = Color(0xFF808080),
            fontSize = 14.sp,
        )
    }
}
