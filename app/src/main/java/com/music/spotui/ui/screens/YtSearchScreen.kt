package com.music.spotui.ui.screens

/**
 * Explore / Search tab (`YtSearchScreen`).
 *
 * Shows the browse categories as decorative mesh tiles (see `BROWSE_CATEGORIES`) and hosts the
 * search field + results. Takes an optional [initialQuery] to open straight into a search.
 */

import androidx.compose.animation.core.RepeatMode
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
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
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.components.shimmer
import com.music.spotui.ui.components.SoloArtwork
import com.music.spotui.ui.components.soloMeshBackground
import androidx.compose.material.icons.rounded.SearchOff
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.Hairline
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged

private val Surface = Surface2
private val SurfaceHigh = Surface3
private val TextDim = TextSecondary
private val TextFaint = TextTertiary

/**
 * Colourful browse tiles shown on the empty search screen.
 *  - [query] is the music search run both when the tile is tapped AND to resolve the tile's
 *    own cover art, so the picture always reflects what the tile plays.
 */
private data class BrowseCategory(
    val label: String,
    val query: String,
    val color: Color,
)

private val BROWSE_CATEGORIES = listOf(
    // Each category carries a deep tint (it colours the tile's scrim) drawn from the
    // Graphite and Azure family (refined azure, deep cobalt, cool steel and graphite slate)
    // so every tile sits cohesively on the graphite canvas while staying distinct from its
    // neighbours. `query` is both what runs on tap and what resolves the tile's cover art, so
    // each tile shows a relatable, current album or playlist cover rather than a generic result.
    BrowseCategory("Trending", "trending songs 2026 official video", Color(0xFF1E4FA8)),
    BrowseCategory("Top Charts", "global top 50 hits 2026", Color(0xFF2A3F6E)),
    BrowseCategory("New Releases", "new music friday 2026", Color(0xFF245A6E)),
    BrowseCategory("Made For You", "feel good hits mix", Color(0xFF2E4C8A)),
    BrowseCategory("Bollywood", "latest bollywood songs 2026", Color(0xFF34496A)),
    BrowseCategory("Punjabi", "new punjabi songs 2026", Color(0xFF3F5566)),
    BrowseCategory("Hip-Hop", "best rap hip hop 2026", Color(0xFF2A3550)),
    BrowseCategory("Pop", "top pop songs 2026", Color(0xFF295F8A)),
    BrowseCategory("Chill & Lo-Fi", "lofi beats to relax study", Color(0xFF2A5560)),
    BrowseCategory("Workout", "gym workout motivation music", Color(0xFF1F4A7A)),
    BrowseCategory("Romance", "romantic love songs 2026", Color(0xFF3A5276)),
    BrowseCategory("Party", "party club dance anthems 2026", Color(0xFF1D4ED8).copy(alpha = 0.9f)),
    BrowseCategory("Devotional", "bhajan devotional songs", Color(0xFF415266)),
    BrowseCategory("90s & Retro", "90s superhit old songs", Color(0xFF32425E)),
    BrowseCategory("Sad", "sad emotional songs 2026", Color(0xFF2E4664)),
    BrowseCategory("English", "top english pop songs 2026", Color(0xFF2A5A6E)),
    BrowseCategory("Instrumental", "instrumental focus music", Color(0xFF3A4F5A)),
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
        // Fill out the queue with related tracks so autoplay/Queue never stalls.
        playerViewModel.ensureRadioQueue()
        navController.navigate(Routes.Player.route)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(com.music.spotui.ui.theme.AppBackgroundBrush)
            .navBarScroll()
            .statusBarsPadding(),
    ) {
        // Explore masthead in Sora.
        Text(
            text = "Explore",
            style = MaterialTheme.typography.headlineLarge,
            color = TextPrimary,
            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 14.dp),
        )
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
                    item(key = "resultsHeader") {
                        Text(
                            text = "TOP ${tab.label.uppercase()}",
                            color = TextFaint,
                            style = MaterialTheme.typography.labelSmall,
                            letterSpacing = 1.4.sp,
                            modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 4.dp),
                        )
                    }
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

/** Songs / Artists / Albums / Playlists selector, styled as Volt-selected pills. */
@Composable
private fun ResultTabs(selected: SearchTab, onSelect: (SearchTab) -> Unit) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(bottom = 6.dp),
    ) {
        items(SearchTab.entries.toList(), key = { it.name }) { entry ->
            com.music.spotui.ui.components.SoloChip(
                label = entry.label,
                selected = entry == selected,
                onClick = { onSelect(entry) },
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                // Artists read as people, so their image is a circle.
                .clip(CircleShape),
            model = artist.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp)) {
            Text(
                text = artist.name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            Text(text = "Artist", color = TextDim, style = MaterialTheme.typography.bodySmall)
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                .clip(SoloShape.sm),
            model = album.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = album.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
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
                style = MaterialTheme.typography.bodySmall,
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
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
                .clip(SoloShape.sm),
            model = playlist.thumbnail,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = null,
        )
        Column(modifier = Modifier.padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = playlist.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
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
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
    }
}

/** The 52dp Surface3 search field: accent edge and icon while focused, inline clear action. */
@Composable
private fun SearchField(
    query: String,
    focusRequester: FocusRequester,
    onValueChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onClear: () -> Unit,
) {
    // Visual only: drives the accent edge and icon.
    var focused by remember { androidx.compose.runtime.mutableStateOf(false) }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .clip(SoloShape.md)
            .background(SurfaceHigh)
            .border(1.dp, if (focused) Accent else Hairline, SoloShape.md)
            .height(52.dp)
            .padding(start = 14.dp, end = 4.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = if (focused) Accent else TextDim,
            modifier = Modifier.size(22.dp),
        )
        BasicTextField(
            value = query,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            cursorBrush = SolidColor(Accent),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .onFocusChanged { focused = it.isFocused }
                .focusRequester(focusRequester),
            decorationBox = { inner ->
                if (query.isEmpty()) {
                    Text(
                        "Search songs, artists, albums",
                        color = TextFaint,
                        style = MaterialTheme.typography.bodyLarge,
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
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onClear)
                    .padding(11.dp),
            )
        }
    }
}


/**
 * Explore card: an md-radius tile whose FULL background is artwork matched to the
 * category, under a scrim that keeps the category tone at the top and settles into the
 * canvas at the bottom, with an accent play chip top-right and the label bottom-left.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun BrowseTile(
    category: BrowseCategory,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    height: androidx.compose.ui.unit.Dp = 168.dp,
) {
    // The tile's own music cover, resolved from the app's YouTube source. Seeded synchronously
    // from the cache (instant when scrolled back into view), then refreshed off the main thread
    // by coverFor below. While it is blank (loading, offline, blocked or no match) the premium
    // mesh underneath shows through, so a tile is never an empty box.
    var cover by remember(category.query) {
        mutableStateOf(com.music.spotui.data.api.BrowseTileImages.cachedFor(category.query))
    }
    LaunchedEffect(category.query) {
        val resolved = com.music.spotui.data.api.BrowseTileImages.coverFor(category.query)
        if (resolved.isNotBlank()) cover = resolved
    }
    Box(
        modifier = modifier
            .height(height)
            // A deterministic Graphite and Azure mesh painted from the label: it shows while the
            // cover loads and stays as a premium, legible fallback if the url is blank or the
            // image fails to decode, so the tile always paints something.
            .soloMeshBackground(category.label, SoloShape.md)
            .border(1.dp, Hairline, SoloShape.md)
            .clickable(onClickLabel = "Explore ${category.label}", onClick = onClick),
    ) {
        // Real music cover over the mesh. Only drawn when a url exists, so a blank/failed cover
        // leaves the premium mesh visible rather than an empty Glide box.
        if (cover.isNotBlank()) {
            SoloArtwork(
                model = cover,
                modifier = Modifier.matchParentSize(),
                shape = SoloShape.md,
                meshFallback = true,
                contentScale = ContentScale.Crop,
                contentDescription = category.label,
            )
        }
        // A soft category-tone + Canvas scrim over the art/mesh so the label always reads.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0f to category.color.copy(alpha = 0.35f),
                        0.45f to com.music.spotui.ui.theme.Canvas.copy(alpha = 0.18f),
                        1f to com.music.spotui.ui.theme.Canvas.copy(alpha = 0.82f),
                    ),
                ),
        )
        // A small accent play chip in the top-right, a premium Explore flourish.
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(10.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(com.music.spotui.ui.theme.AccentBrush),
        ) {
            Icon(
                Icons.Default.PlayArrow,
                contentDescription = null,
                tint = com.music.spotui.ui.theme.OnAccent,
                modifier = Modifier.size(16.dp),
            )
        }
        // Category name sitting ON TOP of the image, bottom-left.
        Text(
            text = category.label,
            color = TextPrimary,
            style = if (height > 168.dp) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.titleLarge,
            maxLines = 2,
            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
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
    // Extra bottom room so the last Explore row clears the mini-player + nav bar
    // and its label is fully readable.
    LazyColumn(contentPadding = PaddingValues(bottom = 240.dp)) {
        if (recent.isNotEmpty()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 8.dp, top = 10.dp, bottom = 2.dp),
                ) {
                    Text(
                        text = "Recent searches",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f),
                    )
                    Text(
                        text = "Clear all",
                        color = Accent,
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier
                            .clip(SoloShape.pill)
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
                        .padding(start = 20.dp, end = 8.dp, top = 4.dp, bottom = 4.dp),
                ) {
                    Icon(
                        Icons.Default.History,
                        contentDescription = null,
                        tint = TextFaint,
                        modifier = Modifier.size(20.dp),
                    )
                    Text(
                        text = entry,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
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
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { onRemoveRecent(entry) }
                            .padding(14.dp),
                    )
                }
            }
        }

        item {
            // Browse heading (the Explore masthead sits above the search field).
            Column(modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 14.dp)) {
                Text(
                    text = "Browse all",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineSmall,
                    color = TextPrimary,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Moods, genres and what's new",
                    style = androidx.compose.material3.MaterialTheme.typography.bodyMedium,
                    color = TextDim,
                )
            }
        }
        item {
            // A uniform two-column grid of image-forward category cards; every tap
            // runs the seeded search behind that category.
            Column(
                modifier = Modifier.padding(horizontal = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                // The first category leads as a full-width hero tile.
                BROWSE_CATEGORIES.firstOrNull()?.let { hero ->
                    BrowseTile(
                        category = hero,
                        onClick = { onPick(hero.query) },
                        modifier = Modifier.fillMaxWidth(),
                        height = 196.dp,
                    )
                }
                BROWSE_CATEGORIES.drop(1).chunked(2).forEach { pair ->
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
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ResultRow(song: SongsModel, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlideImage(
            modifier = Modifier
                .size(52.dp)
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
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = song.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
            )
            Spacer(Modifier.height(3.dp))
            // Lead with the artist when it is known, since that is what people scan
            // for, and fall back to a plain type label when it is not.
            Text(
                text = song.singer.ifBlank { "Song" },
                color = TextDim,
                style = MaterialTheme.typography.bodySmall,
                maxLines = 1,
            )
        }
        val duration = formatDurationMs(song.durationMs)
        if (duration.isNotEmpty()) {
            Text(
                text = duration,
                color = TextFaint,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
        }
    }
}

/** Placeholder rows that shimmer while the first page of results loads. */
@Composable
private fun ResultSkeleton(circular: Boolean = false) {
    Column(modifier = Modifier.padding(top = 6.dp)) {
        repeat(8) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                // Match the shape of the rows being waited for, so the list does not
                // visibly change form when the results land.
                Box(Modifier.size(52.dp).shimmer(if (circular) CircleShape else SoloShape.sm))
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 12.dp),
                ) {
                    Box(Modifier.fillMaxWidth(0.62f).height(13.dp).shimmer())
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.fillMaxWidth(0.38f).height(11.dp).shimmer())
                }
            }
        }
    }
}

@Composable
private fun EmptyState(title: String, message: String) {
    com.music.spotui.ui.components.SoloEmptyState(
        icon = Icons.Rounded.SearchOff,
        title = title,
        message = message,
        modifier = Modifier.fillMaxSize(),
    )
}
