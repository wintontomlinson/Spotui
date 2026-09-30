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
import com.music.spotui.ui.theme.Dusk
import com.music.spotui.ui.theme.GoldLight
import com.music.spotui.ui.theme.Ivory
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Velvet
import com.music.spotui.ui.components.shimmer
import androidx.compose.material.icons.rounded.Settings

// Accent comes from the single source of truth in the theme package.
private val Accent = com.music.spotui.ui.theme.Accent
private val OnAccent = com.music.spotui.ui.theme.OnAccent
private val Surface = Dusk
private val SurfaceHigh = Velvet
private val Hairline = Ivory.copy(alpha = 0.08f)
private val TextDim = TextSecondary

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
    val isRefreshing by vm.isRefreshing

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
            // Kick off autoplay-radio fill so a short list (e.g. a single tapped
            // trending track) grows into a full queue right away.
            playerViewModel.ensureRadioQueue()
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

    val pullState = androidx.compose.material3.pulltorefresh.rememberPullToRefreshState()
    androidx.compose.material3.pulltorefresh.PullToRefreshBox(
        isRefreshing = isRefreshing,
        onRefresh = { vm.pullRefresh() },
        state = pullState,
        modifier = Modifier
            .fillMaxSize()
            .background(com.music.spotui.ui.theme.AppBackgroundBrush),
        indicator = {
            // Themed refresh effect: a gold spinner on a deep-cyan pill so the
            // reload gesture matches the app's cyan+gold look.
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = com.music.spotui.ui.theme.Dusk,
                color = Accent,
            )
        },
    ) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .navBarScroll()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 190.dp),
    ) {
        item { HomeHeader(onOpenSettings = { navController.navigate(Routes.Settings.route) }) }

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
    } // PullToRefreshBox
}

/** Masthead: the Solo wordmark with a settings shortcut, then a time-aware greeting. */
@Composable
private fun HomeHeader(onOpenSettings: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 18.dp, end = 10.dp, top = 12.dp, bottom = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            com.music.spotui.ui.components.SoloWordmark(markHeight = 24.dp, textSize = 24.sp)
            Spacer(Modifier.weight(1f))
            com.music.spotui.ui.components.SoloIconButton(
                icon = androidx.compose.material.icons.Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                filled = true,
                iconSize = 20.dp,
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = greeting(),
            style = androidx.compose.material3.MaterialTheme.typography.displaySmall,
            color = Ivory,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = greetingSubtitle(),
            color = TextDim,
            style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
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
                // Chart-style rank number so the block reads as a real "top" list.
                Text(
                    text = "${index + 1}",
                    color = if (index < 3) Accent else TextDim,
                    fontFamily = com.music.spotui.ui.theme.SoloDisplay,
                    fontSize = if (index < 3) 20.sp else 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    modifier = Modifier.width(24.dp),
                )
                Spacer(Modifier.width(8.dp))
                GlideImage(
                    modifier = Modifier
                        .size(50.dp)
                        .clip(RoundedCornerShape(10.dp)),
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
                        color = Ivory,
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
    com.music.spotui.ui.components.SoloSectionHeader(title = title)
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
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(MOODS, key = { it.first }) { (label, query) ->
            com.music.spotui.ui.components.SoloChip(
                label = label,
                selected = false,
                onClick = { onPick(query) },
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
            .clip(RoundedCornerShape(12.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(end = 12.dp),
    ) {
        GlideImage(
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(topStart = 12.dp, bottomStart = 12.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = song.title,
                color = Ivory,
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
            .width(156.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(156.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(16.dp),
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .clip(RoundedCornerShape(16.dp))
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
            color = Ivory,
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
    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Surface)
            .padding(vertical = 4.dp),
    ) {
        repeat(4) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 9.dp),
            ) {
                Box(Modifier.size(50.dp).shimmer(RoundedCornerShape(10.dp)))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Box(Modifier.fillMaxWidth(0.6f).height(12.dp).shimmer())
                    Spacer(Modifier.height(7.dp))
                    Box(Modifier.fillMaxWidth(0.35f).height(10.dp).shimmer())
                }
            }
        }
    }
}

/** Placeholder cards shown while a section is still loading. */
@Composable
private fun CardSkeletonRow() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.padding(horizontal = 16.dp),
    ) {
        repeat(3) {
            Column(modifier = Modifier.width(156.dp)) {
                Box(Modifier.size(156.dp).shimmer(RoundedCornerShape(16.dp)))
                Spacer(Modifier.height(9.dp))
                Box(Modifier.fillMaxWidth(0.85f).height(12.dp).shimmer())
                Spacer(Modifier.height(7.dp))
                Box(Modifier.fillMaxWidth(0.55f).height(10.dp).shimmer())
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
