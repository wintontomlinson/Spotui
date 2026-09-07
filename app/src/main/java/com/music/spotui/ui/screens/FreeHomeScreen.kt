package com.music.spotui.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.viewmodel.FreeHomeViewModel
import com.music.spotui.ui.viewmodel.HomeRow
import com.music.spotui.ui.viewmodel.PlayerViewModel

/**
 * Login-free Home. No Spotify session needed — content is a set of curated
 * sections populated from YouTube search, and tapping a track plays it through
 * the normal queue/player. Also hosts a recreated search bar that opens the
 * free search screen.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FreeHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: FreeHomeViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val rows by vm.rows

    LaunchedEffect(Unit) { vm.loadOnce() }

    val play: (List<SongsModel>, Int) -> Unit = { list, index ->
        val song = list[index]
        playerViewModel.updateQueue(list)
        playerViewModel.updateSongState(
            song.coverUri, song.title, song.singer, true, song.id, index, song.album,
        )
        SongPlayer.playSong(song.url, context, "song/${song.id}")
        navController.navigate(Routes.Player.route)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 180.dp),
    ) {
        item {
            Column(modifier = Modifier.padding(16.dp, 14.dp, 16.dp, 6.dp)) {
                Text(
                    text = greeting(),
                    color = Color.White,
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                )
                Text(
                    text = "Free music, powered by YouTube — no login needed.",
                    color = Color(0xFFB3B3B3),
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp),
                )
                Spacer(Modifier.height(14.dp))
                // Search lives on the Explore tab — Home only shows mood chips.
                // YouTube Music-style mood chips.
                MoodChips(onPick = { query ->
                    navController.navigate("${Routes.YtSearch.route}?q=${android.net.Uri.encode(query)}")
                })
            }
        }

        items(rows) { row ->
            HomeRowSection(row = row, onPlay = play)
        }

        item { Spacer(Modifier.height(24.dp)) }
    }
}

private val MOODS = listOf(
    "Energize" to "energetic upbeat songs",
    "Relax" to "relaxing chill songs",
    "Focus" to "focus concentration music",
    "Workout" to "workout gym music",
    "Feel good" to "feel good happy songs",
    "Romance" to "romantic love songs",
    "Party" to "party dance hits",
    "Sad" to "sad emotional songs",
)

@Composable
private fun MoodChips(onPick: (String) -> Unit) {
    LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        items(MOODS) { (label, query) ->
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFF1F1F24))
                    .clickable { onPick(query) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun HomeRowSection(row: HomeRow, onPlay: (List<SongsModel>, Int) -> Unit) {
    if (!row.loading && row.tracks.isEmpty()) return // skip empty sections silently

    Text(
        text = row.title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 20.dp, bottom = 12.dp),
    )

    if (row.loading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp),
            contentAlignment = Alignment.Center,
        ) {
            CircularProgressIndicator(color = Color(0xFFFF0033), strokeWidth = 2.dp)
        }
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        items(row.tracks) { song ->
            val index = row.tracks.indexOf(song)
            TrackCard(song = song, onClick = { onPlay(row.tracks, index) })
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TrackCard(song: SongsModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(150.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(6.dp),
    ) {
        GlideImage(
            modifier = Modifier
                .size(138.dp)
                .clip(RoundedCornerShape(8.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = "",
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
        )
        Text(
            text = song.singer,
            color = Color(0xFFB3B3B3),
            fontSize = 12.sp,
            maxLines = 1,
        )
    }
}

private fun greeting(): String {
    // Simple, locale-agnostic greeting (no java.time dependency).
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when {
        hour < 12 -> "Good morning"
        hour < 17 -> "Good afternoon"
        else -> "Good evening"
    }
}
