package com.music.spotui.ui.screens

import androidx.compose.material.icons.rounded.QueueMusic
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import com.music.spotui.ui.components.SongOptionsSheet
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
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
import androidx.compose.material.icons.rounded.DragHandle
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.PlayerViewModel

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary

/**
 * The "play queue" as an overlay sheet over the full-screen player (matching Spotify).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    navController: NavController,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2,
        contentColor = TextPrimary,
        dragHandle = null,
        shape = com.music.spotui.ui.theme.SonvraShape.sheetTop,
        modifier = Modifier.fillMaxHeight(0.92f),
        scrimColor = com.music.spotui.ui.theme.Scrim,
    ) {
        QueueContent(
            navController = navController,
            onClose = onDismiss
        )
    }
}

/**
 * The "play queue" screen (standalone route fallback).
 */
@Composable
fun QueueScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface2)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        QueueContent(
            navController = navController,
            onClose = { navController.navigateUp() }
        )
    }
}

/**
 * The core queue UI, the track that's playing now plus everything coming up.
 * Tapping an upcoming track jumps straight to it; dragging the handle reorders it;
 * swiping a row removes it.
 */
@OptIn(ExperimentalGlideComposeApi::class, ExperimentalMaterial3Api::class)
@Composable
fun QueueContent(
    navController: NavController,
    onClose: () -> Unit,
) {
    val playerViewModel: PlayerViewModel = hiltViewModel()
    val context = LocalContext.current
    val queue by playerViewModel.queue
    val currentId = playerViewModel.currentSongId.value
    val curIdx = queue.indexOfFirst { it.id == currentId }.coerceAtLeast(0)
    val upcoming = if (queue.isNotEmpty()) queue.drop(curIdx + 1) else emptyList()
    val current = queue.getOrNull(curIdx)

    // Drag-to-reorder state: which upcoming row is dragged and its live offset.
    var draggingId by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableFloatStateOf(0f) }
    val rowHeightPx = with(LocalDensity.current) { 64.dp.toPx() }

    var menuSong by remember { mutableStateOf<com.music.spotui.data.entity.SongsModel?>(null) }
    menuSong?.let { sel ->
        SongOptionsSheet(
            song = sel,
            navController = navController,
            context = context,
            onDismiss = { menuSong = null },
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Surface2)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp, 12.dp)
        ) {
            com.music.spotui.ui.components.SonvraIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Close queue",
                onClick = onClose,
                iconSize = 28.dp,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Queue", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.headlineMedium)
        }

        val listState = androidx.compose.foundation.lazy.rememberLazyListState()
        Box(modifier = Modifier.fillMaxSize()) {
            if (queue.isEmpty()) {
                com.music.spotui.ui.components.SonvraEmptyState(
                    icon = androidx.compose.material.icons.Icons.Rounded.QueueMusic,
                    title = "Your queue is empty",
                    message = "Play a song and what comes next shows up here.",
                    modifier = Modifier.align(Alignment.Center),
                )
            }
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxSize()
                    .navigationBarsPadding()
            ) {
                current?.let {
                    item {
                        Text(
                            "NOW PLAYING",
                            color = com.music.spotui.ui.theme.Accent,
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 12.dp, 16.dp, 6.dp)
                        )
                        QueueRow(
                            song = it,
                            highlight = true,
                            onClick = {},
                            onLongClick = { menuSong = it },
                        )
                    }
                }
                if (upcoming.isNotEmpty()) {
                    item {
                        Text(
                            "NEXT UP",
                            color = com.music.spotui.ui.theme.Accent,
                            fontSize = 11.sp,
                            letterSpacing = 1.4.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(16.dp, 18.dp, 16.dp, 6.dp)
                        )
                    }
                    itemsIndexed(upcoming, key = { _, s -> s.id }) { upIdx, song ->
                        val isDragging = draggingId == song.id
                        val dismissState = rememberSwipeToDismissBoxState(
                            confirmValueChange = { value ->
                                if (value != SwipeToDismissBoxValue.Settled) {
                                    playerViewModel.removeFromQueue(song)
                                    true
                                } else false
                            }
                        )
                        SwipeToDismissBox(
                            state = dismissState,
                            backgroundContent = {
                                Box(
                                    contentAlignment = Alignment.CenterEnd,
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(com.music.spotui.ui.theme.DangerSurface)
                                        .padding(horizontal = 24.dp),
                                ) {
                                    Icon(Icons.Default.Delete, contentDescription = "Remove", tint = TextPrimary)
                                }
                            },
                            modifier = Modifier.graphicsLayer {
                                translationY = if (isDragging) dragOffset else 0f
                            },
                        ) {
                            QueueRow(
                                song = song,
                                highlight = false,
                                onLongClick = { menuSong = song },
                                onClick = {
                                    val idx = queue.indexOfFirst { it.id == song.id }
                                    playerViewModel.updateSongState(
                                        song.coverUri, song.title, song.singer, true,
                                        song.id, idx.coerceAtLeast(0), song.album
                                    )
                                    SongPlayer.playSong(song.url, context, "song/${song.id}")
                                },
                                dragHandle = Modifier.pointerInput(song.id) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggingId = song.id
                                            dragOffset = 0f
                                        },
                                        onDragEnd = { draggingId = -1; dragOffset = 0f },
                                        onDragCancel = { draggingId = -1; dragOffset = 0f },
                                    ) { change, dragAmount ->
                                        change.consume()
                                        dragOffset += dragAmount.y
                                        // Crossed a full row height → swap with the neighbour.
                                        val abs = queue.indexOfFirst { it.id == song.id }
                                        if (dragOffset > rowHeightPx / 2 && upIdx < upcoming.size - 1) {
                                            playerViewModel.moveQueueItem(abs, abs + 1)
                                            dragOffset -= rowHeightPx
                                        } else if (dragOffset < -rowHeightPx / 2 && upIdx > 0) {
                                            playerViewModel.moveQueueItem(abs, abs - 1)
                                            dragOffset += rowHeightPx
                                        }
                                    }
                                },
                            )
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(40.dp)) }
            }
            com.music.spotui.ui.components.FastScrollbarForLazyList(
                state = listState,
                modifier = Modifier.align(Alignment.CenterEnd)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalGlideComposeApi::class)
@Composable
private fun QueueRow(
    song: com.music.spotui.data.entity.SongsModel,
    highlight: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit = {},
    dragHandle: Modifier? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(if (highlight) com.music.spotui.ui.theme.Surface4 else Surface2)
            .combinedClickable(
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .padding(16.dp, 8.dp)
    ) {
        GlideImage(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(10.dp)),
            model = song.coverUri,
            contentScale = ContentScale.Crop,
            loading = placeholder(R.drawable.placeholder),
            failure = placeholder(R.drawable.placeholder),
            contentDescription = ""
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (song.explicit) {
                    com.music.spotui.ui.components.ExplicitBadge()
                    Spacer(Modifier.width(4.dp))
                }
                Text(
                    text = song.title,
                    color = if (highlight) com.music.spotui.ui.theme.Accent else TextPrimary,
                    style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Text(
                text = song.singer,
                color = TextSecondary,
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (!highlight && dragHandle != null) {
            Icon(
                imageVector = androidx.compose.material.icons.Icons.Rounded.DragHandle,
                contentDescription = "Reorder",
                tint = TextTertiary,
                modifier = Modifier
                    .size(48.dp)
                    .then(dragHandle)
                    .padding(12.dp)
            )
        }
    }
}
