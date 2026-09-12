package com.music.spotui.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
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
import com.music.spotui.ui.navigation.navBarScroll
import com.music.spotui.ui.viewmodel.AlbumResult
import com.music.spotui.ui.viewmodel.ArtistResult
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.viewmodel.PlaylistResult
import com.music.spotui.ui.viewmodel.SearchTab
import com.music.spotui.ui.viewmodel.YtSearchViewModel
import com.music.spotui.ui.viewmodel.formatDurationMs

// Accent comes from the single source of truth in the theme package.
private val Accent = com.music.spotui.ui.theme.Accent
private val Surface = Color(0xFF0B3A44)
private val SurfaceHigh = Color(0xFF0F4A57)
private val Hairline = Color(0x14FFFFFF)
private val TextDim = Color(0xFFB3B3B3)
private val TextFaint = Color(0xFF7A7A85)

/** Colourful browse tiles shown on the empty search screen, each seeds a search. */
private data class BrowseCategory(val label: String, val query: String, val color: Color)

private val BROWSE_CATEGORIES = listOf(
    // Cyan + Gold Edition — every tile is tinted from the teal/cyan family with a
    // couple of warm gold accents so Explore reads as one cohesive premium
    // palette. Trending & Charts lead the grid.
    BrowseCategory("Trending", "trending songs this week", Color(0xFF0E7C8C)),
    BrowseCategory("Charts", "top charts this week", Color(0xFFC79A2B)),
    BrowseCategory("New releases", "new songs this month", Color(0xFF12909F)),
    BrowseCategory("Bollywood", "bollywood hits", Color(0xFF1AA6B0)),
    BrowseCategory("Punjabi", "punjabi hits", Color(0xFF0B6A78)),
    BrowseCategory("Hip-Hop", "hip hop hits", Color(0xFF0A5866)),
    BrowseCategory("Chill & Lo-Fi", "lofi chill beats", Color(0xFF167E88)),
    BrowseCategory("Workout", "workout songs", Color(0xFF0E7C8C)),
    BrowseCategory("Romance", "romantic songs", Color(0xFF1E9FA8)),
    BrowseCategory("Party", "party songs", Color(0xFF15A0B4)),
    BrowseCategory("Devotional", "devotional songs", Color(0xFFC79A2B)),
    BrowseCategory("90s & Retro", "90s hit songs", Color(0xFF0B6A78)),
    BrowseCategory("Sad songs", "sad songs", Color(0xFF0A5866)),
)

/**
 * Login free search and play. Type a song name, get YouTube Music results, tap to
 * play. No Spotify session is required. Results are mapped to [SongsModel] and go
 * through the exact same queue and player as the rest of the app.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun YtSearchScreen(navController: NavController, initialQuery: String = "") {
    val context = LocalContext.current
    val vm: YtSearchViewModel = hiltViewModel()
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val keyboard = LocalSoftwareKeyboardController.current

    val searchFocus = remember { FocusRequester() }

    // Pre fill and run a search when opened from a Home mood chip, otherwise put
    // the cursor in the search box so typing starts immediately.
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
    val recent by vm.recent
    val tab by vm.tab
    val artistResults by vm.artists
    val albumResults by vm.albums
    val playlistResults by vm.playlists

    val submit: (String) -> Unit = { q ->
        vm.onQueryChange(q)
        vm.search(q)
        keyboard?.hide()
    }

    val playResult: (SongsModel, Int) -> Unit = { song, index ->
        // Queue the whole result list so next and previous work through the results.
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
            .background(com.music.spotui.ui.theme.AppBackgroundBrush)
            .navBarScroll()
            .statusBarsPadding(),
    ) {
        SearchField(
            query = query,
            focusRequester = searchFocus,
            onValueChange = { vm.onQueryChangeDebounced(it) },
            onSubmit = { submit(query) },
            onClear = { vm.clear() },
        )

        // The result-kind filter only makes sense once there is a search to filter.
        if (hasSearched || query.isNotBlank()) {
            ResultTabs(selected = tab, onSelect = { vm.selectTab(it) })
        }

        val currentCount = when (tab) {
            SearchTab.SONGS -> results.size
            SearchTab.ARTISTS -> artistResults.size
            SearchTab.ALBUMS -> albumResults.size
            SearchTab.PLAYLISTS -> playlistResults.size
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && currentCount == 0 -> ResultSkeleton(circular = tab == SearchTab.ARTISTS)

                currentCount > 0 -> LazyColumn(
                    contentPadding = PaddingValues(top = 2.dp, bottom = 180.dp),
                ) {
                    when (tab) {
                        SearchTab.SONGS -> itemsIndexed(results) { index, song ->
                            ResultRow(song = song, onClick = { playResult(song, index) })
                        }
                        SearchTab.ARTISTS -> items(artistResults, key = { it.id }) { artist ->
                            ArtistResultRow(artist) {
                                // The artist page resolves by name on YouTube, which is what
                                // works without a login. The channel id is deliberately not
                                // passed, since that argument means a Spotify id downstream.
                                navController.navigate(
                                    com.music.spotui.ui.navigation.artistRoute(artist.name)
                                )
                            }
                        }
                        SearchTab.ALBUMS -> items(albumResults, key = { it.browseId }) { album ->
                            AlbumResultRow(album) {
                                // The exact album id travels with the link, so the album
                                // screen opens this album rather than guessing from a name
                                // that dozens of unrelated albums also use.
                                navController.navigate(
                                    com.music.spotui.ui.navigation.albumRoute(
                                        album.title,
                                        album.artist,
                                        album.browseId,
                                    )
                                )
                            }
                        }
                        SearchTab.PLAYLISTS -> items(playlistResults, key = { it.id }) { playlist ->
                            PlaylistResultRow(playlist) {
                                navController.navigate(
                                    com.music.spotui.ui.navigation.playlistRoute(playlist.id, playlist.title)
                                )
                            }
                        }
                    }
                }

                hasSearched && !isLoading -> EmptyState(
                    title = "No ${tab.label.lowercase()} found",
                    message = error ?: "Try a different spelling or another filter.",
                )

                else -> DiscoverPane(
                    recent = recent,
                    onPick = submit,
                    onRemoveRecent = { vm.removeRecent(it) },
                    onClearRecent = { vm.clearRecent() },
                )
            }
        }
    }
}

/** Songs / Artists / Albums / Playlists selector, styled as amber pills. */
@Composable
private fun ResultTabs(selected: SearchTab, onSelect: (SearchTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        items(SearchTab.entries.toList(), key = { it.name }) { entry ->
            val active = entry == selected
            Text(
                text = entry.label,
                color = if (active) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Accent else SurfaceHigh)
                    .border(
                        1.dp,
                        if (active) Color.Transparent else Hairline,
                        RoundedCornerShape(50),
                    )
                    .clickable { onSelect(entry) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ArtistResultRow(artist: ArtistResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                // Artists read as people, so their image is a circle.
                .clip(RoundedCornerShape(50)),
            model = artist.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = artist.name,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(text = "Artist", color = TextDim, fontSize = 12.sp)
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun AlbumResultRow(album: AlbumResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
            model = album.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = album.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOfNotNull(
                    "Album",
                    album.artist.takeIf { it.isNotBlank() },
                    album.year?.toString(),
                ).joinToString(" • "),
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun PlaylistResultRow(playlist: PlaylistResult, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
            model = playlist.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = playlist.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = listOfNotNull(
                    "Playlist",
                    playlist.author.takeIf { it.isNotBlank() },
                    playlist.songCount.takeIf { it.isNotBlank() },
                ).joinToString(" • "),
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
    }
}

/** The rounded pill search field with a leading icon and inline clear action. */
@Composable
private fun SearchField(
    query: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .clip(RoundedCornerShape(26.dp))
            .background(Surface)
            .border(1.dp, Hairline, RoundedCornerShape(26.dp))
            .height(52.dp)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = Accent,
            modifier = Modifier.size(22.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 16.sp),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Search songs, artists, albums",
                        color = TextFaint,
                        fontSize = 16.sp,
                    )
                }
                inner()
            },
        )
        if (query.isNotEmpty()) {
            Icon(
                Icons.Default.Close,
                contentDescription = "Clear search",
                tint = TextDim,
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(50))
                    .clickable(onClick = onClear)
                    .padding(9.dp),
            )
        }
    }
}

/**
 * One "Browse all" tile: a coloured card with the label top-left and a real cover
 * tilted into the bottom-right corner, the way Spotify presents its browse categories.
 * The cover is fetched on first display and cached for the session, so scrolling back
 * costs nothing.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BrowseTile(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val coverState = androidx.compose.runtime.remember(category.query) {
        androidx.compose.runtime.mutableStateOf(
            com.music.spotui.data.api.BrowseTileImages.cachedFor(category.query)
        )
    }
    val cover = coverState.value
    androidx.compose.runtime.LaunchedEffect(category.query) {
        if (coverState.value.isBlank()) {
            coverState.value = com.music.spotui.data.api.BrowseTileImages.coverFor(category.query)
        }
    }

    Box(
        modifier = modifier
            .height(150.dp)
            .clip(RoundedCornerShape(18.dp))
            // Soft diagonal wash of the tile's cyan hue so it looks intentional
            // even before (or if) the artwork loads.
            .background(
                Brush.linearGradient(
                    listOf(category.color, category.color.copy(alpha = 0.7f)),
                )
            )
            .border(1.dp, Color(0x33E8C24A), RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
    ) {
        // Cyan + Gold Edition: the artwork fills the whole tile as a background,
        // with a cinematic diagonal scrim over it (crossfaded in with a
        // placeholder) instead of a small tilted corner thumbnail.
        if (cover.isNotBlank()) {
            GlideImage(
                model = cover,
                contentScale = ContentScale.Crop,
                contentDescription = null,
                modifier = Modifier.matchParentSize(),
                loading = placeholder(R.drawable.placeholder),
                failure = placeholder(R.drawable.placeholder),
            )
        }
        // Diagonal scrim: the tile's own hue at the top-left, transparent in the
        // middle, fading into the deep cyan canvas at the bottom-right so the
        // label stays legible over any artwork and the palette stays cohesive.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.linearGradient(
                        colors = listOf(
                            category.color.copy(alpha = 0.35f),
                            Color(0x00041418),
                            Color(0xE6041418),
                        ),
                        start = androidx.compose.ui.geometry.Offset(0f, 0f),
                        end = androidx.compose.ui.geometry.Offset.Infinite,
                    )
                ),
        )
        // Thin gold accent bar above the label — a small premium flourish.
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 14.dp, bottom = 44.dp)
                .height(3.dp)
                .width(26.dp)
                .clip(RoundedCornerShape(50))
                .background(Accent),
        )
        Text(
            text = category.label,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.ExtraBold,
            maxLines = 2,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(14.dp),
        )
    }
}

/** Recent searches plus popular suggestions, shown before any search is run. */
@Composable
private fun DiscoverPane(
    recent: List<String>,
    onPick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecent: () -> Unit,
) {
    LazyColumn(contentPadding = PaddingValues(bottom = 180.dp)) {
        if (recent.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 2.dp),
                ) {
                    Text(
                        text = "Recent searches",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Clear all",
                        color = Accent,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .clickable(onClick = onClearRecent)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                    )
                }
            }
            items(recent, key = { it }) { entry ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPick(entry) }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextFaint,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = entry,
                        color = Color.White,
                        fontSize = 15.sp,
                        maxLines = 1,
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 14.dp, end = 8.dp),
                    )
                    Icon(
                        Icons.Default.Close,
                        contentDescription = "Remove",
                        tint = TextFaint,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(50))
                            .clickable { onRemoveRecent(entry) }
                            .padding(7.dp),
                    )
                }
            }
        }

        item {
            Text(
                text = "Explore",
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.ExtraBold,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 12.dp),
            )
        }
        item {
            // Two column grid of colourful browse tiles. Each is a rounded card in its own
            // hue with the label bottom-left, the familiar premium browse layout, and each
            // tap runs the seeded search.
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                BROWSE_CATEGORIES.chunked(2).forEach { pair ->
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        pair.forEach { cat ->
                            BrowseTile(category = cat, onClick = { onPick(cat.query) },
                                modifier = Modifier.weight(1f))
                        }
                        // Keep the last row aligned when the list is odd.
                        if (pair.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
        // The "Popular searches" chip rows are gone. They were the same idea as the browse
        // tiles above, a canned search behind a label, only without artwork, so the screen
        // was asking twice for the same tap. The tiles do the job better.
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ResultRow(song: SongsModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                .clip(RoundedCornerShape(6.dp)),
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
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            // Lead with the artist when it is known, since that is what people scan
            // for, and fall back to a plain type label when it is not.
            Text(
                text = song.singer.ifBlank { "Song" },
                color = TextDim,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }
        val duration = formatDurationMs(song.durationMs)
        if (duration.isNotEmpty()) {
            Text(
                text = duration,
                color = TextFaint,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Placeholder rows that pulse while the first page of results loads. */
@Composable
private fun ResultSkeleton(circular: Boolean = false) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.75f,
        animationSpec = infiniteRepeatable(
            animation = tween(750),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "skeletonAlpha",
    )
    Column(modifier = Modifier.padding(top = 6.dp)) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        // Match the shape of the rows being waited for, so the list does
                        // not visibly change form when the results land.
                        .clip(RoundedCornerShape(if (circular) 50.dp else 6.dp))
                        .background(Surface.copy(alpha = alpha)),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.62f)
                            .height(13.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Surface.copy(alpha = alpha)),
                    )
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.38f)
                            .height(11.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Surface.copy(alpha = alpha)),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            Icons.Default.MusicNote,
            contentDescription = null,
            tint = Color(0xFF3A3A44),
            modifier = Modifier.size(56.dp),
        )
        Spacer(Modifier.height(14.dp))
        Text(
            text = title,
            color = Color.White,
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = message,
            color = TextFaint,
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
        )
    }
}
