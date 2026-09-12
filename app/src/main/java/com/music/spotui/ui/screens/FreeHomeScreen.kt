package com.music.spotui.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.border
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.navBarScroll
import com.music.spotui.ui.viewmodel.FreeHomeViewModel
import com.music.spotui.ui.viewmodel.HomeRow
import com.music.spotui.ui.viewmodel.PlayerViewModel

// Accent comes from the single source of truth in the theme package.
private val Accent = com.music.spotui.ui.theme.Accent
private val OnAccent = com.music.spotui.ui.theme.OnAccent
private val Surface = Color(0xFF082C34)
private val SurfaceHigh = Color(0xFF0C3A44)
private val Hairline = Color(0x14FFFFFF)
private val TextDim = Color(0xFFB3B3B3)

/**
 * Login free Home. No Spotify session needed. Content is a set of curated and
 * personalised sections populated from YouTube search, and tapping a track plays
 * it through the normal queue and player.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun FreeHomeScreen(navController: NavController) {
    val context = LocalContext.current
    val vm: FreeHomeViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val rows by vm.rows
    val recentlyPlayed by vm.recentlyPlayed
    val trending by vm.trending
    val trendingLoading by vm.trendingLoading

    LaunchedEffect(Unit) { vm.maybeRefresh() }

    val play: (List<SongsModel>, Int) -> Unit = { list, index ->
        // Guard the index: callers pass it from list iteration, but if the list is swapped
        // for a shorter one before a tap's lambda runs, an unchecked list[index] would
        // crash. getOrNull turns that race into a harmless no op.
        list.getOrNull(index)?.let { song ->
            playerViewModel.updateQueue(list)
            playerViewModel.updateSongState(
                song.coverUri, song.title, song.singer, true, song.id, index, song.album,
            )
            SongPlayer.playSong(song.url, context, "song/${song.id}")
            navController.navigate(Routes.Player.route)
        }
    }

    val openSearch: (String) -> Unit = { query ->
        val route = if (query.isBlank()) {
            Routes.YtSearch.route
        } else {
            "${Routes.YtSearch.route}?q=${android.net.Uri.encode(query)}"
        }
        navController.navigate(route) {
            // Avoid piling identical destinations on the back stack.
            launchSingleTop = true
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(com.music.spotui.ui.theme.AppBackgroundBrush)
            .navBarScroll()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 190.dp),
    ) {
        item { HomeHeader() }

        item {
            MoodChips(onPick = openSearch)
            Spacer(Modifier.height(4.dp))
        }

        // Trending leads the screen, so the newest songs are the first thing seen.
        item {
            SectionHeader(title = "Trending now")
            when {
                trendingLoading && trending.isEmpty() -> TrendingSkeleton()
                trending.isNotEmpty() -> QuickPicks(tracks = trending, onPlay = play)
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item {
                SectionHeader(title = "Recently played")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    itemsIndexed(recentlyPlayed) { index, song ->
                        RecentTile(song = song, onClick = { play(recentlyPlayed, index) })
                    }
                }
            }
        }

        items(rows, key = { it.title }) { row ->
            HomeRowSection(row = row, onPlay = play)
        }

        item { Spacer(Modifier.height(20.dp)) }
    }
}

/** Premium greeting masthead: an amber accent bar, a gradient greeting, and a subtitle. */
@Composable
private fun HomeHeader() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 18.dp, top = 20.dp, bottom = 16.dp),
    ) {
        // A small amber accent bar above the greeting, a premium masthead touch that ties
        // the header to the app's colour.
        Box(
            modifier = Modifier
                .width(34.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Accent, com.music.spotui.ui.theme.AccentDark)
                    )
                ),
        )
        Spacer(Modifier.height(14.dp))
        // The greeting is drawn in a warm amber gradient with the heavier title cut, so it
        // reads as a premium masthead rather than plain white body text.
        Text(
            text = greeting(),
            fontFamily = com.music.spotui.ui.theme.SpotifyMixTitle,
            fontWeight = FontWeight.Bold,
            fontSize = 30.sp,
            letterSpacing = (-0.5).sp,
            style = androidx.compose.ui.text.TextStyle(
                brush = Brush.horizontalGradient(
                    listOf(Color(0xFFFFE0A3), Accent)
                ),
            ),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = greetingSubtitle(),
            color = TextDim,
            fontSize = 13.sp,
        )
    }
}

/**
 * Quick picks. A compact numbered list of playable rows, which gives the top of Home
 * a dense, premium feel rather than another row of identical cards.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun QuickPicks(tracks: List<SongsModel>, onPlay: (List<SongsModel>, Int) -> Unit) {
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            // A soft top down gradient inside the card plus a hairline edge gives the
            // quick picks block real depth instead of a flat panel.
            .background(
                Brush.verticalGradient(colors = listOf(SurfaceHigh, Surface)),
            )
            .border(1.dp, Hairline, RoundedCornerShape(18.dp))
            .padding(vertical = 4.dp),
    ) {
        tracks.forEachIndexed { index, song ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPlay(tracks, index) }
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                GlideImage(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(9.dp)),
                    model = song.coverUri,
                    contentScale = ContentScale.Crop,
                    failure = placeholder(R.drawable.placeholder),
                    loading = placeholder(R.drawable.placeholder),
                    contentDescription = null,
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp, end = 8.dp),
                ) {
                    Text(
                        text = song.title,
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                    )
                    if (song.singer.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.singer,
                            color = TextDim,
                            fontSize = 12.sp,
                            maxLines = 1,
                        )
                    }
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Accent.copy(alpha = 0.16f)),
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = null,
                        tint = Accent,
                        modifier = Modifier.size(19.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = Color.White,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(start = 16.dp, top = 22.dp, bottom = 13.dp),
    )
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
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        items(MOODS, key = { it.first }) { (label, query) ->
            Text(
                text = label,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(SurfaceHigh)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { onPick(query) }
                    .padding(horizontal = 16.dp, vertical = 9.dp),
            )
        }
    }
}

/** Compact tile used by the Recently played row. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun RecentTile(song: SongsModel, onClick: () -> Unit) {
    // Width scales with the screen instead of a fixed 232dp, so on small phones the tile
    // does not overflow and on tablets it does not look cramped. Bounded so it stays a
    // tidy card rather than stretching edge to edge.
    val screenWidth = androidx.compose.ui.platform.LocalConfiguration.current.screenWidthDp.dp
    val tileWidth = (screenWidth * 0.62f).coerceIn(200.dp, 280.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .width(tileWidth)
            .clip(RoundedCornerShape(8.dp))
            .background(Surface)
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
    ) {
        GlideImage(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            if (song.singer.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = song.singer,
                    color = TextDim,
                    fontSize = 11.sp,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun HomeRowSection(row: HomeRow, onPlay: (List<SongsModel>, Int) -> Unit) {
    // Skip a section that finished loading with nothing to show.
    if (!row.loading && row.tracks.isEmpty()) return

    SectionHeader(title = row.title)

    if (row.loading) {
        CardSkeletonRow()
        return
    }

    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        itemsIndexed(row.tracks) { index, song ->
            TrackCard(song = song, onClick = { onPlay(row.tracks, index) })
        }
    }
}

/** Premium artwork card with a gradient scrim and a play badge. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TrackCard(song: SongsModel, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .width(152.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(152.dp)
                .shadow(
                    elevation = 10.dp,
                    shape = RoundedCornerShape(12.dp),
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .clip(RoundedCornerShape(12.dp))
                .background(Surface),
        ) {
            GlideImage(
                modifier = Modifier.fillMaxSize(),
                model = song.coverUri,
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.placeholder),
                loading = placeholder(R.drawable.placeholder),
                contentDescription = null,
            )
            // Soft scrim so the play badge stays legible on bright artwork.
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.45f)),
                            startY = 150f,
                        )
                    ),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Accent),
            ) {
                Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = null,
                    // Dark glyph on the light amber badge for strong contrast.
                    tint = OnAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = song.title,
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            lineHeight = 17.sp,
        )
        if (song.singer.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.singer,
                color = TextDim,
                fontSize = 11.sp,
                maxLines = 1,
            )
        }
    }
}

/** Placeholder rows for the trending block while it loads. */
@Composable
private fun TrendingSkeleton() {
    val transition = rememberInfiniteTransition(label = "trendingSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "trendingSkeletonAlpha",
    )
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Surface),
    ) {
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SurfaceHigh.copy(alpha = alpha)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.6f)
                            .height(12.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHigh.copy(alpha = alpha)),
                    )
                    Spacer(Modifier.height(7.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.35f)
                            .height(10.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(SurfaceHigh.copy(alpha = alpha)),
                    )
                }
            }
        }
    }
}

/** Pulsing placeholders shown while a section is still loading. */
@Composable
private fun CardSkeletonRow() {
    val transition = rememberInfiniteTransition(label = "cardSkeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "cardSkeletonAlpha",
    )
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        repeat(3) {
            Column(modifier = Modifier.width(152.dp)) {
                Box(
                    modifier = Modifier
                        .size(152.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Surface.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(9.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.85f)
                        .height(12.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Surface.copy(alpha = alpha)),
                )
                Spacer(Modifier.height(7.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.55f)
                        .height(10.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(Surface.copy(alpha = alpha)),
                )
            }
        }
    }
}

/**
 * Time aware greeting. Locale agnostic and free of any java.time dependency so it
 * works on every supported API level.
 */
private fun greeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..4 -> "Late night listening"
        in 5..11 -> "Good morning"
        in 12..16 -> "Good afternoon"
        in 17..20 -> "Good evening"
        else -> "Good night"
    }
}

/** A short line under the greeting that sets the tone for the hour. */
private fun greetingSubtitle(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 0..4 -> "Something calm to see the night through"
        in 5..11 -> "Start your day with something you love"
        in 12..16 -> "Keep the afternoon going"
        in 17..20 -> "Unwind with your favourites"
        else -> "Wind down with a quiet mix"
    }
}
