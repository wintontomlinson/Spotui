package com.music.spotui.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import com.music.spotui.ui.theme.SoloShape
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.spotui.ui.components.MiniPlayer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LibraryMusic
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.ui.graphics.vector.ImageVector
import com.music.spotui.ui.theme.Accent
import com.music.spotui.ui.theme.Canvas
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import com.music.spotui.ui.theme.SoloMotion
import com.music.spotui.ui.components.soloClickable
import com.music.spotui.ui.theme.GlassFill
import com.music.spotui.ui.theme.GlassFillStrong
import com.music.spotui.ui.theme.Hairline
import com.music.spotui.ui.theme.TextSecondary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class NoRippleInteractionSource : MutableInteractionSource {

    override val interactions: Flow<Interaction> = emptyFlow()

    override suspend fun emit(interaction: Interaction) {}

    override fun tryEmit(interaction: Interaction) = true
}

@Composable
fun MainBottomNavigation(navController: NavHostController, bottomBarState: MutableState<Boolean>, bottomBarPlayerState : MutableState<Boolean>, onSearchReselected: () -> Unit = {}) {

    val navItems = listOf(
        Routes.Home,
        Routes.YtSearch,
        Routes.Library
    )
    AnimatedVisibility(
        visible = bottomBarState.value,
        enter = slideInVertically(initialOffsetY = { it }),
        exit = slideOutVertically(targetOffsetY = { it }),
        content = {
            // How compressed the bar should be, driven by content scrolling.
            // 0f = fully expanded (idle / at top), 1f = compressed (scrolling down).
            val compression by animateFloatAsState(
                targetValue = NavBarScrollState.compression,
                animationSpec = tween(SoloMotion.NAV_MS),
                label = "navBarCompression",
            )
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    // A soft canvas scrim so content fades out behind the mini player
                    // instead of being cut off by a hard edge.
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Canvas.copy(alpha = 0.55f),
                                Canvas.copy(alpha = 0.92f),
                            ),
                            startY = 0f
                        )
                    )
            ) {

                Column {

                    AnimatedVisibility(
                        visible = bottomBarPlayerState.value,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        content = {
                            MiniPlayer(navController)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    val navStack by navController.currentBackStackEntryAsState()
                    // Some destinations carry optional arguments, for example "ytsearch?q={q}".
                    // Compare on the base route so the correct tab stays highlighted.
                    val currentRoute = navStack?.destination?.route?.substringBefore("?")
                    val rootRoutes = listOf(Routes.Home.route, Routes.YtSearch.route, Routes.Library.route)
                    // Tracks which tab to highlight. On destinations that are not tabs (player,
                    // playlist) the last tab stays lit. Taps are decided from currentRoute.
                    var currentTab by remember { mutableStateOf(Routes.Home.route) }
                    val route = currentRoute
                    if (route != null && route in rootRoutes) currentTab = route

                    // Docked full-width glass bar with a 1dp hairline top edge: 64dp,
                    // compressing to 56dp (labels fade) while content scrolls down.
                    val glass = if (Build.VERSION.SDK_INT >= 31) GlassFill else GlassFillStrong
                    androidx.compose.foundation.layout.BoxWithConstraints(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(glass)
                            .drawBehind {
                                drawLine(
                                    color = Hairline,
                                    start = Offset(0f, 0f),
                                    end = Offset(size.width, 0f),
                                    strokeWidth = 1.dp.toPx(),
                                )
                            }
                            .navigationBarsPadding()
                            .height(64.dp - 8.dp * compression),
                    ) {
                        // 3x22dp accent indicator that slides above the selected icon.
                        val tabWidth = maxWidth / navItems.size
                        val selectedIndex = navItems.indexOfFirst { it.route == currentTab }.coerceAtLeast(0)
                        val indicatorWidth = 22.dp
                        val indicatorOffset by androidx.compose.animation.core.animateDpAsState(
                            targetValue = tabWidth * selectedIndex + (tabWidth - indicatorWidth) / 2,
                            animationSpec = SoloMotion.spring(),
                            label = "navIndicator",
                        )
                        Box(
                            modifier = Modifier
                                .offset(x = indicatorOffset)
                                .size(width = indicatorWidth, height = 3.dp)
                                .clip(SoloShape.pill)
                                .background(Accent),
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize(),
                        ) {
                        navItems.forEach { item ->
                            val selected = currentTab == item.route
                            NavTab(
                                item = item,
                                selected = selected,
                                labelAlpha = 1f - compression,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                // Decide from the LIVE route, never from the
                                // remembered tab. The remembered value can drift out
                                // of sync, and when it did the old code fell through
                                // to popBackStack for a destination that was no
                                // longer on the stack, which silently did nothing and
                                // left the tab unresponsive.
                                if (currentRoute != item.route) {
                                    val startRoute = navController.graph.startDestinationRoute
                                    if (startRoute != null && item.route == startRoute) {
                                        // Home IS the graph's start destination, and that
                                        // makes the usual tab recipe cancel itself out.
                                        // NavController runs popUpTo first, and a pop with
                                        // saveState records the saved stack under the
                                        // popUpTo destination's own id. Navigating to that
                                        // same destination with restoreState then finds
                                        // that fresh entry and restores the stack it just
                                        // popped, so the screen never actually changed and
                                        // the only way back to Home was the back button.
                                        // Popping straight to Home avoids the round trip,
                                        // and saveState still keeps the other tabs' state
                                        // for when they are reselected.
                                        val popped = navController.popBackStack(
                                            route = startRoute,
                                            inclusive = false,
                                            saveState = true,
                                        )
                                        if (!popped) {
                                            // A deep link can leave Home off the stack
                                            // entirely, in which case there is nothing to
                                            // pop back to and it has to be pushed.
                                            navController.navigate(startRoute) {
                                                launchSingleTop = true
                                            }
                                        }
                                    } else {
                                        navController.navigate(item.route) {
                                            popUpTo(startRoute ?: item.route) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                                },
                            )
                        }
                        }
                    }
                }
            }
        }
    )
}

@Composable
private fun NavTab(
    item: Routes,
    selected: Boolean,
    labelAlpha: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val tint by animateColorAsState(
        targetValue = if (selected) Accent else TextSecondary,
        animationSpec = tween(SoloMotion.NAV_MS),
        label = "navTint",
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxHeight()
            .heightIn(min = 48.dp)
            .soloClickable(onClick = onClick)
            .semantics { this.selected = selected; role = Role.Tab },
    ) {
        Icon(
            imageVector = navIcon(item, selected),
            contentDescription = item.label,
            tint = tint,
            modifier = Modifier.size(24.dp),
        )
        if (labelAlpha > 0.05f) {
            Text(
                text = item.label,
                color = tint,
                style = MaterialTheme.typography.labelSmall,
                maxLines = 1,
                modifier = Modifier
                    .padding(top = 2.dp)
                    .graphicsLayer { alpha = labelAlpha },
            )
        }
    }
}

/** Filled glyph for the active tab, outlined for the rest. */
private fun navIcon(item: Routes, selected: Boolean): ImageVector = when (item) {
    Routes.YtSearch -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
    Routes.Library -> if (selected) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
    else -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
}
