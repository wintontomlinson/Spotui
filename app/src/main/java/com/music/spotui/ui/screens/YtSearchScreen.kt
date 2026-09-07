package com.music.spotui.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
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
import com.music.spotui.ui.viewmodel.PlayerViewModel
import com.music.spotui.ui.viewmodel.SearchFilterType
import com.music.spotui.ui.viewmodel.YtSearchViewModel
import com.music.spotui.ui.viewmodel.formatDurationMs

private val Accent = Color(0xFFFF0033)
private val Surface = Color(0xFF17171C)
private val SurfaceHigh = Color(0xFF20202A)
private val TextDim = Color(0xFFB3B3B3)
private val TextFaint = Color(0xFF7A7A85)

/** Popular starting points offered when the search box is still empty. */
private val SUGGESTIONS = listOf(
    "Top hits", "New releases", "Bollywood", "Lofi", "Punjabi",
    "Workout", "Party", "Romantic", "Instrumental", "90s",
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
    val filter by vm.filter
    val recent by vm.recent

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
            .background(Color.Black)
            .statusBarsPadding(),
    ) {
        SearchField(
            query = query,
            focusRequester = searchFocus,
            onValueChange = { vm.onQueryChangeDebounced(it) },
            onSubmit = { submit(query) },
            onClear = { vm.clear() },
        )

        // Type filters appear once there is something to filter.
        if (query.isNotBlank()) {
            FilterChips(selected = filter, onSelect = { vm.setFilter(it) })
        }

        Box(modifier = Modifier.fillMaxSize()) {
            when {
                isLoading && results.isEmpty() -> ResultSkeleton()

                results.isNotEmpty() -> {
                    LazyColumn(contentPadding = PaddingValues(top = 4.dp, bottom = 180.dp)) {
                        itemsIndexed(results) { index, song ->
                            ResultRow(
                                song = song,
                                isVideo = filter == SearchFilterType.VIDEOS,
                                onClick = { playResult(song, index) },
                            )
                        }
                    }
                }

                hasSearched && !isLoading -> EmptyState(
                    title = "No results found",
                    message = error ?: "Try a different spelling or a shorter search.",
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
            .height(50.dp)
            .padding(start = 16.dp, end = 6.dp),
    ) {
        Icon(
            Icons.Default.Search,
            contentDescription = null,
            tint = TextDim,
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

@Composable
private fun FilterChips(selected: SearchFilterType, onSelect: (SearchFilterType) -> Unit) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(start = 14.dp, end = 14.dp, bottom = 6.dp),
    ) {
        SearchFilterType.entries.forEach { type ->
            val active = type == selected
            Text(
                text = type.label,
                color = if (active) Color.Black else Color.White,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier
                    .clip(RoundedCornerShape(50))
                    .background(if (active) Color.White else SurfaceHigh)
                    .clickable { onSelect(type) }
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }
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
                text = "Popular searches",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp, top = 18.dp, bottom = 12.dp),
            )
        }
        item {
            // Two rows of suggestion chips so they read as a tidy block.
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SUGGESTIONS.chunked(5).forEach { chunk ->
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(chunk, key = { it }) { label ->
                            Text(
                                text = label,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(SurfaceHigh)
                                    .clickable { onPick(label) }
                                    .padding(horizontal = 16.dp, vertical = 9.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun ResultRow(song: SongsModel, isVideo: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Videos read better as a 16:9 frame, songs as square art.
        GlideImage(
            modifier = Modifier
                .then(
                    if (isVideo) Modifier.width(76.dp).height(44.dp)
                    else Modifier.size(52.dp)
                )
                .clip(RoundedCornerShape(if (isVideo) 8.dp else 6.dp)),
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
            val meta = buildString {
                append(if (isVideo) "Video" else "Song")
                if (song.singer.isNotBlank()) append("  •  ").append(song.singer)
            }
            Text(
                text = meta,
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
private fun ResultSkeleton() {
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
                        .clip(RoundedCornerShape(6.dp))
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
