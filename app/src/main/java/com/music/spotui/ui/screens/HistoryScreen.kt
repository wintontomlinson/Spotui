package com.music.spotui.ui.screens

import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Person
import com.music.spotui.ui.components.AppSearchBar
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.HistoryEntry
import com.music.spotui.data.preferences.HistorySortOption
import com.music.spotui.data.preferences.clearListeningHistory
import com.music.spotui.data.preferences.getHistorySortOption
import com.music.spotui.data.preferences.getListeningHistory
import com.music.spotui.data.preferences.isHistorySortDescending
import com.music.spotui.data.preferences.removeListeningHistory
import com.music.spotui.data.preferences.setHistorySortOption
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.SongOptionsSheet
import com.music.spotui.ui.navigation.artistRoute
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.PlayerViewModel
import java.text.DateFormat
import java.util.Date
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.AccentSoft
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3

private val CardBg = Surface2
private val BarTrack = Surface3
private val MutedText = TextSecondary

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: NavController) {
    val context = LocalContext.current
    val playerViewModel: PlayerViewModel = hiltViewModel()
    var history by remember { mutableStateOf(getListeningHistory(context)) }
    var showClearDialog by remember { mutableStateOf(false) }

    var searchQuery by remember { mutableStateOf("") }
    var currentSort by remember { mutableStateOf(getHistorySortOption(context)) }
    var isDescending by remember { mutableStateOf(isHistorySortDescending(context)) }
    var showSortSheet by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<SongsModel?>(null) }
    menuSong?.let { sel ->
        SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = { menuSong = null },
        )
    }

    val filteredHistory = remember(history, searchQuery, currentSort, isDescending) {
        val filtered = if (searchQuery.isBlank()) {
            history
        } else {
            history.filter {
                it.title.contains(searchQuery, ignoreCase = true) ||
                        it.singer.contains(searchQuery, ignoreCase = true) ||
                        it.album.contains(searchQuery, ignoreCase = true)
            }
        }
        when (currentSort) {
            HistorySortOption.DATE -> if (isDescending) filtered else filtered.reversed()
            HistorySortOption.TITLE -> if (isDescending) filtered.sortedByDescending { it.title.lowercase() } else filtered.sortedBy { it.title.lowercase() }
            HistorySortOption.ARTIST -> if (isDescending) filtered.sortedByDescending { it.singer.lowercase() } else filtered.sortedBy { it.singer.lowercase() }
        }
    }

    fun playEntry(entry: HistoryEntry) {
        if (entry.url.isBlank()) return
        playerViewModel.updateQueue(listOf(
            SongsModel(
                id = entry.songId,
                title = entry.title,
                album = entry.album,
                singer = entry.singer,
                coverUri = entry.image,
                url = entry.url,
            ),
        ))
        playerViewModel.updateSongState(
            entry.image, entry.title, entry.singer, true, entry.songId, 0, entry.album,
        )
        SongPlayer.playSong(entry.url, context, "song/${entry.songId}")
    }

    val topArtists = remember(history) {
        history.groupingBy { it.singer.substringBefore(",").trim() }
            .eachCount().entries
            .filter { it.key.isNotBlank() }
            .sortedByDescending { it.value }
            .take(5)
    }
    val topTracks = remember(history) {
        history.groupingBy { "${it.title}, ${it.singer}" }
            .eachCount().entries
            .sortedByDescending { it.value }
            .take(5)
    }

    val maxArtistPlays = remember(topArtists) { topArtists.firstOrNull()?.value?.toFloat() ?: 1f }
    val maxTrackPlays = remember(topTracks) { topTracks.firstOrNull()?.value?.toFloat() ?: 1f }

    Surface(modifier = Modifier.fillMaxSize()) {
        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .background(com.music.spotui.ui.theme.Canvas)
                    .statusBarsPadding()
            ) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp, 16.dp, 16.dp, 8.dp),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            com.music.spotui.ui.components.SoloIconButton(
                                icon = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                onClick = { navController.navigateUp() },
                                filled = true,
                            )
                            Spacer(Modifier.width(12.dp))
                            Text("Listening history", color = TextPrimary, style = MaterialTheme.typography.headlineSmall)
                        }
                        if (history.isNotEmpty()) {
                            Text(
                                "Clear all",
                                color = MutedText,
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                ) { showClearDialog = true },
                            )
                        }
                    }
                }

                if (history.isEmpty()) {
                    item {
                        com.music.spotui.ui.components.SoloEmptyState(
                            icon = androidx.compose.material.icons.Icons.Rounded.History,
                            title = "No listening history yet",
                            message = "Songs you play show up here, with your top artists and stats.",
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                } else {
                    // ── Stats card ──
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                                .clip(SoloShape.md)
                                .background(
                                    Brush.linearGradient(
                                        colors = listOf(
                                            com.music.spotui.ui.theme.Elevated,
                                            Surface2,
                                        )
                                    )
                                )
                                .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.md)
                                .padding(16.dp),
                        ) {
                            Column {
                                Text("YOUR LISTENING HABITS", color = Accent, style = MaterialTheme.typography.labelSmall)
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceAround,
                                ) {
                                    StatPill("Total plays", "${history.size}", Accent)
                                    StatPill("Unique songs", "${history.distinctBy { it.songId }.size}", TextPrimary)
                                    StatPill("Artists", "${history.distinctBy { it.singer.substringBefore(",") }.size}", TextPrimary)
                                }
                            }
                        }
                    }

                    // ── Top artists ──
                    if (topArtists.isNotEmpty()) {
                        item {
                            Text(
                                "Top artists",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                            )
                        }
                        items(topArtists.size) { i ->
                            val entry = topArtists[i]
                            val artistImage = remember(entry.key, history) {
                                history.lastOrNull {
                                    it.singer.substringBefore(",").trim() == entry.key
                                }?.image ?: ""
                            }
                            TopArtistRow(
                                rank = i + 1,
                                name = entry.key,
                                plays = entry.value,
                                progress = entry.value.toFloat() / maxArtistPlays,
                                imageUrl = artistImage,
                            ) {
                                navController.navigate(artistRoute(entry.key))
                            }
                        }
                    }

                    // ── Top tracks ──
                    if (topTracks.isNotEmpty()) {
                        item {
                            Text(
                                "Top tracks",
                                color = TextPrimary,
                                style = MaterialTheme.typography.titleLarge,
                                modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                            )
                        }
                        items(topTracks.size) { i ->
                            val entry = topTracks[i]
                            val parts = entry.key.split(", ", limit = 2)
                            val trackTitle = parts.getOrElse(0) { entry.key }
                            val trackArtist = parts.getOrElse(1) { "" }
                            val trackImage = remember(entry.key, history) {
                                history.lastOrNull {
                                    "${it.title}, ${it.singer}" == entry.key
                                }?.image ?: ""
                            }
                            TopTrackRow(
                                rank = i + 1,
                                title = trackTitle,
                                artist = trackArtist,
                                plays = entry.value,
                                progress = entry.value.toFloat() / maxTrackPlays,
                                imageUrl = trackImage,
                            ) {
                                val latest = history.lastOrNull {
                                    "${it.title}, ${it.singer}" == entry.key
                                }
                                if (latest != null) playEntry(latest)
                            }
                        }
                    }

                    // ── History list ──
                    item {
                        Text(
                            "History",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(16.dp, 16.dp, 16.dp, 4.dp),
                        )
                    }

                    // Search Bar
                    item {
                        AppSearchBar(
                            query = searchQuery,
                            onQueryChange = { searchQuery = it },
                            modifier = Modifier.padding(16.dp, 8.dp),
                            placeholder = "Search in history",
                        )
                    }

                    // Sort action button
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 0.dp, 16.dp, 8.dp),
                            horizontalArrangement = Arrangement.Start
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(SoloShape.pill)
                                    .background(Surface3)
                                    .clickable { showSortSheet = true }
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = currentSort.getDescriptiveLabel(isDescending),
                                    color = TextPrimary,
                                    style = MaterialTheme.typography.labelLarge)
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = "Sort Options",
                                    tint = TextPrimary,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .padding(start = 4.dp)
                                )
                            }
                        }
                    }

                    if (filteredHistory.isEmpty() && searchQuery.isNotBlank()) {
                        item {
                            com.music.spotui.ui.components.SoloEmptyState(
                                icon = androidx.compose.material.icons.Icons.Rounded.SearchOff,
                                title = "No matches",
                                message = "Nothing here matches \"$searchQuery\".",
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    } else {
                        items(filteredHistory.size) { i ->
                            val entry = filteredHistory[i]
                            HistoryRow(
                                entry = entry,
                                onRemove = {
                                    removeListeningHistory(context, entry)
                                    history = getListeningHistory(context)
                                },
                                onClick = { playEntry(entry) },
                                onLongClick = {
                                    menuSong = SongsModel(
                                        id = entry.songId,
                                        title = entry.title,
                                        album = entry.album,
                                        singer = entry.singer,
                                        coverUri = entry.image,
                                        url = entry.url,
                                    )
                                },
                            )
                        }
                    }
                }
                item { Spacer(Modifier.height(140.dp)) }
            }

            com.music.spotui.ui.components.FastScrollbarForLazyList(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )

            if (showSortSheet) {
                ModalBottomSheet(
                    onDismissRequest = { showSortSheet = false },
                    containerColor = Surface2,
                    shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
                    dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
                    scrimColor = com.music.spotui.ui.theme.Scrim,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                    ) {
                        Text(
                            text = "Sort by",
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 12.dp)
                        )
                        HorizontalDivider(color = Surface3)
                        Spacer(modifier = Modifier.height(4.dp))
                        HistorySortOption.entries.forEach { option ->
                            val isSelected = option == currentSort
                            val icon = when (option) {
                                HistorySortOption.DATE -> Icons.Default.DateRange
                                HistorySortOption.TITLE -> Icons.AutoMirrored.Filled.List
                                HistorySortOption.ARTIST -> Icons.Default.Person
                            }

                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (currentSort == option) {
                                            isDescending = !isDescending
                                        } else {
                                            currentSort = option
                                            isDescending = (option == HistorySortOption.DATE)
                                        }
                                        setHistorySortOption(context, currentSort, isDescending)
                                        showSortSheet = false
                                    }
                                    .padding(16.dp, 14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = icon,
                                    contentDescription = null,
                                    tint = if (isSelected) com.music.spotui.ui.theme.Accent else TextPrimary,
                                    modifier = Modifier.size(22.dp)
                                )
                                Spacer(modifier = Modifier.width(18.dp))
                                Text(
                                    text = if (isSelected) option.getDescriptiveLabel(isDescending) else option.getDescriptiveLabel(option == HistorySortOption.DATE),
                                    color = if (isSelected) com.music.spotui.ui.theme.Accent else TextPrimary,
                                    style = MaterialTheme.typography.bodyLarge,
                                    modifier = Modifier.weight(1f)
                                )
                                if (isSelected) {
                                    Icon(
                                        imageVector = if (isDescending) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowUp,
                                        contentDescription = null,
                                        tint = com.music.spotui.ui.theme.Accent,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }

            if (showClearDialog) {
                AlertDialog(
                    onDismissRequest = { showClearDialog = false },
                    title = { Text("Clear listening history?", color = TextPrimary, style = MaterialTheme.typography.titleLarge) },
                    text = { Text("This will remove all ${history.size} plays. This action cannot be undone.", color = MutedText) },
                    confirmButton = {
                        com.music.spotui.ui.components.SoloDialogConfirm(
                            text = "Clear",
                            danger = true,
                            onClick = {
                            clearListeningHistory(context)
                            history = emptyList()
                            showClearDialog = false
                        },
                        )
                    },
                    dismissButton = {
                        com.music.spotui.ui.components.SoloDialogDismiss(
                            text = "Cancel",
                            onClick = { showClearDialog = false },
                        )
                    },
                    containerColor = Surface2,
                    titleContentColor = TextPrimary,
                    textContentColor = MutedText,
                )
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String, accent: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = accent, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, color = MutedText, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TopArtistRow(
    rank: Int,
    name: String,
    plays: Int,
    progress: Float,
    imageUrl: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(SoloShape.sm)
            .background(CardBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(12.dp),
    ) {
        Text(
            text = "$rank",
            color = MutedText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
        )
        GlideImage(
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape),
            model = imageUrl.ifBlank { null },
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = "",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                name,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(SoloShape.xs),
                color = Accent,
                trackColor = BarTrack,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$plays plays",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun TopTrackRow(
    rank: Int,
    title: String,
    artist: String,
    plays: Int,
    progress: Float,
    imageUrl: String,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(SoloShape.sm)
            .background(CardBg)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
            ) { onClick() }
            .padding(12.dp),
    ) {
        Text(
            text = "$rank",
            color = MutedText,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.width(22.dp),
        )
        GlideImage(
            modifier = Modifier
                .size(48.dp)
                .clip(SoloShape.xs),
            model = imageUrl.ifBlank { null },
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = "",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
        ) {
            Text(
                title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (artist.isNotBlank()) {
                Text(
                    artist,
                    color = MutedText,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(Modifier.height(6.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(SoloShape.xs),
                color = Accent,
                trackColor = BarTrack,
                strokeCap = StrokeCap.Round,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "$plays plays",
                color = MutedText,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
private fun HistoryRow(
    entry: HistoryEntry,
    onRemove: () -> Unit,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 6.dp)
            .clip(SoloShape.sm)
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            ),
    ) {
        GlideImage(
            modifier = Modifier
                .size(44.dp)
                .clip(SoloShape.xs),
            model = entry.image,
            contentScale = ContentScale.Crop,
            failure = placeholder(R.drawable.placeholder),
            loading = placeholder(R.drawable.placeholder),
            contentDescription = "",
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = 10.dp, end = 8.dp),
        ) {
            Text(entry.title, color = TextPrimary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                "${entry.singer} • ${DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT).format(Date(entry.ts))}",
                color = TextTertiary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Remove",
            tint = MutedText,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable { onRemove() }
                .padding(15.dp),
        )
    }
}
