package com.music.spotui.ui.screens

import androidx.compose.material.icons.rounded.Download
import androidx.compose.material3.MaterialTheme
import com.music.spotui.ui.theme.SoloShape
import com.music.spotui.ui.components.soloClickable
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import kotlin.math.roundToInt
import androidx.compose.animation.togetherWith
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.lerp
import kotlin.math.absoluteValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.bumptech.glide.integration.compose.ExperimentalGlideComposeApi
import com.bumptech.glide.integration.compose.GlideImage
import com.music.spotui.R
import com.music.spotui.data.api.Response
import com.music.spotui.data.entity.SongsModel
import com.music.spotui.data.preferences.addLikedSongId
import com.music.spotui.data.preferences.alternativeStreamKey
import com.music.spotui.data.preferences.clearAlternativeStream
import com.music.spotui.data.preferences.getAlternativeStream
import com.music.spotui.data.preferences.isSongLiked
import com.music.spotui.data.preferences.removeLikedSongId
import com.music.spotui.data.preferences.setLocalAlternativeStream
import com.music.spotui.data.preferences.setYouTubeAlternativeStream
import com.music.spotui.di.Palette
import com.music.spotui.di.RepeatMode
import com.music.spotui.di.SongPlayer
import com.music.spotui.ui.components.Snackbar
import com.music.spotui.ui.navigation.Routes
import com.music.spotui.ui.navigation.albumRoute
import com.music.spotui.ui.theme.Canvas
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.viewmodel.PlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.ui.draw.blur
import androidx.compose.material.icons.rounded.AddCircleOutline
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import com.music.spotui.ui.theme.Surface4
import com.music.spotui.ui.theme.Surface2
import com.music.spotui.ui.theme.AccentSoft
import com.music.spotui.ui.theme.TextPrimary
import com.music.spotui.ui.theme.OnAccent
import com.music.spotui.ui.theme.TextSecondary
import com.music.spotui.ui.theme.TextTertiary
import com.music.spotui.ui.theme.Surface3
import com.music.spotui.ui.theme.Gold

private val artistImageCache = java.util.concurrent.ConcurrentHashMap<String, String>()
private val artistIdCache = java.util.concurrent.ConcurrentHashMap<String, String>()

@OptIn(
    ExperimentalGlideComposeApi::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class
)
@Composable
fun PlayerScreen(navController: NavController) {
    val density = LocalDensity.current
    val screenHeight = with(density) {
        val dpHeight = androidx.compose.ui.platform.LocalConfiguration.current.screenHeightDp.dp
        if (dpHeight.value > 0f) dpHeight.toPx() else 2000f
    }
    val coroutineScope = rememberCoroutineScope()

    // offsetY represents the current translation offset of the player screen.
    // It starts at screenHeight (so the screen initially renders fully off-screen)
    // and animates up to 0f.
    var offsetY by remember { mutableFloatStateOf(screenHeight) }
    val animatable = remember { Animatable(screenHeight) }

    var animationJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    // Track whether the entrance animation has completed so the auto-dismiss
    // safety-net doesn't fire on the initial off-screen state.
    var hasAppeared by remember { mutableStateOf(false) }

    suspend fun slideTo(targetValue: Float, velocity: Float = 0f) {
        try {
            animatable.snapTo(offsetY)
            animatable.animateTo(
                targetValue = targetValue,
                initialVelocity = velocity,
                animationSpec = if (targetValue == 0f || targetValue == screenHeight)
                    tween(300) else spring()
            ) {
                offsetY = this.value
            }
        } catch (_: kotlinx.coroutines.CancellationException) {
            // Animation was cancelled, offsetY is wherever the Animatable stopped.
        }
    }

    fun cancelRunningAnimation() {
        animationJob?.cancel()
        animationJob = null
    }

    fun launchAnimation(targetValue: Float, velocity: Float = 0f) {
        cancelRunningAnimation()
        animationJob = coroutineScope.launch {
            slideTo(targetValue, velocity)
        }
    }

    // ── Safety net: if offsetY ever reaches the bottom after the player has
    //    opened, dismiss regardless of how it got there. ──
    LaunchedEffect(offsetY) {
        if (hasAppeared && offsetY >= screenHeight - 1f) {
            navController.navigateUp()
        }
    }

    // ── Configure dialog window for edge-to-edge ──
    // `decorFitsSystemWindows = false` in DialogProperties does NOT actually
    // make a Compose Navigation dialog draw behind system bars (known unfixed
    // issue).  The proven workaround is to copy the Activity window's
    // LayoutParams onto the dialog window and resize the dialog's parent view
    // to fill the screen, see https://stackoverflow.com/a/75768025
    val view = androidx.compose.ui.platform.LocalView.current
    androidx.compose.runtime.SideEffect {
        // Walk up the view tree to find the dialog window.
        var dialogWindow: android.view.Window? = null
        var v: android.view.View? = view
        while (v != null) {
            if (v is androidx.compose.ui.window.DialogWindowProvider) {
                dialogWindow = v.window
                break
            }
            val parent = v.parent
            if (parent is android.view.View) {
                v = parent
            } else {
                if (parent is androidx.compose.ui.window.DialogWindowProvider) {
                    dialogWindow = parent.window
                    break
                }
                break
            }
        }
        // Get the Activity window through the context (works even inside a dialog).
        val activityWindow = generateSequence<android.content.Context>(view.context) { ctx ->
            (ctx as? android.content.ContextWrapper)?.baseContext
        }.filterIsInstance<android.app.Activity>().firstOrNull()?.window

        if (activityWindow != null && dialogWindow != null) {
            // Copy the Activity's window attributes (which already have
            // edge-to-edge configured) onto the dialog window.
            val attrs = android.view.WindowManager.LayoutParams()
            attrs.copyFrom(activityWindow.attributes)
            attrs.type = dialogWindow.attributes.type
            dialogWindow.attributes = attrs
            // Resize the dialog's parent view to fill the screen.
            val parentView = view.parent as? android.view.View
            parentView?.layoutParams = android.widget.FrameLayout.LayoutParams(
                activityWindow.decorView.width,
                activityWindow.decorView.height
            )
            // Make bars transparent.
            dialogWindow.statusBarColor = android.graphics.Color.TRANSPARENT
            dialogWindow.navigationBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    // Animate the player sliding up when first opened
    LaunchedEffect(Unit) {
        // Start closer to final position for a snappier entrance feel
        offsetY = screenHeight * 0.85f
        slideTo(0f)
        hasAppeared = true
    }

    // Function to handle sliding down the player and popping the backstack
    val dismissPlayer: () -> Unit = {
        launchAnimation(screenHeight)
    }

    // Intercept hardware system back press to slide player down smoothly
    BackHandler {
        dismissPlayer()
    }

    // Create nested scroll connection to handle drag gestures
    val nestedScrollConnection = remember(screenHeight) {
        object : NestedScrollConnection {
            override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                val delta = available.y
                // Only cancel ongoing slide animations when the USER is physically
                // dragging.  Fling-driven scroll events must not interfere.
                if (source == NestedScrollSource.Drag) {
                    cancelRunningAnimation()
                }
                // If the player is currently offset (offsetY > 0) and the user
                // drags up (delta < 0), consume the drag to slide the player back
                // up towards 0.
                if (offsetY > 0f && delta < 0f) {
                    val newOffset = (offsetY + delta).coerceIn(0f, screenHeight)
                    offsetY = newOffset
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override fun onPostScroll(
                consumed: Offset,
                available: Offset,
                source: NestedScrollSource
            ): Offset {
                val delta = available.y
                if (source == NestedScrollSource.Drag) {
                    cancelRunningAnimation()
                }
                // Unconsumed downward scroll (delta > 0), translate the sheet
                // down ONLY when the user is physically dragging OR the sheet is
                // already partially offset.  During a fling, if the list just
                // reached its top, the leftover velocity must NOT start dragging
                // the sheet, it should stop here.
                if (delta > 0f && (source == NestedScrollSource.Drag || offsetY > 0f)) {
                    offsetY = (offsetY + delta).coerceIn(0f, screenHeight)
                    return Offset(0f, delta)
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                // When the drag is released, if the player is offset, animate it
                // to either 0f (open) or screenHeight (dismiss).
                if (offsetY > 0f) {
                    val targetValue = when {
                        available.y < -500f -> 0f
                        available.y > 500f -> screenHeight
                        else -> if (offsetY > screenHeight * 0.25f) screenHeight else 0f
                    }
                    val job = coroutineScope.launch {
                        slideTo(targetValue, available.y)
                    }
                    animationJob = job
                    try {
                        job.join()
                    } catch (_: Exception) {
                    }
                    return available
                }
                return Velocity.Zero
            }

            override suspend fun onPostFling(consumed: Velocity, available: Velocity): Velocity {
                // After the fling fully completes, if the sheet ended up partially
                // offset (e.g. a drag-then-fling that didn't trigger onPreFling's
                // snap logic), settle it now.
                if (offsetY > 0f) {
                    val targetValue = if (offsetY > screenHeight * 0.25f) screenHeight else 0f
                    val job = coroutineScope.launch {
                        slideTo(targetValue, available.y)
                    }
                    animationJob = job
                    try {
                        job.join()
                    } catch (_: Exception) {
                    }
                    return available
                }
                return Velocity.Zero
            }
        }
    }

    val playerViewModel: PlayerViewModel = hiltViewModel()
    val songTitle by playerViewModel.currentSongTitle
    val songSinger by playerViewModel.currentSongSinger
    val songCoverUri by playerViewModel.currentSongCoverUri
    val songPlayingState by playerViewModel.currentSongPlayingState
    val songId by playerViewModel.currentSongId

    // Reconcile play/pause state immediately when opening the full screen player, ensuring
    // the play/pause button always accurately reflects whether audio is playing in SongPlayer.
    LaunchedEffect(Unit) {
        playerViewModel.syncWithPlayer()
    }
    val context = LocalContext.current
    val isLiked = remember(songId) {
        mutableStateOf(isSongLiked(context, songId.toString()))
    }
    var showMenu by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showSavedIn by remember { mutableStateOf(false) }
    var showArtistSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }

    if (showMenu) {
        PlayerOptionsSheet(
            navController = navController,
            playerViewModel = playerViewModel,
            context = context,
            isLiked = isLiked,
            onDismiss = { showMenu = false },
            onOpenQueue = { showQueueSheet = true }
        )
    }

    if (showSavedIn) {
        playerViewModel.queue.value.firstOrNull { it.id == songId }?.let { track ->
            com.music.spotui.ui.components.SavedInSheet(
                song = track,
                context = context,
                onDismiss = { showSavedIn = false },
                onLikedChanged = { isLiked.value = it },
            )
        } ?: run { showSavedIn = false }
    }

    if (showArtistSheet) {
        ArtistsSheet(
            artistNames = songSinger.split(",").map { it.trim() }.filter { it.isNotBlank() },
            artistIds = playerViewModel.currentArtistIds.value.split(",").map { it.trim() },
            context = context,
            onDismiss = { showArtistSheet = false },
            navController = navController,
        )
    }


    var songProgress by remember {
        mutableStateOf(
            maxOf(
                0f,
                SongPlayer.getCurrentPosition().toFloat()
            )
        )
    }
    var songDurationText by remember { mutableStateOf("0") }
    var songProgressText by remember { mutableStateOf("") }

    songDurationText = if (SongPlayer.getDuration() < 0) {
        "0:00"
    } else {
        playerViewModel.formatDuration(SongPlayer.getDuration())
    }
    songProgressText = if (SongPlayer.getCurrentPosition() < 0) {
        "0:00"
    } else {
        playerViewModel.formatDuration(SongPlayer.getCurrentPosition())
    }


    var dominentColor by remember {
        mutableStateOf(com.music.spotui.ui.theme.Canvas)
    }
    // The artwork tone eases between tracks instead of snapping.
    val artTone by androidx.compose.animation.animateColorAsState(
        targetValue = dominentColor,
        animationSpec = com.music.spotui.ui.theme.SoloMotion.emphasized(),
        label = "artTone",
    )
    // Artwork breathes down slightly while paused and springs back on play.
    val artScale by animateFloatAsState(
        targetValue = if (songPlayingState) 1f else 0.92f,
        animationSpec = com.music.spotui.ui.theme.SoloMotion.spring(),
        label = "artScale",
    )
    var showDevicesSheet by remember { mutableStateOf(false) }
    // Once per cover: this screen recomposes every 300 ms while playing.
    LaunchedEffect(songCoverUri) {
        if (songCoverUri.isNotBlank()) {
            Palette().extractSecondColorFromCoverUrl(context = context, songCoverUri) { color ->
                dominentColor = color
            }
        }
    }

    val songsResponse by playerViewModel.songs.collectAsState()
    val shuffle by playerViewModel.shuffleState
    val repeat by playerViewModel.repeatState

    val songs = if (songsResponse is Response.Success) {
        (songsResponse as Response.Success).data
    } else {
        emptyList<SongsModel>()
    }

    // The queue is whatever list the user actually started playing (album tracks,
    // search results, liked songs), stored when the song was tapped. Falling back
    // to the global top-tracks feed used to crash / be empty (it's rate-limited).
    val queueSongs by playerViewModel.queue

    // ── Now-playing swipe pager ──
    // Index of the playing track in the queue (fallback to 0 so the pager is valid
    // even before the queue/current id line up).
    val currentIndex = queueSongs.indexOfFirst { it.id == songId }
        .let { if (it >= 0) it else 0 }
    val artworkPagerState = rememberPagerState(
        initialPage = currentIndex.coerceIn(0, (queueSongs.size - 1).coerceAtLeast(0)),
        pageCount = { queueSongs.size.coerceAtLeast(1) },
    )
    var wasUserDragged by remember { mutableStateOf(false) }

    LaunchedEffect(artworkPagerState) {
        artworkPagerState.interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is androidx.compose.foundation.interaction.DragInteraction.Start -> wasUserDragged = true
                is androidx.compose.foundation.interaction.DragInteraction.Cancel -> wasUserDragged = false
                is androidx.compose.foundation.interaction.DragInteraction.Stop -> {}
            }
        }
    }

    // External track changes (auto-advance, prev/next buttons, queue edits) → snap the
    // pager to the new track. Guard on settled state so we don't fight an in-progress swipe.
    LaunchedEffect(currentIndex, queueSongs.size) {
        if (currentIndex in 0 until queueSongs.size &&
            artworkPagerState.currentPage != currentIndex &&
            !artworkPagerState.isScrollInProgress
        ) {
            artworkPagerState.scrollToPage(currentIndex)
        }
    }
    // User settled the pager on a different page via touch drag → play that track. Compare against the
    // live current id to avoid a replay feedback loop.
    LaunchedEffect(artworkPagerState) {
        snapshotFlow { artworkPagerState.settledPage }
            .distinctUntilChanged()
            .collect { page ->
                if (wasUserDragged) {
                    wasUserDragged = false
                    val currentQueue = playerViewModel.queue.value
                    val currentIdx = currentQueue.indexOfFirst { it.id == playerViewModel.currentSongId.value }
                        .let { if (it >= 0) it else 0 }
                    if (page != currentIdx && page in currentQueue.indices) {
                        playerViewModel.playSongAt(currentQueue, page, context)
                        currentQueue.getOrNull(page)?.let { target ->
                            isLiked.value = isSongLiked(context, target.id.toString())
                        }
                    }
                }
            }
    }

    // The next two tracks are pre-resolved by CurrentSongState; here we only make the
    // previous one ready (URL only) so a back-skip is instant too.
    LaunchedEffect(songId, queueSongs) {
        val idx = queueSongs.indexOfFirst { it.id == songId }
        if (idx > 0) {
            queueSongs.getOrNull(idx - 1)?.let { SongPlayer.prefetch(it.url, context, preBuffer = false) }
        }
    }

    // Load the current track's Spotify Canvas (full-screen looping video background).
    LaunchedEffect(songId, queueSongs) {
        val track = queueSongs.firstOrNull { it.id == songId }
        // Downloaded tracks are meant for offline use, skip the Canvas video
        // (which needs network to stream) and always show the squared artwork.
        val downloaded = track != null &&
            com.music.spotui.data.preferences.isDownloaded(context, track.id.toString())
        playerViewModel.loadCanvas(if (downloaded) "" else track?.spotifyTrackId.orEmpty())
    }




    LaunchedEffect(songId, songPlayingState) {
        while (true) {
            // Keep playing state continuously reconciled with physical audio engine state
            playerViewModel.syncWithPlayer()

            val dur = SongPlayer.getDuration()
            songDurationText = if (dur < 0) "0:00" else playerViewModel.formatDuration(dur)

            val pos = SongPlayer.getCurrentPosition()
            songProgress = pos.toFloat()
            songProgressText = if (pos < 0) "0:00" else playerViewModel.formatDuration(pos)

            if (songPlayingState) {
                delay(300L)
            } else {
                if (dur > 0) {
                    delay(2000L)
                } else {
                    delay(300L)
                }
            }
        }
    }


    val canvasUrl = playerViewModel.canvasUrl.value
    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(nestedScrollConnection)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent(PointerEventPass.Initial)
                        val anyPressed = event.changes.any { it.pressed }
                        if (anyPressed) {
                            if (animationJob != null) {
                                cancelRunningAnimation()
                            }
                        }
                    }
                }
            }
            .graphicsLayer {
                translationY = offsetY
                alpha = (1f - (offsetY / screenHeight)).coerceIn(0f, 1f)
            }
            .background(Canvas)
            .background(
                // The artwork's tone crowns the screen and settles into the ink canvas.
                Brush.verticalGradient(
                    0f to artTone.copy(alpha = 0.55f),
                    0.5f to lerp(artTone, Canvas, 0.78f),
                    1f to Canvas,
                )
            )
    ) {
        // Android 12+ only: a heavily blurred copy of the artwork glows behind the
        // controls. RenderEffect blur does not exist below API 31, where the tone
        // gradient above is the whole background.
        if (canvasUrl == null && songCoverUri.isNotBlank() &&
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S
        ) {
            GlideImage(
                model = songCoverUri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.66f)
                    // Guarded parallax/depth: the blurred backdrop drifts and eases its
                    // scale with the dismiss drag so pulling the sheet feels dimensional.
                    // Derived from the existing offsetY/playing state so it adds no new
                    // recomposition trigger; API 31+ only (inside the RenderEffect guard).
                    .graphicsLayer {
                        val dragFraction = (offsetY / screenHeight).coerceIn(0f, 1f)
                        translationY = -dragFraction * 48.dp.toPx()
                        val base = if (songPlayingState) 1.06f else 1.02f
                        val s = base + dragFraction * 0.06f
                        scaleX = s
                        scaleY = s
                    }
                    .blur(64.dp)
                    .alpha(0.35f),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Transparent,
                            0.40f to Color.Transparent,
                            0.66f to Canvas.copy(alpha = 0.88f),
                            1f to Canvas,
                        )
                    )
            )
        }
        if (canvasUrl != null) {
            // Spotify Canvas: the looping video fills the whole now-playing screen
            // edge-to-edge behind the controls (the "immersive" treatment), with a
            // scrim on top so the title, slider and buttons stay readable.
            CanvasVideo(
                url = canvasUrl,
                modifier = Modifier.fillMaxSize(),
                onError = { playerViewModel.clearCanvas() },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.50f),
                                Color.Black.copy(alpha = 0.10f),
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.80f),
                            )
                        )
                    )
            )
        }
        androidx.compose.foundation.lazy.LazyColumn(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .fillParentMaxHeight()
                        .fillMaxWidth()
                        .navigationBarsPadding(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Spacer(modifier = Modifier.statusBarsPadding())
                    PlayerTopBar(
                        navController = navController,
                        onMenuClick = { showMenu = true },
                        contextName = playerViewModel.currentSongAlbum.value,
                        onLyricsClick = { showLyrics = true },
                        onQueueClick = { navController.navigate(Routes.Queue.route) },
                        onBackClick = { dismissPlayer() }
                    )
                    // Swipe the artwork left/right to skip to the next/previous track. Using a
                    // HorizontalPager makes the artwork follow the finger and snap, syncing the
                    // change with the track (Spotify's now-playing gesture) instead of an abrupt
                    // swipe-then-switch. When the queue is empty fall back to a static image.
                    // When a Canvas is playing it fills the screen behind this column, so the
                    // artwork is hidden (alpha 0) rather than removed, the pager stays in
                    // the layout so the swipe-to-skip gesture keeps working over the video.
                    // The artwork is the FLEXIBLE part of the screen (weight), capped at its
                    // old 385dp size. On short/scaled displays the fixed-size version pushed
                    // the slider and playback buttons off the bottom of the screen; now the
                    // artwork shrinks instead and the controls always fit.
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        if (queueSongs.isEmpty()) {
                            GlideImage(
                                modifier = Modifier
                                    .sizeIn(maxWidth = 385.dp, maxHeight = 385.dp)
                                    .aspectRatio(1f)
                                    .padding(27.dp)
                                    .graphicsLayer { scaleX = artScale; scaleY = artScale }
                                    // Soft violet glow + thin gold hairline for a premium edge.
                                    .shadow(
                                        elevation = 34.dp,
                                        shape = com.music.spotui.ui.theme.SoloShape.xl,
                                        clip = false,
                                        ambientColor = Accent,
                                        spotColor = Accent,
                                    )
                                    .clip(com.music.spotui.ui.theme.SoloShape.xl)
                                    .border(1.dp, Gold.copy(alpha = 0.45f), com.music.spotui.ui.theme.SoloShape.xl)
                                    .alpha(if (canvasUrl != null) 0f else 1f),
                                model = songCoverUri,
                                contentScale = ContentScale.Crop,
                                contentDescription = ""
                            )
                        } else {
                            HorizontalPager(
                                state = artworkPagerState,
                                modifier = Modifier
                                    .sizeIn(maxWidth = 385.dp, maxHeight = 385.dp)
                                    .aspectRatio(1f),
                            ) { page ->
                                // The playing page sits full size with a soft drop shadow
                                // for a premium, lifted feel; neighbouring pages scale down
                                // slightly so the current artwork clearly stands out as the
                                // finger drags between tracks.
                                val pageOffset = (
                                    (artworkPagerState.currentPage - page) +
                                        artworkPagerState.currentPageOffsetFraction
                                    ).absoluteValue.coerceIn(0f, 1f)
                                val scale = androidx.compose.ui.util.lerp(1f, 0.86f, pageOffset) *
                                    (if (page == currentIndex) artScale else 1f)
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                GlideImage(
                                    modifier = Modifier
                                        .fillMaxSize(0.86f)
                                        .graphicsLayer {
                                            scaleX = scale
                                            scaleY = scale
                                        }
                                        // Soft violet glow lifts the current artwork; a thin
                                        // gold hairline gives it a crafted, premium edge.
                                        .shadow(
                                            elevation = 34.dp,
                                            shape = com.music.spotui.ui.theme.SoloShape.xl,
                                            clip = false,
                                            ambientColor = Accent,
                                            spotColor = Accent,
                                        )
                                        .clip(com.music.spotui.ui.theme.SoloShape.xl)
                                        .border(
                                            1.dp,
                                            Gold.copy(alpha = if (page == currentIndex) 0.45f else 0f),
                                            com.music.spotui.ui.theme.SoloShape.xl,
                                        )
                                        .alpha(if (canvasUrl != null) 0f else 1f),
                                    model = queueSongs.getOrNull(page)?.coverUri ?: songCoverUri,
                                    contentScale = ContentScale.Crop,
                                    contentDescription = "Album artwork"
                                )
                                }
                            }
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                    ) {
                        // Reads each 300ms tick (songProgress recomposition) so it reflects
                        // the current engine, Spotify vs Lossless (SpotiFLAC) vs YouTube.
                        PlayerInfo(
                            songTitle, songSinger, songId, context, isLiked,
                            source = SongPlayer.currentSource,
                            quality = SongPlayer.currentQuality,
                            isResolving = playerViewModel.isResolving.value,
                            resolveStatus = playerViewModel.resolveStatus.value,
                            resolveError = playerViewModel.resolveError.value,
                            resolveDetailNote = playerViewModel.resolveDetailNote.value,
                            onArtistClick = {
                                val artists = songSinger.split(",").map { it.trim() }
                                    .filter { it.isNotBlank() }
                                val ids = playerViewModel.currentArtistIds.value.split(",")
                                    .map { it.trim() }
                                if (artists.size == 1) {
                                    val aId = ids.getOrElse(0) { "" }
                                    navController.navigate(
                                        com.music.spotui.ui.navigation.artistRoute(
                                            artists[0],
                                            aId
                                        )
                                    )
                                } else {
                                    showArtistSheet = true
                                }
                            },
                            spotifyTrackId = queueSongs.firstOrNull { it.id == songId }?.spotifyTrackId.orEmpty(),
                            onShowSavedIn = { showSavedIn = true },
                            isExplicit = queueSongs.firstOrNull { it.id == songId }?.explicit == true,
                        )

                        // Smooth scrubbing: while dragging, the thumb follows the finger
                        // locally (no seek per delta, that fired a web seek on every pixel
                        // and fought the polled position, making it jerky). We seek ONCE on
                        // release.
                        var isDragging by remember { mutableStateOf(false) }
                        var dragValue by remember { mutableStateOf(0f) }
                        val durForBar = SongPlayer.getDuration().toFloat()
                        val liveFraction = durForBar.let { dur ->
                            if (dur > 0f) (SongPlayer.getCurrentPosition()
                                .toFloat() / dur).coerceIn(0f, 1f) else 0f
                        }
                        val bufferedFraction = if (durForBar > 0f)
                            (SongPlayer.getBufferedPosition().toFloat() / durForBar).coerceIn(0f, 1f) else 0f
                        CustomSlider(
                            value = if (isDragging) dragValue else liveFraction,
                            bufferedFraction = bufferedFraction,
                            onValueChange = { newValue ->
                                isDragging = true
                                dragValue = newValue
                            },
                            onValueChangeFinished = {
                                val dur = SongPlayer.getDuration()
                                if (dur > 0) SongPlayer.seekTo((dragValue * dur).toLong())
                                isDragging = false
                                if (!songPlayingState) {
                                    SongPlayer.play()
                                    playerViewModel.updateSongState(
                                        playerViewModel.currentSongCoverUri.value,
                                        playerViewModel.currentSongTitle.value,
                                        playerViewModel.currentSongSinger.value,
                                        true,
                                        playerViewModel.currentSongId.value,
                                        playerViewModel.currentSongIndex.value,
                                        playerViewModel.currentSongAlbum.value
                                    )
                                }
                            },
                            valueRange = 0f..1f,
                            steps = 0,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp, 12.dp, 16.dp, 0.dp),
                            colors = null
                        )

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(25.dp, 0.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                // While scrubbing, show the dragged time so the label tracks the finger.
                                text = if (isDragging) {
                                    val dur = SongPlayer.getDuration()
                                    if (dur > 0) playerViewModel.formatDuration((dragValue * dur).toLong()) else "0:00"
                                } else songProgressText,
                                color = TextTertiary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            )
                            Text(
                                text = songDurationText,
                                color = TextTertiary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium.copy(fontFeatureSettings = "tnum"),
                            )
                        }


                        // Next-up peek: the song queued after the current one. Tapping it
                        // opens the full queue. Hidden when nothing follows.
                        val nextUp = queueSongs.getOrNull(currentIndex + 1)
                        if (nextUp != null) {
                            NextUpPeek(
                                song = nextUp,
                                onClick = { navController.navigate(Routes.Queue.route) },
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        PlayerFull(
                            songPlayingState,
                            playerViewModel,
                            context,
                            isLiked,
                            shuffle,
                            repeat,
                            queueSongs
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Spotify-style bottom row: current audio device (Connect) on the left,
                    // share + queue on the right.
                    PlayerConnectRow(
                        navController = navController,
                        context = context,
                        currentTrack = queueSongs.firstOrNull { it.id == playerViewModel.currentSongId.value },
                        onOpenDevices = { showDevicesSheet = true },
                        onOpenQueue = { showQueueSheet = true }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                }
            }
            item {
                InlineLyrics(
                    title = songTitle,
                    artist = songSinger,
                    album = playerViewModel.currentSongAlbum.value,
                    accentColor = dominentColor,
                    onExpand = { showLyrics = true },
                )
            }
        }

        if (showLyrics) {
            LyricsScreen(
                title = songTitle,
                artist = songSinger,
                album = playerViewModel.currentSongAlbum.value,
                accentColor = dominentColor,
                onClose = { showLyrics = false }
            )
        }

        if (showDevicesSheet) {
            com.music.spotui.ui.components.DevicesSheet(
                context = context,
                onDismiss = { showDevicesSheet = false }
            )
        }

        if (showQueueSheet) {
            QueueSheet(
                navController = navController,
                onDismiss = { showQueueSheet = false }
            )
        }
    }
}


@Composable
fun PlayerTopBar(
    navController: NavController,
    onMenuClick: () -> Unit,
    contextName: String = "",
    onBackClick: () -> Unit,
    onLyricsClick: (() -> Unit)? = null,
    onQueueClick: (() -> Unit)? = null,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        com.music.spotui.ui.components.SoloIconButton(
            icon = Icons.Rounded.KeyboardArrowDown,
            contentDescription = "Close player",
            onClick = onBackClick,
            filled = true,
            iconSize = 26.dp,
        )

        // The source context (album / playlist) the track is playing from.
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 8.dp),
        ) {
            Text(
                text = "PLAYING FROM",
                color = Accent,
                style = androidx.compose.material3.MaterialTheme.typography.labelSmall,
                letterSpacing = 1.6.sp,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = contextName.ifBlank { "Now Playing" },
                color = TextPrimary,
                style = androidx.compose.material3.MaterialTheme.typography.titleSmall,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        if (onLyricsClick != null) {
            com.music.spotui.ui.components.SoloIconButton(
                icon = Icons.Rounded.Lyrics,
                contentDescription = "Lyrics",
                onClick = onLyricsClick,
            )
        }
        if (onQueueClick != null) {
            com.music.spotui.ui.components.SoloIconButton(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                contentDescription = "Up next",
                onClick = onQueueClick,
            )
        }
        com.music.spotui.ui.components.SoloIconButton(
            icon = Icons.Rounded.MoreVert,
            contentDescription = "More options",
            onClick = onMenuClick,
        )
    }
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PlayerInfo(
    songTitle: String,
    songSinger: String,
    songId: Int,
    context: Context,
    isLiked: MutableState<Boolean>,
    source: String = "",
    quality: String = "",
    isResolving: Boolean = false,
    resolveStatus: String = "",
    resolveError: String? = null,
    resolveDetailNote: String? = null,
    onArtistClick: (() -> Unit)? = null,
    spotifyTrackId: String = "",
    onShowSavedIn: (() -> Unit)? = null,
    isExplicit: Boolean = false,
) {

    var snackbarMessage by remember {
        mutableStateOf("")
    }
    var snackbarVisible by remember {
        mutableStateOf(false)
    }
    var showStreamDetailDialog by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(snackbarVisible) {
        delay(1500)
        snackbarVisible = false
    }

    if (showStreamDetailDialog) {
        AlertDialog(
            onDismissRequest = { showStreamDetailDialog = false },
            title = {
                Text("Stream Resolution Info", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            },
            text = {
                Column {
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                        Text("Engine / Source:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(115.dp))
                        Text(if (source.isBlank()) "Standard" else source, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                    }
                    if (quality.isNotBlank()) {
                        Row(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Text("Audio Quality:", color = TextSecondary, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.width(115.dp))
                            Text(quality, color = TextPrimary, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                    if (!resolveDetailNote.isNullOrBlank()) {
                        Spacer(Modifier.height(8.dp))
                        Text("Status Note:", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            resolveDetailNote,
                            color = com.music.spotui.ui.theme.Warning,
                            style = MaterialTheme.typography.bodySmall,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(SoloShape.sm)
                                .background(com.music.spotui.ui.theme.Warning.copy(alpha = 0.13f))
                                .padding(10.dp)
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    Text("Resolution Trace Log:", color = TextSecondary, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    SelectionContainer {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 220.dp)
                                .clip(SoloShape.sm)
                                .background(Canvas)
                                .padding(10.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            Column {
                                val logs = com.music.spotui.di.SongPlayer.resolutionLogs.toList()
                                if (logs.isEmpty()) {
                                    Text("No live trace logs captured for current playback session.", color = TextTertiary, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace)
                                } else {
                                    logs.forEach { line ->
                                        val textColor = when {
                                            line.contains("✓") -> com.music.spotui.ui.theme.Success
                                            line.contains("✗") -> com.music.spotui.ui.theme.Danger
                                            line.contains("Fallback") || line.contains("Exhausted") -> com.music.spotui.ui.theme.Warning
                                            else -> TextSecondary
                                        }
                                        Text(line, color = textColor, style = MaterialTheme.typography.bodySmall, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, lineHeight = 15.sp)
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Row {
                    val clipboardManager = androidx.compose.ui.platform.LocalClipboardManager.current
                    TextButton(onClick = { showStreamDetailDialog = false }) {
                        Text("Close", color = TextPrimary, style = androidx.compose.material3.MaterialTheme.typography.labelLarge)
                    }
                    Spacer(Modifier.width(8.dp))
                    com.music.spotui.ui.components.SoloPillButton(text = "Copy Logs", onClick = {
                        val logs = com.music.spotui.di.SongPlayer.resolutionLogs.toList()
                        val text = logs.joinToString("\n").ifBlank { "No logs available" }
                        clipboardManager.setText(androidx.compose.ui.text.AnnotatedString(text))
                        Toast.makeText(context, "Resolution logs copied to clipboard", Toast.LENGTH_SHORT).show()
                    })
                }
            },
            containerColor = Surface2,
            shape = SoloShape.xl,
            titleContentColor = TextPrimary,
            textContentColor = TextPrimary,
        )
    }

    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 24.dp, end = 12.dp, top = 10.dp, bottom = 10.dp)
    ) {

        if (snackbarVisible) {
            Snackbar(showMessage = snackbarMessage)
        } else {
            Row(
                horizontalArrangement = Arrangement.Start,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 12.dp)
            ) {
                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (isExplicit) {
                            com.music.spotui.ui.components.ExplicitBadge(size = 11.sp)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(
                            modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                            text = songTitle,
                            color = TextPrimary,
                            // Sora Bold 24, left-aligned.
                            style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            softWrap = false,
                        )
                    }
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = songSinger,
                        color = TextSecondary,
                        style = androidx.compose.material3.MaterialTheme.typography.bodyLarge,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                        modifier = if (onArtistClick != null) Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { onArtistClick() } else Modifier,
                    )
                    val isResolvingState = isResolving && resolveStatus.isNotBlank()
                    val hasError = !resolveError.isNullOrBlank()
                    val displaySource =
                        if (isResolvingState) "Resolving" else if (hasError) "Error" else source
                    if (displaySource.isNotBlank()) {
                        // Source badge: green = real Spotify; other colors = not Spotify
                        // (Lossless via SpotiFLAC's Tidal/Qobuz/Amazon/Deezer mirrors, or YouTube).
                        val badgeColor = when {
                            hasError -> com.music.spotui.ui.theme.Danger
                            isResolvingState -> AccentSoft
                            source == "Spotify" -> Accent
                            source.startsWith("Lossless") -> AccentSoft
                            source.startsWith("Deezer") || source == "Deezer" -> AccentSoft
                            source == "Downloaded" -> TextSecondary
                            else -> com.music.spotui.ui.theme.Danger
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .padding(top = 8.dp)
                                .clip(SoloShape.pill)
                                .background(Surface3)
                                .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.pill)
                                .clickable { showStreamDetailDialog = true }
                                .padding(horizontal = 10.dp, vertical = 5.dp),
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(7.dp)
                                    .clip(CircleShape)
                                    .background(badgeColor)
                            )
                            Text(
                                text = when {
                                    hasError -> resolveError.orEmpty()
                                    isResolvingState -> resolveStatus
                                    else -> {
                                        source + (if (quality.isNotBlank()) " • $quality" else "")
                                    }
                                },
                                color = if (hasError) badgeColor else TextPrimary,
                                style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                modifier = Modifier.padding(start = 6.dp),
                            )
                        }
                    }
                }
            }

            // The like glyph pops and its tint animates to the accent on toggle so the
            // save registers tactilely.
            val likeTint by androidx.compose.animation.animateColorAsState(
                targetValue = if (isLiked.value) Accent else TextPrimary,
                animationSpec = com.music.spotui.ui.theme.SoloMotion.standard(),
                label = "playerLikeTint",
            )
            val likeScale by androidx.compose.animation.core.animateFloatAsState(
                targetValue = if (isLiked.value) com.music.spotui.ui.theme.SoloMotion.TOGGLE_POP_SCALE else 1f,
                animationSpec = com.music.spotui.ui.theme.SoloMotion.toggleSpring(),
                label = "playerLikeScale",
            )
            Icon(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .graphicsLayer { scaleX = likeScale; scaleY = likeScale }
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = {
                            if (isLiked.value && onShowSavedIn != null) {
                                // Already saved, second tap opens the Spotify-style
                                // "Saved in" sheet (Liked Songs + playlists) instead of
                                // silently unliking.
                                onShowSavedIn()
                                return@combinedClickable
                            }
                            if (isLiked.value) {
                                removeLikedSongId(context, songId.toString())
                                snackbarMessage = "Removed from Liked Songs"
                            } else {
                                // Save the whole track when it is in the queue, so Liked
                                // Songs can show it without any account.
                                val song = SongPlayer.queuedSong(songId)
                                if (song != null) {
                                    com.music.spotui.data.preferences.addLikedSong(context, song)
                                } else {
                                    addLikedSongId(context, songId.toString())
                                }
                                snackbarMessage = "Added to Liked Songs"
                            }
                            snackbarVisible = true
                            isLiked.value = isSongLiked(context, songId.toString())
                            // Mirror the like to the real Spotify account.
                            com.music.spotui.data.api.SpotifySync.setTrackSaved(
                                context,
                                spotifyTrackId,
                                isLiked.value
                            )
                        },
                        onLongClick = { onShowSavedIn?.invoke() },
                    )
                    .padding(9.dp),
                imageVector = if (isLiked.value) Icons.Rounded.CheckCircle else Icons.Rounded.AddCircleOutline,
                tint = likeTint,
                contentDescription = if (isLiked.value) "Saved to Liked Songs" else "Save to Liked Songs"
            )
        }
    }


}

/**
 * Solo seek bar: a 3dp Surface4 track with a faint gold buffered-ahead tint, the
 * violet→cyan accent gradient on the played part, and a Gold scrubber thumb that scales
 * up on drag (with a soft gold halo), inside a 48dp touch band. Exposes slider semantics
 * so TalkBack can read and set the position.
 *
 * [bufferedFraction] is the stream's buffered-ahead position (0..1); it only tints the
 * track and never affects seeking.
 */
@Composable
fun CustomSlider(
    modifier: Modifier = Modifier,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: (() -> Unit)? = null,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 0,
    bufferedFraction: Float = 0f,
    colors: Any? = null, // kept for API compat; unused
) {
    var isDragging by remember { mutableStateOf(false) }
    val thumbDp by animateDpAsState(
        targetValue = if (isDragging) 18.dp else 12.dp,
        animationSpec = com.music.spotui.ui.theme.SoloMotion.spring(),
        label = "thumbSize",
    )
    val span = (valueRange.endInclusive - valueRange.start).takeIf { it > 0f } ?: 1f
    val fraction = ((value - valueRange.start) / span).coerceIn(0f, 1f)

    Box(
        modifier = modifier
            .height(48.dp)
            .semantics {
                contentDescription = "Seek"
                progressBarRangeInfo = androidx.compose.ui.semantics.ProgressBarRangeInfo(value, valueRange)
                setProgress { target ->
                    onValueChange(target.coerceIn(valueRange.start, valueRange.endInclusive))
                    onValueChangeFinished?.invoke()
                    true
                }
            }
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val f = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                    onValueChange(valueRange.start + f * span)
                    onValueChangeFinished?.invoke()
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures(
                    onDragStart = { isDragging = true },
                    onDragEnd = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onDragCancel = {
                        isDragging = false
                        onValueChangeFinished?.invoke()
                    },
                    onHorizontalDrag = { change, _ ->
                        change.consume()
                        val f = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                        onValueChange(valueRange.start + f * span)
                    }
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(20.dp)) {
            val trackPx = 3.dp.toPx()
            val y = size.height / 2f
            val thumbR = thumbDp.toPx() / 2f
            val x = (fraction * size.width).coerceIn(0f, size.width)
            // Base (unbuffered) track.
            drawLine(
                color = com.music.spotui.ui.theme.Surface4,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = trackPx,
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
            )
            // Buffered-ahead tint: a faint gold wash showing how far the stream is ready.
            val bufX = (bufferedFraction.coerceIn(0f, 1f) * size.width).coerceIn(0f, size.width)
            if (bufX > x) {
                drawLine(
                    color = Gold.copy(alpha = 0.22f),
                    start = Offset(x, y),
                    end = Offset(bufX, y),
                    strokeWidth = trackPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            if (x > 0f) {
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(com.music.spotui.ui.theme.AccentSoft, Accent, com.music.spotui.ui.theme.AccentDeep),
                        startX = 0f,
                        endX = size.width,
                    ),
                    start = Offset(0f, y),
                    end = Offset(x, y),
                    strokeWidth = trackPx,
                    cap = androidx.compose.ui.graphics.StrokeCap.Round,
                )
            }
            // Soft gold halo while dragging so the thumb reads as the premium scrubber.
            if (isDragging) {
                drawCircle(color = Gold.copy(alpha = 0.28f), radius = thumbR * 1.9f, center = Offset(x, y))
            }
            drawCircle(color = Gold, radius = thumbR, center = Offset(x, y))
        }
    }
}


/**
 * "Next up" peek: a compact strip showing the song queued after the current one, with a
 * small thumbnail, a gold-tinted "NEXT UP" label and a chevron. Tapping it opens the full
 * queue. The caller hides it when there is no next item.
 */
@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun NextUpPeek(
    song: SongsModel,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp)
            .padding(top = 6.dp)
            .clip(SoloShape.pill)
            .background(Surface2.copy(alpha = 0.6f))
            .border(1.dp, com.music.spotui.ui.theme.HairlineAccent, SoloShape.pill)
            .soloClickable(ripple = true, onClick = onClick)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        GlideImage(
            model = song.coverUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(36.dp)
                .clip(SoloShape.sm),
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = "NEXT UP",
                color = Gold,
                style = MaterialTheme.typography.labelSmall,
                letterSpacing = 1.4.sp,
            )
            Spacer(Modifier.height(1.dp))
            Text(
                text = song.title + (if (song.singer.isNotBlank()) " • ${song.singer}" else ""),
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
        }
        Icon(
            imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
            contentDescription = "Open queue",
            tint = TextSecondary,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
fun PlayerFull(
    songPlayingState: Boolean,
    playerViewModel: PlayerViewModel,
    context: Context,
    isLiked: MutableState<Boolean>,
    shuffle: Boolean,
    repeat: RepeatMode,
    queueSongs: List<SongsModel>
) {

    // A light tactile tick on skip so the transport feels premium under the thumb.
    val haptics = androidx.compose.ui.platform.LocalHapticFeedback.current

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 22.dp, vertical = 16.dp)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .soloClickable(ripple = true) {
                    if (shuffle) {
                        playerViewModel.updateShuffleState(false)
                    } else {
                        playerViewModel.updateShuffleState(true)
                    }

                }
        ) {
            Icon(
                modifier = Modifier.size(24.dp),
                tint = if (shuffle) {
                    Accent
                } else {
                    TextPrimary
                },
                imageVector = Icons.Rounded.Shuffle,
                contentDescription = if (shuffle) "Shuffle on" else "Shuffle off"
            )
            // 4dp accent dot marks an active mode (same as repeat).
            if (shuffle) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Accent, shape = CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
        Icon(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .soloClickable(ripple = true) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    // The queue itself is already in shuffled order when shuffle
                    // is on (reordered once at toggle), never re-shuffle per tap.
                    playerViewModel.playPreviousSong(queueSongs, context)
                    isLiked.value =
                        isSongLiked(context, playerViewModel.currentSongId.value.toString())
                }
                .padding(5.dp),
            tint = TextPrimary,
            imageVector = Icons.Rounded.SkipPrevious,
            contentDescription = "Previous"
        )
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                // requiredSize forces an exact 76×76 square even if the parent Column
                // constrains height, .size() alone let it get squished into an ellipse.
                .requiredSize(76.dp)
                // A soft accent glow under the play disc so it reads as the primary control.
                .shadow(
                    elevation = 22.dp,
                    shape = CircleShape,
                    clip = false,
                    ambientColor = Accent,
                    spotColor = Accent,
                )
                .clip(CircleShape)
                .background(com.music.spotui.ui.theme.AccentBrush)
                .soloClickable(ripple = true) {
                    // Single source of truth: toggle based on the engine's real
                    // state so the button never gets stuck showing the wrong icon.
                    playerViewModel.togglePlayPause()
                },
            contentAlignment = Alignment.Center
        ) {
            // Only show the spinner while LOCATING the stream (resolving). During
            // plain buffering keep the play/pause icon visible and correct — the
            // button stays tappable and the state is clear.
            if (playerViewModel.isResolving.value) {
                androidx.compose.material3.CircularProgressIndicator(
                    modifier = Modifier.size(30.dp),
                    color = OnAccent,
                    strokeWidth = 3.dp
                )
            } else {
                androidx.compose.animation.AnimatedContent(
                    targetState = songPlayingState,
                    transitionSpec = {
                        (androidx.compose.animation.fadeIn(com.music.spotui.ui.theme.SoloMotion.quick()) +
                            androidx.compose.animation.scaleIn(com.music.spotui.ui.theme.SoloMotion.spring(), initialScale = 0.6f))
                            .togetherWith(
                                androidx.compose.animation.fadeOut(com.music.spotui.ui.theme.SoloMotion.quick()) +
                                    androidx.compose.animation.scaleOut(com.music.spotui.ui.theme.SoloMotion.quick(), targetScale = 0.6f)
                            )
                    },
                    label = "playPause",
                ) { playing ->
                    Icon(
                        modifier = Modifier
                            .size(38.dp),
                        tint = OnAccent,
                        imageVector = if (playing) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playing) "Pause" else "Play"
                    )
                }
            }
        }

        Icon(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .soloClickable(ripple = true) {
                    haptics.performHapticFeedback(androidx.compose.ui.hapticfeedback.HapticFeedbackType.LongPress)
                    playerViewModel.playNextSongs(queueSongs, context)
                    isLiked.value =
                        isSongLiked(context, playerViewModel.currentSongId.value.toString())
                }
                .padding(5.dp),
            tint = TextPrimary,
            imageVector = Icons.Rounded.SkipNext,
            contentDescription = "Next"
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .soloClickable(ripple = true) {
                    val nextRepeat = when (repeat) {
                        RepeatMode.OFF -> RepeatMode.ALL
                        RepeatMode.ALL -> RepeatMode.ONE
                        RepeatMode.ONE -> RepeatMode.OFF
                    }
                    playerViewModel.updateRepeatState(nextRepeat)
                }
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(24.dp)
            ) {
                Icon(
                    modifier = Modifier.size(24.dp),
                    tint = if (repeat != RepeatMode.OFF) {
                        Accent
                    } else {
                        TextPrimary
                    },
                    imageVector = if (repeat == RepeatMode.ONE) Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
                    contentDescription = when (repeat) {
                        RepeatMode.OFF -> "Repeat off"
                        RepeatMode.ALL -> "Repeat all"
                        RepeatMode.ONE -> "Repeat one"
                    }
                )
            }
            if (repeat != RepeatMode.OFF) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .background(Accent, shape = CircleShape)
                )
            } else {
                Spacer(modifier = Modifier.height(6.dp))
            }
        }
    }
}



@Composable
fun PlayerConnectRow(
    navController: NavController,
    context: Context,
    currentTrack: SongsModel?,
    onOpenDevices: () -> Unit = {},
    onOpenQueue: () -> Unit = {},
) {
    DisposableEffect(context) {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as android.media.AudioManager
        val callback = object : android.media.AudioDeviceCallback() {
            override fun onAudioDevicesAdded(addedDevices: Array<out android.media.AudioDeviceInfo>?) {
                com.music.spotui.ui.utils.AudioDeviceHelper.updateRouteName(context)
            }
            override fun onAudioDevicesRemoved(removedDevices: Array<out android.media.AudioDeviceInfo>?) {
                com.music.spotui.ui.utils.AudioDeviceHelper.updateRouteName(context)
            }
        }
        am.registerAudioDeviceCallback(callback, null)
        com.music.spotui.ui.utils.AudioDeviceHelper.updateRouteName(context)
        onDispose {
            am.unregisterAudioDeviceCallback(callback)
        }
    }

    val routeName by com.music.spotui.ui.utils.AudioDeviceHelper.currentRouteNameState
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 22.dp, end = 10.dp, top = 4.dp, bottom = 4.dp),
    ) {
        // Current audio output, as a Surface3 chip that opens the device picker.
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .clip(SoloShape.pill)
                .background(Surface3)
                .border(1.dp, com.music.spotui.ui.theme.Hairline, SoloShape.pill)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) { onOpenDevices() }
                .padding(horizontal = 12.dp, vertical = 7.dp)
        ) {
            Icon(
                imageVector = Icons.Rounded.Headphones,
                tint = Accent,
                modifier = Modifier.size(17.dp),
                contentDescription = "Audio output",
            )
            Text(
                text = routeName,
                color = Accent,
                style = MaterialTheme.typography.labelMedium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                modifier = Modifier
                    .padding(start = 6.dp)
                    .widthIn(max = 170.dp),
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Share,
                tint = TextPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable {
                        val link = currentTrack?.spotifyTrackId
                            ?.takeIf { it.isNotBlank() }
                            ?.let { "https://open.spotify.com/track/$it" }
                            ?: "${currentTrack?.title ?: ""} ${currentTrack?.singer ?: ""}".trim()
                        val send = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, link)
                        }
                        context.startActivity(Intent.createChooser(send, "Share"))
                    }
                    .padding(13.dp),
                contentDescription = "Share",
            )
            Icon(
                imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                tint = TextPrimary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable { onOpenQueue() }
                    .padding(12.dp),
                contentDescription = "Queue",
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun ArtistsSheet(
    artistNames: List<String>,
    artistIds: List<String> = emptyList(),
    context: Context,
    onDismiss: () -> Unit,
    navController: NavController,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val artistImages = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }
    val resolvedIds = remember { androidx.compose.runtime.mutableStateMapOf<String, String>() }

    LaunchedEffect(artistNames, artistIds) {
        artistNames.forEachIndexed { index, name ->
            val id = artistIds.getOrElse(index) { "" }
            if (id.isNotBlank()) {
                val cachedUrl = artistImageCache[id]
                if (!cachedUrl.isNullOrBlank()) {
                    artistImages[id] = cachedUrl
                } else {
                    if (com.music.spotui.data.api.SpotifyTokenProvider.ensureToken(context)) {
                        com.metrolist.spotify.Spotify.artist(id).getOrNull()?.let { artist ->
                            val url = artist.images.firstOrNull()?.url.orEmpty()
                            artistImageCache[id] = url
                            artistImages[id] = url
                        }
                    }
                }
            } else if (name.isNotBlank()) {
                val cachedId = artistIdCache[name]
                val cachedUrl = artistImageCache[cachedId ?: name]
                if (!cachedUrl.isNullOrBlank()) {
                    artistImages[name] = cachedUrl
                    if (!cachedId.isNullOrBlank()) resolvedIds[name] = cachedId
                } else {
                    var found = false
                    if (com.music.spotui.data.api.SpotifyTokenProvider.ensureToken(context)) {
                        com.metrolist.spotify.Spotify.search(name, types = listOf("artist"), limit = 1).getOrNull()
                            ?.artists?.items?.firstOrNull()?.let { artist ->
                                val url = artist.images.firstOrNull()?.url.orEmpty()
                                artistIdCache[name] = artist.id
                                artistImageCache[name] = url
                                artistImageCache[artist.id] = url
                                artistImages[name] = url
                                resolvedIds[name] = artist.id
                                found = url.isNotBlank()
                            }
                    }
                    // Login free fallback: with no Spotify token the lookup above never
                    // runs, so ask YouTube. A song search for the artist name returns an
                    // ArtistItem carrying a channel avatar; use its thumbnail so the sheet
                    // shows a real photo instead of the grey placeholder.
                    if (!found) {
                        runCatching {
                            com.metrolist.innertube.YouTube.search(
                                name,
                                com.metrolist.innertube.YouTube.SearchFilter.FILTER_SONG,
                            ).getOrNull()
                        }.getOrNull()?.let { result ->
                            val artistThumb = result.items
                                .filterIsInstance<com.metrolist.innertube.models.ArtistItem>()
                                .firstOrNull { !it.thumbnail.isNullOrBlank() }
                                ?.thumbnail
                            val songThumb = result.items
                                .filterIsInstance<com.metrolist.innertube.models.SongItem>()
                                .firstOrNull()
                                ?.thumbnail
                            val url = com.music.spotui.ui.viewmodel.hiResThumbnail(
                                artistThumb ?: songThumb
                            )
                            if (url.isNotBlank()) {
                                artistImageCache[name] = url
                                artistImages[name] = url
                            }
                        }
                    }
                }
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2,
        dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
        shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
        scrimColor = com.music.spotui.ui.theme.Scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Text(
                text = "Artists",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
            )
            artistNames.forEachIndexed { index, name ->
                val directId = artistIds.getOrElse(index) { "" }
                val effectiveId = directId.ifBlank { resolvedIds[name].orEmpty() }
                val imageUrl = if (directId.isNotBlank()) {
                    artistImages[directId].orEmpty()
                } else {
                    artistImages[name].orEmpty()
                }
                var following by remember(effectiveId) {
                    mutableStateOf(
                        effectiveId.isNotBlank() && com.music.spotui.data.preferences.isArtistFollowed(
                            context,
                            effectiveId
                        )
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                ) {
                    if (imageUrl.isNotBlank()) {
                        GlideImage(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape),
                            model = imageUrl,
                            contentScale = ContentScale.Crop,
                            contentDescription = name,
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(Surface3),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                tint = TextTertiary,
                                modifier = Modifier.size(24.dp),
                                contentDescription = name,
                            )
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = name,
                        color = TextPrimary,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .weight(1f)
                            .clickable {
                                onDismiss()
                                navController.navigate(
                                    com.music.spotui.ui.navigation.artistRoute(name, effectiveId)
                                )
                            },
                    )
                    if (effectiveId.isNotBlank()) {
                        Box(
                            modifier = Modifier
                                .heightIn(min = 36.dp)
                                .clip(SoloShape.pill)
                                .border(
                                    1.dp,
                                    if (following) Color.Transparent else TextTertiary,
                                    SoloShape.pill,
                                )
                                .background(
                                    if (following) Accent else Color.Transparent,
                                    SoloShape.pill,
                                )
                                .clickable {
                                    following = !following
                                    if (following) {
                                        com.music.spotui.data.preferences.addFollowedArtist(
                                            context,
                                            effectiveId,
                                            name
                                        )
                                    } else {
                                        com.music.spotui.data.preferences.removeFollowedArtist(
                                            context,
                                            effectiveId
                                        )
                                    }
                                    com.music.spotui.data.api.SpotifySync.setArtistFollowed(
                                        context,
                                        effectiveId,
                                        following
                                    )
                                }
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (following) "Following" else "Follow",
                                color = if (following) OnAccent else TextPrimary,
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalGlideComposeApi::class)
@Composable
fun PlayerOptionsSheet(
    navController: NavController,
    playerViewModel: PlayerViewModel,
    context: Context,
    isLiked: MutableState<Boolean>,
    onDismiss: () -> Unit,
    onOpenQueue: () -> Unit = {}
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showSleep by remember { mutableStateOf(false) }
    var showSpeed by remember { mutableStateOf(false) }
    var showSavedIn by remember { mutableStateOf(false) }
    var showAlternativeStream by remember { mutableStateOf(false) }
    var showYouTubeSearch by remember { mutableStateOf(false) }
    val altSearchViewModel: com.music.spotui.ui.viewmodel.AlternativeSearchViewModel =
        hiltViewModel()

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            kotlinx.coroutines.delay(250L)
        }
    }
    val sleepTimerEnd = SongPlayer.sleepTimerEndAt
    val remainingMillis = (sleepTimerEnd - currentTime).coerceAtLeast(0L)
    val remainingSeconds = remainingMillis / 1000L
    val minutesLeft = (remainingSeconds + 59) / 60

    val title by playerViewModel.currentSongTitle
    val singer by playerViewModel.currentSongSinger
    val cover by playerViewModel.currentSongCoverUri
    val album by playerViewModel.currentSongAlbum
    val songId by playerViewModel.currentSongId
    val currentQueue by playerViewModel.queue
    // The full track model (spotify id, real album, stream url), the state above
    // only carries display strings, and `album` is the *context* name (playlist…).
    val currentSong = currentQueue.firstOrNull { it.id == songId }
    var downloaded by remember(songId) {
        mutableStateOf(
            com.music.spotui.data.preferences.isDownloaded(
                context,
                songId.toString()
            )
        )
    }
    var downloadingNow by remember(songId) {
        mutableStateOf(
            currentSong != null && SongPlayer.isDownloading(
                currentSong.url
            )
        )
    }
    val alternativeKey = currentSong?.let { alternativeStreamKey(it) }.orEmpty()
    var currentAlternative by remember(songId, alternativeKey) {
        mutableStateOf(alternativeKey.takeIf { it.isNotBlank() }
            ?.let { getAlternativeStream(context, it) })
    }
    val localFileLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            val song = currentSong ?: return@rememberLauncherForActivityResult
            val picked = uri ?: return@rememberLauncherForActivityResult
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    picked,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
            }
            setLocalAlternativeStream(
                context,
                alternativeKey,
                picked,
                picked.lastPathSegment.orEmpty()
            )
            currentAlternative = getAlternativeStream(context, alternativeKey)
            Toast.makeText(context, "Alternative stream set to local file", Toast.LENGTH_SHORT).show()
            SongPlayer.invalidateSongCache(song, context, reloadIfPlaying = true, clearAltStream = false)
        }

    if (showSavedIn && currentSong != null) {
        com.music.spotui.ui.components.SavedInSheet(
            song = currentSong,
            context = context,
            onDismiss = { showSavedIn = false; onDismiss() },
            onLikedChanged = { isLiked.value = it },
        )
        return
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Surface2,
        dragHandle = { com.music.spotui.ui.components.SoloDragHandle() },
        shape = com.music.spotui.ui.theme.SoloShape.sheetTop,
        scrimColor = com.music.spotui.ui.theme.Scrim,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            if (showYouTubeSearch) {
                YouTubeSearchView(
                    viewModel = altSearchViewModel,
                    songTitle = title,
                    songArtist = singer,
                    enabled = currentSong != null,
                    onBack = { showYouTubeSearch = false },
                    onUseVideoId = { videoId ->
                        val song = currentSong ?: return@YouTubeSearchView
                        setYouTubeAlternativeStream(context, alternativeKey, videoId)
                        currentAlternative = getAlternativeStream(context, alternativeKey)
                        showYouTubeSearch = false
                        showAlternativeStream = false
                        Toast.makeText(
                            context,
                            "Alternative stream set to YouTube",
                            Toast.LENGTH_SHORT
                        ).show()
                        SongPlayer.invalidateSongCache(song, context, reloadIfPlaying = true, clearAltStream = false)
                        onDismiss()
                    },
                )
            } else if (showAlternativeStream) {
                AlternativeStreamEditor(
                    currentAlternative = currentAlternative,
                    enabled = currentSong != null,
                    onBack = { showAlternativeStream = false },
                    onUseYouTube = { text ->
                        val song = currentSong ?: return@AlternativeStreamEditor
                        val videoId = SongPlayer.videoIdFromYouTubeLink(text)
                        if (videoId == null) {
                            Toast.makeText(
                                context,
                                "Paste a YouTube video link or video ID",
                                Toast.LENGTH_SHORT
                            ).show()
                        } else {
                            setYouTubeAlternativeStream(context, alternativeKey, videoId)
                            currentAlternative = getAlternativeStream(context, alternativeKey)
                            Toast.makeText(
                                context,
                                "Alternative stream set to YouTube",
                                Toast.LENGTH_SHORT
                            ).show()
                            SongPlayer.invalidateSongCache(song, context, reloadIfPlaying = true, clearAltStream = false)
                        }
                    },
                    onPickLocal = {
                        localFileLauncher.launch(arrayOf("audio/*"))
                    },
                    onClear = {
                        val song = currentSong ?: return@AlternativeStreamEditor
                        clearAlternativeStream(context, alternativeKey)
                        currentAlternative = null
                        Toast.makeText(context, "Alternative stream cleared", Toast.LENGTH_SHORT).show()
                        SongPlayer.invalidateSongCache(song, context, reloadIfPlaying = true, clearAltStream = true)
                    },
                    onOpenYouTubeSearch = { showYouTubeSearch = true },
                )
            } else if (showSpeed) {
                PlaybackSpeedPanel(context = context, onBack = { showSpeed = false })
            } else if (!showSleep) {
                // ── Now-playing header ──
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    GlideImage(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(SoloShape.sm),
                        model = cover,
                        contentScale = ContentScale.Crop,
                        contentDescription = ""
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            title,
                            color = TextPrimary,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                        Text(
                            singer,
                            color = TextTertiary,
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(color = com.music.spotui.ui.theme.Hairline)

                PlayerMenuRow(
                    icon = Icons.Default.Share,
                    label = "Share"
                ) {
                    val shareText = currentSong?.spotifyTrackId?.takeIf { it.isNotBlank() }
                        ?.let { "https://open.spotify.com/track/$it" }
                        ?: "Listening to $title by $singer"
                    val send = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, shareText)
                    }
                    context.startActivity(Intent.createChooser(send, "Share"))
                    onDismiss()
                }
                PlayerMenuRow(
                    icon = if (downloaded) Icons.Default.CheckCircle else androidx.compose.material.icons.Icons.Rounded.Download,
                    iconTint = if (downloaded) Accent else TextPrimary,
                    label = when {
                        downloaded -> "Remove download"
                        downloadingNow -> "Downloading…"
                        else -> "Download"
                    },
                    enabled = currentSong != null && !downloadingNow,
                ) {
                    val song = currentSong ?: return@PlayerMenuRow
                    if (downloaded) {
                        com.music.spotui.data.preferences.removeDownload(
                            context,
                            song.id.toString()
                        )
                        downloaded = false
                    } else {
                        downloadingNow = true
                        SongPlayer.downloadSong(song, context) { ok ->
                            downloadingNow = false
                            downloaded = ok
                        }
                    }
                }
                PlayerMenuRow(
                    icon = Icons.Default.PlayArrow,
                    iconTint = if (currentAlternative != null) Accent else TextPrimary,
                    label = if (currentAlternative == null) "Alternative stream" else "Alternative stream set",
                    enabled = currentSong != null,
                    trailingArrow = true,
                ) {
                    showAlternativeStream = true
                }
                PlayerMenuRow(
                    icon = Icons.Default.Refresh,
                    label = "Invalidate cache",
                    enabled = currentSong != null,
                ) {
                    val song = currentSong ?: return@PlayerMenuRow
                    onDismiss()
                    SongPlayer.invalidateSongCache(song, context, reloadIfPlaying = true)
                }
                PlayerMenuRow(
                    icon = if (isLiked.value) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    iconTint = if (isLiked.value) Accent else TextPrimary,
                    label = if (isLiked.value) "Remove from Liked Songs" else "Add to Liked Songs"
                ) {
                    if (isLiked.value) {
                        removeLikedSongId(context, songId.toString())
                    } else {
                        // Save the whole track when we have it, so Liked Songs can show
                        // it without any account behind the scenes.
                        val song = currentSong
                        if (song != null) {
                            com.music.spotui.data.preferences.addLikedSong(context, song)
                        } else {
                            addLikedSongId(context, songId.toString())
                        }
                    }
                    isLiked.value = isSongLiked(context, songId.toString())
                    // Mirror the like to the real Spotify account.
                    com.music.spotui.data.api.SpotifySync.setTrackSaved(
                        context, currentSong?.spotifyTrackId.orEmpty(), isLiked.value
                    )
                    onDismiss()
                }
                PlayerMenuRow(
                    icon = Icons.Default.Add,
                    label = "Add to playlist",
                    enabled = currentSong != null,
                    trailingArrow = true,
                ) {
                    showSavedIn = true
                }
                PlayerMenuRow(
                    icon = Icons.AutoMirrored.Filled.List,
                    label = "View queue"
                ) {
                    onDismiss()
                    onOpenQueue()
                }
                // Use the track's REAL album (currentSongAlbum is the playing
                // context, a playlist name would resolve to garbage).
                val realAlbum = currentSong?.album?.ifBlank { null } ?: album
                PlayerMenuRow(
                    icon = Icons.Default.PlayArrow,
                    label = "Go to album",
                    enabled = realAlbum.isNotBlank()
                ) {
                    onDismiss()
                    navController.navigate(albumRoute(realAlbum, singer))
                }
                PlayerMenuRow(
                    icon = Icons.Default.Person,
                    label = "Go to artist",
                    enabled = singer.isNotBlank()
                ) {
                    onDismiss()
                    playerViewModel.goToArtist(
                        currentSong?.spotifyTrackId.orEmpty(),
                        singer
                    ) { route ->
                        navController.navigate(route)
                    }
                }
                PlayerMenuRow(
                    icon = Icons.Default.Notifications,
                    label = "Sleep timer",
                    subtitle = if (minutesLeft > 0) "$minutesLeft min left" else null,
                    trailingArrow = true
                ) { showSleep = true }
                val speedNow = com.music.spotui.data.preferences.getPlaybackSpeed(context)
                PlayerMenuRow(
                    icon = androidx.compose.material.icons.Icons.Rounded.Speed,
                    label = "Playback speed",
                    subtitle = if (speedNow != 1f) "${formatSpeed(speedNow)}×" else null,
                    trailingArrow = true,
                ) { showSpeed = true }
                PlayerMenuRow(
                    icon = androidx.compose.material.icons.Icons.Rounded.GraphicEq,
                    label = "Equalizer",
                    trailingArrow = true,
                ) {
                    onDismiss()
                    navController.navigate(com.music.spotui.ui.navigation.Routes.Equalizer.route)
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    com.music.spotui.ui.components.SoloIconButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = { showSleep = false },
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "Sleep timer",
                        color = TextPrimary,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    if (remainingSeconds > 0) {
                        val minutes = remainingSeconds / 60
                        val seconds = remainingSeconds % 60
                        Text(
                            text = String.format("%02d:%02d", minutes, seconds),
                            color = Accent,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(end = 16.dp)
                        )
                    }
                }
                androidx.compose.material3.HorizontalDivider(color = com.music.spotui.ui.theme.Hairline)
                val options = listOf(
                    "Off" to 0L,
                    "5 minutes" to 5L,
                    "15 minutes" to 15L,
                    "30 minutes" to 30L,
                    "45 minutes" to 45L,
                    "1 hour" to 60L
                )
                options.forEach { (label, minutesOption) ->
                    PlayerMenuRow(icon = Icons.Default.Notifications, label = label) {
                        SongPlayer.setSleepTimer(minutesOption * 60_000L)
                        onDismiss()
                    }
                }
            }
        }
    }
}

private fun formatSpeed(v: Float): String =
    if (v == v.toInt().toFloat()) v.toInt().toString() else "%.2f".format(v).trimEnd('0').trimEnd('.')

/** Speed chips, a pitch slider and Reset, shown inside the player options sheet. */
@Composable
private fun PlaybackSpeedPanel(context: Context, onBack: () -> Unit) {
    var speed by remember { mutableStateOf(com.music.spotui.data.preferences.getPlaybackSpeed(context)) }
    var pitch by remember { mutableStateOf(com.music.spotui.data.preferences.getPlaybackPitch(context)) }
    fun apply() = SongPlayer.setPlaybackSpeedPitch(context, speed, pitch)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        com.music.spotui.ui.components.SoloIconButton(
            icon = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            onClick = onBack,
        )
        Text(
            "Playback speed",
            color = TextPrimary,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.weight(1f),
        )
        Text(
            "${formatSpeed(speed)}×",
            color = Accent,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(end = 16.dp),
        )
    }
    androidx.compose.material3.HorizontalDivider(color = com.music.spotui.ui.theme.Hairline)
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 16.dp),
    ) {
        listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 2f).forEach { option ->
            com.music.spotui.ui.components.SoloChip(
                label = "${formatSpeed(option)}×",
                selected = speed == option,
                onClick = { speed = option; apply() },
                modifier = Modifier.heightIn(min = 48.dp),
            )
        }
    }
    Text(
        "Pitch  ${"%.2f".format(pitch)}",
        color = TextSecondary,
        style = MaterialTheme.typography.bodyMedium,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
    androidx.compose.material3.Slider(
        value = pitch,
        onValueChange = { pitch = (it * 20).roundToInt() / 20f },
        onValueChangeFinished = { apply() },
        valueRange = 0.5f..1.5f,
        colors = androidx.compose.material3.SliderDefaults.colors(
            thumbColor = Accent,
            activeTrackColor = Accent,
            inactiveTrackColor = com.music.spotui.ui.theme.Surface4,
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .semantics { contentDescription = "Pitch" },
    )
    Row(
        horizontalArrangement = Arrangement.End,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        com.music.spotui.ui.components.SoloPillButton(
            text = "Reset",
            primary = false,
            onClick = { speed = 1f; pitch = 1f; apply() },
        )
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun AlternativeStreamEditor(
    currentAlternative: com.music.spotui.data.preferences.AlternativeStream?,
    enabled: Boolean,
    onBack: () -> Unit,
    onUseYouTube: (String) -> Unit,
    onPickLocal: () -> Unit,
    onClear: () -> Unit,
    onOpenYouTubeSearch: () -> Unit,
) {
    var youtubeText by remember { mutableStateOf("") }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            com.music.spotui.ui.components.SoloIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Back",
                onClick = onBack,
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Alternative stream",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(14.dp))
        if (currentAlternative != null && currentAlternative.isYouTube) {
            val context = LocalContext.current
            val videoId = currentAlternative.value
            val thumbnailUrl = "https://img.youtube.com/vi/$videoId/mqdefault.jpg"
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(SoloShape.sm)
                    .background(Surface3)
                    .clickable {
                        val intent = Intent(
                            Intent.ACTION_VIEW,
                            Uri.parse("https://www.youtube.com/watch?v=$videoId")
                        )
                        context.startActivity(intent)
                    }
                    .padding(10.dp),
            ) {
                GlideImage(
                    model = thumbnailUrl,
                    modifier = Modifier
                        .size(64.dp)
                        .clip(SoloShape.xs),
                    contentScale = ContentScale.Crop,
                    contentDescription = null,
                )
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "YouTube alternative",
                        color = Accent,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Medium,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        text = videoId,
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                    tint = TextSecondary,
                    modifier = Modifier.size(18.dp),
                    contentDescription = "Open on YouTube",
                )
            }
        } else if (currentAlternative != null && currentAlternative.isLocal) {
            Text(
                text = "Current: local file ${currentAlternative.label.ifBlank { currentAlternative.value }}",
                color = Accent,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 2,
            )
        } else {
            Text(
                text = "No alternative stream set",
                color = TextSecondary,
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = youtubeText,
            onValueChange = { youtubeText = it },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            label = { Text("YouTube link or video ID", color = TextSecondary) },
            shape = SoloShape.md,
            colors = soloFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (enabled && youtubeText.isNotBlank()) {
                com.music.spotui.ui.components.SoloPillButton(
                    text = "Use YouTube",
                    onClick = { onUseYouTube(youtubeText) },
                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                )
            } else {
                Text(
                    "Use YouTube",
                    color = TextTertiary,
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 21.dp),
                )
            }
        }
        PlayerMenuRow(
            icon = Icons.Default.Search,
            label = "Search YouTube",
            enabled = enabled,
            trailingArrow = true,
        ) {
            onOpenYouTubeSearch()
        }
        PlayerMenuRow(
            icon = Icons.Default.Add,
            label = "Use local audio file",
            enabled = enabled,
        ) {
            onPickLocal()
        }
        PlayerMenuRow(
            icon = Icons.Default.CheckCircle,
            label = "Clear alternative stream",
            enabled = enabled && currentAlternative != null,
            iconTint = com.music.spotui.ui.theme.Danger,
        ) {
            onClear()
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
fun YouTubeSearchView(
    viewModel: com.music.spotui.ui.viewmodel.AlternativeSearchViewModel,
    songTitle: String,
    songArtist: String,
    enabled: Boolean,
    onBack: () -> Unit,
    onUseVideoId: (String) -> Unit,
) {
    LaunchedEffect(songTitle, songArtist) {
        viewModel.initQuery(songTitle, songArtist)
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopPreview() }
    }

    BackHandler { viewModel.stopPreview(); onBack() }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 8.dp, end = 20.dp, top = 8.dp, bottom = 16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            com.music.spotui.ui.components.SoloIconButton(
                icon = Icons.Default.KeyboardArrowDown,
                contentDescription = "Back",
                onClick = { viewModel.stopPreview(); onBack() },
            )
            Spacer(Modifier.width(4.dp))
            Text(
                "Search YouTube",
                color = TextPrimary,
                style = MaterialTheme.typography.titleLarge,
            )
        }
        Spacer(Modifier.height(14.dp))
        OutlinedTextField(
            value = viewModel.searchQuery,
            onValueChange = { viewModel.updateQuery(it) },
            enabled = enabled,
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyMedium.copy(color = TextPrimary),
            label = { Text("Search query", color = TextSecondary) },
            shape = SoloShape.md,
            colors = soloFieldColors(),
            modifier = Modifier.fillMaxWidth().padding(start = 12.dp),
        )
        Spacer(Modifier.height(12.dp))

        when {
            viewModel.isSearching -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        color = Accent,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }

            // Snapshot the error into a local first. It is observable state a background
            // resolution can clear, so testing it and then dereferencing with !! raced that
            // clear and could crash with an NPE right as the error resolved itself.
            viewModel.error != null -> {
                val errorText = viewModel.error
                if (errorText != null) {
                    Text(
                        text = errorText,
                        color = com.music.spotui.ui.theme.Danger,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }

            viewModel.searchResults.isEmpty() && viewModel.searchQuery.isNotBlank() -> {
                Text(
                    text = "No results found",
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            else -> {
                androidx.compose.foundation.lazy.LazyColumn(
                    modifier = Modifier.heightIn(max = 360.dp),
                ) {
                    items(
                        count = viewModel.searchResults.size,
                        key = { viewModel.searchResults[it].id },
                    ) { index ->
                        val song = viewModel.searchResults[index]
                        YouTubeSearchResultRow(
                            song = song,
                            isPreviewing = viewModel.previewingVideoId == song.id,
                            isResolving = viewModel.previewingVideoId == song.id && viewModel.isResolvingPreview,
                            enabled = enabled,
                            onPreview = { viewModel.preview(song) },
                            onSelect = {
                                viewModel.stopPreview()
                                onUseVideoId(song.id)
                            },
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalGlideComposeApi::class)
@Composable
private fun YouTubeSearchResultRow(
    song: com.metrolist.innertube.models.SongItem,
    isPreviewing: Boolean,
    isResolving: Boolean = false,
    enabled: Boolean,
    onPreview: () -> Unit,
    onSelect: () -> Unit,
) {
    val durationText = song.duration?.let { dur ->
        val min = dur / 60
        val sec = dur % 60
        "%d:%02d".format(min, sec)
    }.orEmpty()

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
    ) {
        GlideImage(
            model = song.thumbnail,
            modifier = Modifier
                .size(44.dp)
                .clip(SoloShape.sm),
            contentScale = ContentScale.Crop,
            contentDescription = null,
        )
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                color = TextPrimary,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = song.artists.joinToString(", ") { it.name },
                    color = TextSecondary,
                    style = MaterialTheme.typography.bodySmall,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (durationText.isNotBlank()) {
                    Text(
                        text = " · $durationText",
                        color = TextSecondary,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
        Spacer(Modifier.width(4.dp))
        if (isResolving) {
            androidx.compose.material3.CircularProgressIndicator(
                color = Accent,
                modifier = Modifier
                    .size(48.dp)
                    .padding(13.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Icon(
                imageVector = if (isPreviewing) Icons.Default.Pause else Icons.Default.PlayArrow,
                tint = if (isPreviewing) Accent else TextSecondary,
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled) { onPreview() }
                    .padding(12.dp),
                contentDescription = "Preview",
            )
        }
        Spacer(Modifier.width(2.dp))
        Icon(
            imageVector = Icons.Default.Check,
            tint = TextSecondary,
            modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .clickable(enabled = enabled) { onSelect() }
                .padding(12.dp),
            contentDescription = "Use this",
        )
    }
}

@Composable
fun PlayerMenuRow(
    icon: ImageVector,
    label: String,
    subtitle: String? = null,
    iconTint: Color = TextPrimary,
    enabled: Boolean = true,
    trailingArrow: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 52.dp)
            .clickable(enabled = enabled) { onClick() }
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Icon(
            imageVector = icon,
            tint = if (enabled) iconTint else TextTertiary,
            modifier = Modifier.size(22.dp),
            contentDescription = null
        )
        Spacer(modifier = Modifier.width(20.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                color = if (enabled) TextPrimary else TextTertiary,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    color = TextTertiary,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                )
            }
        }
        if (trailingArrow) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                tint = TextTertiary,
                modifier = Modifier.size(20.dp),
                contentDescription = null
            )
        }
    }
}

/**
 * Plays a Spotify Canvas clip: a short, muted, looping video filling the
 * now-playing background. Uses a dedicated ExoPlayer (separate from the audio
 * engine) released when the composable leaves. Falls back to nothing if the URL fails.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun CanvasVideo(url: String, modifier: Modifier = Modifier, onError: (() -> Unit)? = null) {
    val context = LocalContext.current
    val exo = remember(url) {
        androidx.media3.exoplayer.ExoPlayer.Builder(context).build().apply {
            setMediaItem(androidx.media3.common.MediaItem.fromUri(url))
            repeatMode = androidx.media3.common.Player.REPEAT_MODE_ALL
            volume = 0f
            playWhenReady = true
            prepare()
        }
    }
    androidx.compose.runtime.DisposableEffect(url) {
        val listener = object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                onError?.invoke()
            }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener); exo.release() }
    }
    androidx.compose.ui.viewinterop.AndroidView(
        modifier = modifier,
        factory = { ctx ->
            androidx.media3.ui.PlayerView(ctx).apply {
                player = exo
                // Strip ALL chrome: no controller, no buffering spinner, and no
                // artwork/placeholder icon (that "play icon" overlay), just video.
                useController = false
                controllerAutoShow = false
                setShowBuffering(androidx.media3.ui.PlayerView.SHOW_BUFFERING_NEVER)
                setArtworkDisplayMode(androidx.media3.ui.PlayerView.ARTWORK_DISPLAY_MODE_OFF)
                setDefaultArtwork(null)
                hideController()
                resizeMode = androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                setBackgroundColor(android.graphics.Color.TRANSPARENT)
            }
        },
    )
}

/** Outlined field colours for sheets: Surface3 fill, hairline edge, accent when focused. */
@Composable
private fun soloFieldColors() = androidx.compose.material3.OutlinedTextFieldDefaults.colors(
    focusedBorderColor = Accent,
    unfocusedBorderColor = com.music.spotui.ui.theme.Hairline,
    focusedContainerColor = Surface3,
    unfocusedContainerColor = Surface3,
    disabledContainerColor = Surface3,
    cursorColor = Accent,
    focusedLabelColor = Accent,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
)
