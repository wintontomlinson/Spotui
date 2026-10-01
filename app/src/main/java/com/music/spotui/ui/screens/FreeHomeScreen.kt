package com.music.spotui.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
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
import androidx.compose.foundation.layout.fillMaxHeight
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
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.AccentSoft
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.components.shimmer
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.CloudOff
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.Hairline

private val Surface = Surface2
private val SurfaceHigh = Surface3
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
    val mix by vm.mix
    val topArtists by vm.topArtists

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
            // Themed refresh effect: an accent spinner on a surface pill so the
            // reload gesture matches the app's Aurora Noir look.
            androidx.compose.material3.pulltorefresh.PullToRefreshDefaults.Indicator(
                state = pullState,
                isRefreshing = isRefreshing,
                modifier = Modifier.align(Alignment.TopCenter),
                containerColor = com.music.spotui.ui.theme.Surface2,
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

        // Daily "Mix for you" hero, only once there is listening history to build it from.
        if (mix.isNotEmpty()) {
            item(key = "mix") {
                MixForYouCard(mix = mix, onPlay = { play(mix, 0) })
            }
        }

        if (recentlyPlayed.isNotEmpty()) {
            item(key = "jump") {
                SectionHeader(title = "Jump back in")
                JumpBackInGrid(tracks = recentlyPlayed.take(6), onPlay = { i -> play(recentlyPlayed, i) })
            }
        }

        // Trending leads the screen, so the newest songs are the first thing seen.
        item {
            SectionHeader(title = "Trending now")
            when {
                trendingLoading && trending.isEmpty() -> TrendingSkeleton()
                trending.isNotEmpty() -> QuickPicks(tracks = trending, onPlay = play)
                !trendingLoading -> com.music.spotui.ui.components.SoloEmptyState(
                    icon = androidx.compose.material.icons.Icons.Rounded.CloudOff,
                    title = "Couldn't load Home",
                    message = "Check your connection and try again.",
                    actionLabel = "Retry",
                    onAction = { vm.pullRefresh() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }

        if (topArtists.isNotEmpty()) {
            item(key = "artists") {
                SectionHeader(title = "Your top artists")
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    itemsIndexed(topArtists, key = { _, it -> it.first }) { index, (name, image) ->
                        ArtistCircle(
                            name = name,
                            image = image,
                            highlighted = index == 0,
                            onClick = { navController.navigate(com.music.spotui.ui.navigation.artistRoute(name, "")) },
                        )
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
            .padding(start = 20.dp, end = 12.dp, top = 12.dp, bottom = 14.dp),
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
        Spacer(Modifier.height(18.dp))
        Text(
            text = greeting(),
            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
            color = TextSecondary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = greetingSubtitle(),
            color = com.music.spotui.ui.theme.TextTertiary,
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
            .padding(horizontal = 20.dp)
            .clip(SoloShape.lg)
            // A soft top down gradient inside the card plus a hairline edge gives the
            // quick picks block real depth instead of a flat panel.
            .background(
                Brush.verticalGradient(colors = listOf(SurfaceHigh, Surface)),
            )
            .border(1.dp, Hairline, SoloShape.lg)
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
                        .clip(SoloShape.sm),
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
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                    )
                    if (song.singer.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = song.singer,
                            color = TextDim,
                            style = MaterialTheme.typography.bodySmall,
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
        contentPadding = PaddingValues(horizontal = 20.dp),
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

/**
 * "Mix for you" hero: a full-width 184dp card. The 2x2 artwork collage fills the
 * trailing side, a Elevated-to-transparent scrim carries the title, and an accent
 * "Play mix" pill starts it.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun MixForYouCard(mix: List<SongsModel>, onPlay: () -> Unit) {
    Box(
        modifier = Modifier
            .padding(start = 20.dp, end = 20.dp, top = 18.dp)
            .fillMaxWidth()
            .height(184.dp)
            .shadow(
                elevation = 14.dp,
                shape = SoloShape.lg,
                clip = false,
                ambientColor = com.music.spotui.ui.theme.Shadow,
                spotColor = com.music.spotui.ui.theme.Shadow,
            )
            .clip(SoloShape.lg)
            .background(com.music.spotui.ui.theme.Elevated)
            .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.lg),
    ) {
        val covers = mix.map { it.coverUri }.filter { it.isNotBlank() }.distinct().take(4)
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(184.dp),
        ) {
            for (r in 0 until 2) {
                Row(Modifier.weight(1f)) {
                    for (c in 0 until 2) {
                        GlideImage(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxSize(),
                            model = covers.getOrNull(r * 2 + c) ?: covers.firstOrNull(),
                            contentScale = ContentScale.Crop,
                            failure = placeholder(R.drawable.placeholder),
                            loading = placeholder(R.drawable.placeholder),
                            contentDescription = null,
                        )
                    }
                }
            }
        }
        // Elevated -> transparent scrim so the copy reads over the collage.
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to com.music.spotui.ui.theme.Elevated,
                        0.48f to com.music.spotui.ui.theme.Elevated.copy(alpha = 0.92f),
                        1f to Color.Transparent,
                    )
                ),
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(0.62f)
                .padding(start = 18.dp, end = 8.dp),
        ) {
            Text(
                text = "DAILY",
                color = Accent,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Mix for you",
                style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                color = TextPrimary,
                maxLines = 1,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Made from what you love · ${mix.size} songs",
                color = TextSecondary,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 2,
            )
            Spacer(Modifier.height(14.dp))
            com.music.spotui.ui.components.SoloPillButton(
                text = "Play mix",
                icon = Icons.Default.PlayArrow,
                onClick = onPlay,
            )
        }
    }
}

/** "Jump back in": the last distinct tracks as a compact two-column grid. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun JumpBackInGrid(tracks: List<SongsModel>, onPlay: (Int) -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        tracks.chunked(2).forEachIndexed { rowIdx, pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                pair.forEachIndexed { colIdx, song ->
                    val index = rowIdx * 2 + colIdx
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clip(SoloShape.sm)
                            .background(com.music.spotui.ui.theme.Surface1)
                            .border(1.dp, Hairline, SoloShape.sm)
                            .clickable(onClickLabel = "Play ${song.title}") { onPlay(index) },
                    ) {
                        GlideImage(
                            modifier = Modifier.size(56.dp),
                            model = song.coverUri,
                            contentScale = ContentScale.Crop,
                            failure = placeholder(R.drawable.placeholder),
                            loading = placeholder(R.drawable.placeholder),
                            contentDescription = null,
                        )
                        Text(
                            text = song.title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 2,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 10.dp),
                        )
                    }
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

/** Circular artist portrait with the name under it. */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ArtistCircle(name: String, image: String, onClick: () -> Unit, highlighted: Boolean = false) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(92.dp)
            .clickable(onClickLabel = "Open $name", onClick = onClick),
    ) {
        Box(
            modifier = Modifier
                .size(84.dp)
                .clip(CircleShape)
                // The top artist gets an accent ring; the rest a quiet hairline.
                .then(
                    if (highlighted) Modifier.background(com.music.spotui.ui.theme.AccentBrush)
                    else Modifier.background(Hairline)
                )
                .padding(if (highlighted) 2.5.dp else 1.dp)
                .clip(CircleShape)
                .background(Surface),
        ) {
            GlideImage(
                modifier = Modifier.fillMaxSize(),
                model = image.ifBlank { null },
                contentScale = ContentScale.Crop,
                failure = placeholder(R.drawable.placeholder),
                loading = placeholder(R.drawable.placeholder),
                contentDescription = null,
            )
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = name,
            color = if (highlighted) TextPrimary else TextSecondary,
            style = MaterialTheme.typography.labelMedium,
            maxLines = 1,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
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
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
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
            .width(150.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            ),
    ) {
        Box(
            modifier = Modifier
                .size(150.dp)
                .shadow(
                    elevation = 12.dp,
                    shape = SoloShape.md,
                    clip = false,
                    ambientColor = Color.Black,
                    spotColor = Color.Black,
                )
                .clip(SoloShape.md)
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
                    // Dark glyph on the Volt badge for strong contrast.
                    tint = OnAccent,
                    modifier = Modifier.size(20.dp),
                )
            }
        }
        Spacer(Modifier.height(9.dp))
        Text(
            text = song.title,
            color = TextPrimary,
            style = MaterialTheme.typography.titleSmall,
            maxLines = 2,
            lineHeight = 17.sp,
        )
        if (song.singer.isNotBlank()) {
            Spacer(Modifier.height(3.dp))
            Text(
                text = song.singer,
                color = TextDim,
                style = MaterialTheme.typography.bodySmall,
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
            .padding(horizontal = 20.dp)
            .clip(SoloShape.lg)
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
                Box(Modifier.size(50.dp).shimmer(SoloShape.sm))
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
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.padding(horizontal = 20.dp),
    ) {
        repeat(3) {
            Column(modifier = Modifier.width(150.dp)) {
                Box(Modifier.size(150.dp).shimmer(SoloShape.md))
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
