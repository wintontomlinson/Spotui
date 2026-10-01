package com.music.spotui.ui.components

import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.Search
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.border
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.bumptech.glide.integration.compose.placeholder
import com.music.spotui.R
import com.music.spotui.data.preferences.addLikedSongId
import com.music.spotui.data.preferences.isSongLiked
import com.music.spotui.data.preferences.removeLikedSongId
import com.music.spotui.di.Palette
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.viewmodel.PlayerViewModel
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState
import kotlinx.coroutines.delay
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.launch
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.Surface1
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.foundation.layout.statusBarsPadding
import com.music.spotui.ui.theme.Shadow
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import com.music.spotui.ui.theme.SoloMotion
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.ui.graphics.StrokeCap

/**
 * Shared loading state for album / artist / playlist / liked / show screens: a header
 * skeleton (artwork + title lines) and track-row skeletons, instead of a bare spinner,
 * so the layout is already in place when content arrives.
 */
@Composable
fun Loader() {
    Column(
        Modifier
            .fillMaxWidth()
            .background(com.music.spotui.ui.theme.Canvas)
            .statusBarsPadding()
            .padding(top = 56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.size(200.dp).shimmer(SoloShape.lg))
        Spacer(Modifier.height(18.dp))
        Box(Modifier.fillMaxWidth(0.55f).height(18.dp).shimmer())
        Spacer(Modifier.height(10.dp))
        Box(Modifier.fillMaxWidth(0.35f).height(12.dp).shimmer())
        Spacer(Modifier.height(18.dp))
        SoloShimmerList(count = 6)
    }
}

@Composable
fun ExplicitBadge(
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.TextUnit = 9.sp,
) {
    Text(
        text = "E",
        color = TextSecondary,
        fontSize = size,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(Surface3, SoloShape.xs)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    )
}

@OptIn(ExperimentalGlideComposeApi::class, ExperimentalFoundationApi::class)
@Composable
fun MiniPlayer(navController: NavHostController) {
    val miniPlayerViewModel : PlayerViewModel = hiltViewModel()
    val songTitle by miniPlayerViewModel.currentSongTitle
    val songSinger by miniPlayerViewModel.currentSongSinger
    val songCoverUri by miniPlayerViewModel.currentSongCoverUri
    val songPlayingState by miniPlayerViewModel.currentSongPlayingState
    val songId by miniPlayerViewModel.currentSongId
    val songIndex by miniPlayerViewModel.currentSongIndex
    val songAlbum by miniPlayerViewModel.currentSongAlbum
    val isCurrentSongExplicit by remember(songId) {
        mutableStateOf(miniPlayerViewModel.queue.value.firstOrNull { it.id == songId }?.explicit == true)
    }

    // Keep MiniPlayer playing state in sync with physical SongPlayer audio engine
    LaunchedEffect(Unit) {
        miniPlayerViewModel.syncWithPlayer()
    }

    var swipeOffsetY by remember { mutableFloatStateOf(0f) }
    var swipeOffsetX by remember { mutableFloatStateOf(0f) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(songId) {
        swipeOffsetX = 0f
    }

    var songProgress by remember { mutableFloatStateOf(0f) }

    songProgress = if (SongPlayer.getDuration() > 0) {
        SongPlayer.getCurrentPosition().toFloat() / SongPlayer.getDuration().toFloat()
    } else {
        0f
    }

    // Progress polling only. The end of a track is handled by the playback service
    // (autoplay / radio / crossfade), so the mini player never pauses or navigates here.
    LaunchedEffect(key1 = songPlayingState) {
        while (songPlayingState) {
            songProgress = SongPlayer.getDuration().toFloat().let { dur ->
                if (dur > 0f) (SongPlayer.getCurrentPosition().toFloat() / dur).coerceIn(0f, 1f) else 0f
            }
            delay(300L)
        }
    }

    val context = LocalContext.current

    var darkVibrantColor by remember { mutableStateOf(Surface1) }
    LaunchedEffect(songCoverUri) {
        if (songCoverUri.isNotBlank()) {
            Palette().extractFirstColorFromImageUrl(context = context, songCoverUri) { color ->
                darkVibrantColor = color
            }
        }
    }

    val miniTone by androidx.compose.animation.animateColorAsState(
        targetValue = darkVibrantColor,
        animationSpec = com.music.spotui.ui.theme.SoloMotion.emphasized(),
        label = "miniTone",
    )

    var isLiked by remember {
        mutableStateOf(false)
    }
    val likeState = miniPlayerViewModel.likeState.value
    LaunchedEffect(likeState, songId) {
        isLiked = isSongLiked(context, songId.toString())
    }
    val currentTrack = miniPlayerViewModel.queue.value.firstOrNull { it.id == songId }
    var showSavedIn by remember { mutableStateOf(false) }
    if (showSavedIn && currentTrack != null) {
        SavedInSheet(
            song = currentTrack,
            context = context,
            onDismiss = { showSavedIn = false },
            onLikedChanged = {
                isLiked = it
                miniPlayerViewModel.updateLikeState(!likeState)
            },
        )
    }


    Column(
        modifier = Modifier

            .padding(horizontal = 8.dp)
            .graphicsLayer {
                translationY = swipeOffsetY
                alpha = (1f + swipeOffsetY / 150f).coerceIn(0f, 1f)
            }
            // Surface2 card docked above the nav, with a faint artwork tint on the leading edge.
            .shadow(
                elevation = 8.dp,
                shape = SoloShape.md,
                clip = false,
                ambientColor = Shadow,
                spotColor = Shadow,
            )
            .clip(SoloShape.md)
            .background(
                Brush.horizontalGradient(
                    colors = listOf(
                        androidx.compose.ui.graphics.lerp(miniTone, Surface2, 0.72f),
                        Surface2,
                    )
                )
            )
            .border(1.dp, Hairline, SoloShape.md)

    ) {
        // 2dp accent progress along the top edge (tap or drag to seek).
        CustomSlider(
            value = songProgress,
            onValueChange = { newValue ->
                val dur = SongPlayer.getDuration()
                if (dur > 0) SongPlayer.seekTo((newValue * dur).toLong())
            },
            valueRange = 0f..1f,
            steps = 0,
            modifier = Modifier.fillMaxWidth()
        )
        Row(horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 8.dp, end = 4.dp, bottom = 6.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    navController.navigate(Routes.Player.route)
                }
                .pointerInput(Unit) {
                    var navigated = false
                    var lastEventTimeMs = 0L
                    val distThresholdY = 20.dp.toPx()
                    val velThresholdY = 1500f // px/s upward
                    val distThresholdX = 40.dp.toPx()
                    val velThresholdX = 1000f // px/s horizontal
                    val touchSlop = viewConfiguration.touchSlop

                    var totalDx = 0f
                    var totalDy = 0f
                    var currentDirection = 0 // 0: Undetermined, 1: Horizontal, 2: Vertical
                    var lastVelocityX = 0f

                    detectDragGestures(
                        onDragStart = {
                            navigated = false
                            lastEventTimeMs = 0L
                            totalDx = 0f
                            totalDy = 0f
                            currentDirection = 0
                            lastVelocityX = 0f
                        },
                        onDragEnd = {
                            if (currentDirection == 2) {
                                if (!navigated) {
                                    // Snap back with spring animation
                                    coroutineScope.launch {
                                        val anim = Animatable(swipeOffsetY)
                                        anim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 400f
                                            )
                                        ) { swipeOffsetY = value }
                                    }
                                } else {
                                    // Continue upward + fade out while full player slides in
                                    coroutineScope.launch {
                                        val anim = Animatable(swipeOffsetY)
                                        anim.animateTo(
                                            targetValue = -300f,
                                            animationSpec = tween(280)
                                        ) { swipeOffsetY = value }
                                        swipeOffsetY = 0f
                                    }
                                }
                            } else if (currentDirection == 1) {
                                val queue = miniPlayerViewModel.queue.value
                                if (swipeOffsetX > distThresholdX || lastVelocityX > velThresholdX) {
                                    // Swiped right (dragged from left to right): Previous track
                                    coroutineScope.launch {
                                        val anim = Animatable(swipeOffsetX)
                                        anim.animateTo(
                                            targetValue = 300f,
                                            animationSpec = tween(150)
                                        )
                                        miniPlayerViewModel.playPreviousSong(queue, context)
                                        swipeOffsetX = 0f
                                    }
                                } else if (swipeOffsetX < -distThresholdX || lastVelocityX < -velThresholdX) {
                                    // Swiped left (dragged from right to left): Next track
                                    coroutineScope.launch {
                                        val anim = Animatable(swipeOffsetX)
                                        anim.animateTo(
                                            targetValue = -300f,
                                            animationSpec = tween(150)
                                        )
                                        miniPlayerViewModel.playNextSongs(queue, context)
                                        swipeOffsetX = 0f
                                    }
                                } else {
                                    // Snap back with spring animation
                                    coroutineScope.launch {
                                        val anim = Animatable(swipeOffsetX)
                                        anim.animateTo(
                                            targetValue = 0f,
                                            animationSpec = spring(
                                                dampingRatio = 0.7f,
                                                stiffness = 400f
                                            )
                                        ) { swipeOffsetX = value }
                                    }
                                }
                            }
                            currentDirection = 0
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                val animY = Animatable(swipeOffsetY)
                                animY.animateTo(0f, spring(0.7f, 400f)) { swipeOffsetY = value }
                            }
                            coroutineScope.launch {
                                val animX = Animatable(swipeOffsetX)
                                animX.animateTo(0f, spring(0.7f, 400f)) { swipeOffsetX = value }
                            }
                            currentDirection = 0
                        },
                    ) { change, dragAmount ->
                        change.consume()
                        val now = change.uptimeMillis
                        val dtMs = if (lastEventTimeMs > 0L) now - lastEventTimeMs else 0L
                        lastEventTimeMs = now

                        if (currentDirection == 0) {
                            totalDx += dragAmount.x
                            totalDy += dragAmount.y
                            if (kotlin.math.abs(totalDx) > touchSlop || kotlin.math.abs(totalDy) > touchSlop) {
                                if (kotlin.math.abs(totalDx) > kotlin.math.abs(totalDy)) {
                                    currentDirection = 1 // HORIZONTAL
                                } else {
                                    currentDirection = 2 // VERTICAL
                                }
                            }
                        }

                        if (currentDirection == 2) {
                            swipeOffsetY = (swipeOffsetY + dragAmount.y).coerceAtMost(0f)
                            if (!navigated) {
                                val velocityPxPerSec = if (dtMs > 5) dragAmount.y / dtMs * 1000f else 0f
                                if (swipeOffsetY < -distThresholdY || velocityPxPerSec < -velThresholdY) {
                                    navigated = true
                                    navController.navigate(Routes.Player.route)
                                }
                            }
                        } else if (currentDirection == 1) {
                            swipeOffsetX += dragAmount.x
                            if (dtMs > 5) {
                                lastVelocityX = dragAmount.x / dtMs * 1000f
                            }
                        }
                    }
                }
        ){

            Row(horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .clipToBounds()
                    .graphicsLayer {
                        translationX = swipeOffsetX
                    }
            ) {
                GlideImage(
                    modifier = Modifier
                        .padding(end = 12.dp)
                        .size(44.dp)
                        .clip(SoloShape.sm),
                    model = songCoverUri,
                    contentScale = ContentScale.Crop,
                    failure = placeholder(R.drawable.placeholder),
                    loading = placeholder(R.drawable.placeholder),
                    contentDescription = ""
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isCurrentSongExplicit) {
                            ExplicitBadge()
                            Spacer(Modifier.width(4.dp))
                        }
                        // Pure-Compose now-playing equalizer glyph: dances while audio is
                        // playing and freezes when paused. Driven by playback state only —
                        // no Visualizer / audio-session tap, so playback and permissions
                        // stay untouched.
                        EqualizerGlyph(
                            playing = songPlayingState,
                            modifier = Modifier
                                .padding(end = 6.dp)
                                .size(width = 14.dp, height = 12.dp),
                        )
                        Text(text = songTitle, color = TextPrimary, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                    }
                    Spacer(Modifier.height(1.dp))
                    Text(text = songSinger, color = TextSecondary, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {

                // Plus = save; green check = already saved. A second tap opens the
                // "Saved in" sheet (Liked Songs + playlists) instead of unliking.
                // The glyph scale-pops and its tint animates to the accent on the
                // transition so the like registers tactilely.
                val likeTint by androidx.compose.animation.animateColorAsState(
                    targetValue = if (isLiked) Accent else TextPrimary,
                    animationSpec = SoloMotion.standard(),
                    label = "likeTint",
                )
                val likeScale by animateFloatAsState(
                    targetValue = if (isLiked) SoloMotion.TOGGLE_POP_SCALE else 1f,
                    animationSpec = SoloMotion.toggleSpring(),
                    label = "likeScale",
                )
                if (isLiked) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        tint = likeTint,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .clickable { showSavedIn = true }
                            .padding(12.dp)
                            .graphicsLayer { scaleX = likeScale; scaleY = likeScale },
                        contentDescription = "Saved",
                    )
                } else {
                    Icon(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .combinedClickable(
                                onClick = {
                                    addLikedSongId(context, songId.toString())
                                    isLiked = true
                                    miniPlayerViewModel.updateLikeState(!likeState)
                                    // Mirror the like to the real Spotify account.
                                    com.music.spotui.data.api.SpotifySync.setTrackSaved(
                                        context, currentTrack?.spotifyTrackId.orEmpty(), true)
                                },
                                onLongClick = { showSavedIn = true },
                            )
                            .padding(12.dp)
                            .graphicsLayer { scaleX = likeScale; scaleY = likeScale },
                        imageVector = Icons.Rounded.AddCircleOutline,
                        tint = likeTint,
                        contentDescription = "Save to Liked Songs",
                    )
                }


                // Only spin while LOCATING the stream (resolving); during plain
                // buffering keep the tappable play/pause icon so the button always
                // works and shows the right state.
                val playSource = remember { MutableInteractionSource() }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .soloPress(playSource)
                        .padding(4.dp)
                        .clip(CircleShape)
                        .background(com.music.spotui.ui.theme.AccentBrush)
                        .clickable(
                            interactionSource = playSource,
                            indication = null
                        ) {
                            // Single source of truth: toggle from the engine's
                            // real state so the mini-player icon never sticks.
                            miniPlayerViewModel.togglePlayPause()
                        },
                    contentAlignment = Alignment.Center,
                ) {
                    if (miniPlayerViewModel.isResolving.value) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = OnAccent,
                            strokeWidth = 2.5.dp
                        )
                    } else {
                        androidx.compose.animation.Crossfade(
                            targetState = songPlayingState,
                            animationSpec = tween(SoloMotion.ICON_MS),
                            label = "miniPlayPause",
                        ) { playing ->
                            Icon(
                                imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (playing) "Pause" else "Play",
                                tint = OnAccent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }


        }
    }



}


/**
 * A small now-playing equalizer glyph: three accent bars whose heights breathe up and down
 * on an infinite transition while [playing] is true, and freeze at a steady level when
 * paused. This is a pure Compose animation driven only by the playback state — it does NOT
 * read the audio session (no android.media.audiofx.Visualizer), so it needs no permission
 * and never touches playback.
 */
@Composable
fun EqualizerGlyph(
    playing: Boolean,
    modifier: Modifier = Modifier,
    color: Color = Accent,
    bars: Int = 3,
) {
    val transition = rememberInfiniteTransition(label = "equalizer")
    // Each bar animates on its own period/phase so they never move in lockstep.
    val periods = remember(bars) { List(bars) { 520 + it * 170 } }
    val levels = periods.mapIndexed { index, period ->
        transition.animateFloat(
            initialValue = 0.35f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = period, easing = SoloMotion.Standard),
                repeatMode = RepeatMode.Reverse,
            ),
            label = "equalizerBar$index",
        )
    }
    // When paused, hold a calm static silhouette instead of animating.
    val staticLevels = remember(bars) { List(bars) { 0.45f + (it % 2) * 0.3f } }

    Canvas(modifier = modifier) {
        val gap = size.width * 0.22f / (bars - 1).coerceAtLeast(1)
        val barWidth = (size.width - gap * (bars - 1)) / bars
        for (i in 0 until bars) {
            val fraction = if (playing) levels[i].value else staticLevels[i]
            val barHeight = size.height * fraction
            val left = i * (barWidth + gap)
            drawLine(
                color = color,
                start = Offset(left + barWidth / 2f, size.height),
                end = Offset(left + barWidth / 2f, size.height - barHeight),
                strokeWidth = barWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    colors: Any? = null, // kept for API compat; unused
) {
    val fraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start))
        .coerceIn(0f, 1f)
    val density = LocalDensity.current

    Box(
        modifier = modifier
            .height(10.dp) // compact touch target; the line sits on its top edge
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val newFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val mapped = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mapped)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val newFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                    val mapped = valueRange.start + newFraction * (valueRange.endInclusive - valueRange.start)
                    onValueChange(mapped)
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(2.dp)) {
            val trackHeightPx = with(density) { 2.dp.toPx() }
            val trackY = size.height / 2f
            val thumbX = fraction * size.width

            // Inactive track
            drawLine(
                color = Surface4,
                start = Offset(0f, trackY),
                end = Offset(size.width, trackY),
                strokeWidth = trackHeightPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round
            )
            // Active track in the accent
            if (thumbX > 0f) drawLine(
                color = Accent,
                start = Offset(0f, trackY),
                end = Offset(thumbX, trackY),
                strokeWidth = trackHeightPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Butt
            )
        }
    }
}


@Composable
fun Snackbar(showMessage : String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .clip(SoloShape.md)
            .background(Surface3)
            .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.md),
        contentAlignment = Alignment.Center
    ){
        Text(
            style = MaterialTheme.typography.labelLarge,
            color = TextPrimary,
            text = showMessage
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SwipeToPlayNextWrapper(
    onPlayNext: () -> Unit,
    content: @Composable () -> Unit
) {
    var hasFired by remember { mutableStateOf(false) }

    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd && !hasFired) {
                hasFired = true
                onPlayNext()
            }
            false
        }
    )

    LaunchedEffect(dismissState.currentValue) {
        if (dismissState.currentValue == SwipeToDismissBoxValue.Settled) {
            hasFired = false
        }
    }

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxSize()
                    .background(Accent) // accent
                    .padding(horizontal = 24.dp)
            ) {
                Icon(
                    painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.AutoMirrored.Rounded.PlaylistAdd),
                    contentDescription = "Play next",
                    // Dark content on the Volt accent keeps the contrast strong.
                    tint = OnAccent,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    ) {
        content()
    }
}

/**
 * Modular dark-themed search bar used across all screens in the app.
 */
@Composable
fun AppSearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "What do you want to listen to?",
    focusRequester: FocusRequester? = null,
    onFocusChange: ((Boolean) -> Unit)? = null,
    onClear: (() -> Unit)? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
) {
    val internalFocusRequester = remember { FocusRequester() }
    val effectiveFocusRequester = focusRequester ?: internalFocusRequester
    // Visual only: the field gains an accent edge and icon while focused.
    var focused by remember { mutableStateOf(false) }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(SoloShape.md)
            .background(Surface3)
            .border(1.dp, if (focused) Accent else Hairline, SoloShape.md)
            .height(height)
            .padding(horizontal = 14.dp)
    ) {
        Icon(
            painter = androidx.compose.ui.graphics.vector.rememberVectorPainter(androidx.compose.material.icons.Icons.Rounded.Search),
            tint = if (focused) Accent else TextSecondary,
            contentDescription = "Search",
            modifier = Modifier
                .size(22.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null
                ) {
                    effectiveFocusRequester.requestFocus()
                }
        )

        TextField(
            enabled = enabled,
            modifier = Modifier
                .weight(1f)
                .onFocusChanged {
                    focused = it.isFocused
                    onFocusChange?.invoke(it.isFocused)
                }
                .focusRequester(effectiveFocusRequester),
            value = query,
            onValueChange = onQueryChange,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = TextPrimary),
            colors = TextFieldDefaults.colors(
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedPlaceholderColor = TextSecondary,
                unfocusedPlaceholderColor = TextSecondary,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedContainerColor = Color.Transparent,
                disabledIndicatorColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = Accent
            ),
            singleLine = true,
            placeholder = {
                Text(
                    text = placeholder,
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Start
                )
            }
        )

        if (query.isNotEmpty()) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                tint = TextSecondary,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable {
                        if (onClear != null) {
                            onClear()
                        } else {
                            onQueryChange("")
                        }
                    }
                    .padding(10.dp)
            )
        }
    }
}