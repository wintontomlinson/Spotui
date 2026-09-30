package com.music.spotui.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
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
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.unit.sp
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
import com.music.spotui.ui.theme.Gold
import com.music.spotui.ui.theme.Ink
import com.music.spotui.ui.theme.TextTertiary
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
                label = "navBarCompression",
            )
            // As the user scrolls down, the floating bar shrinks toward the bottom,
            // shrinks a little and stays fully visible so the tabs are always
            // reachable — it never fades away. Springs back to full size on scroll up.
            val barScale = 1f - 0.12f * compression
            val barAlpha = 1f - 0.12f * compression
            val barTranslateY = 8f * compression

            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    // A soft ink scrim so content fades out beneath the floating bar
                    // instead of being cut off by a hard edge.
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Ink.copy(alpha = 0.55f),
                                Ink.copy(alpha = 0.92f),
                            ),
                            startY = 0f
                        )
                    )
            ) {

                Column(
                    modifier = Modifier.navigationBarsPadding()
                ) {

                    AnimatedVisibility(
                        visible = bottomBarPlayerState.value,
                        enter = slideInVertically(initialOffsetY = { it }),
                        exit = slideOutVertically(targetOffsetY = { it }),
                        content = {
                            MiniPlayer(navController)
                        }
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    NavigationBar(
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 8.dp)
                            .fillMaxWidth()
                            // Compress on scroll: shrink, fade and slide down as the
                            // user scrolls into the content, spring back on scroll up.
                            .graphicsLayer {
                                scaleX = barScale
                                scaleY = barScale
                                translationY = barTranslateY
                                alpha = barAlpha
                            }
                            // Floating glass pill: a lifted velvet surface with a gold
                            // light catching its top edge.
                            .shadow(
                                elevation = 24.dp,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black,
                            )
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xF21B1529),
                                        Color(0xF2141020),
                                    ),
                                )
                            )
                            .border(
                                width = 1.dp,
                                brush = Brush.verticalGradient(
                                    listOf(Gold.copy(alpha = 0.30f), Gold.copy(alpha = 0.05f))
                                ),
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                            ),
                        containerColor = Color.Transparent,
                        windowInsets = androidx.compose.foundation.layout.WindowInsets(0, 0, 0, 0)
                    ) {
                        val navStack by navController.currentBackStackEntryAsState()
                        // Some destinations carry optional arguments, for example
                        // "ytsearch?q={q}". Compare on the base route so the correct
                        // tab stays highlighted regardless of arguments.
                        val currentRoute = navStack?.destination?.route?.substringBefore("?")

                        val rootRoutes = listOf(Routes.Home.route, Routes.YtSearch.route, Routes.Library.route)
                        // Tracks which tab to highlight. On destinations that are not
                        // tabs, such as the player or a playlist, the last tab stays lit.
                        // This is presentation only: taps are decided from currentRoute.
                        var currentTab by remember { mutableStateOf(Routes.Home.route) }
                        if (currentRoute in rootRoutes) {
                            currentTab = currentRoute!!
                        }

                        navItems.forEach { item ->
                            NavigationBarItem(
                                selected = currentTab == item.route,
                                icon = {
                                    Icon(
                                        imageVector = navIcon(item, selected = currentTab == item.route),
                                        contentDescription = item.label,
                                    )
                                },
                                label = {
                                    Text(
                                        text = item.label,
                                        color = if (currentTab == item.route) Gold else TextTertiary,
                                        fontSize = 11.sp,
                                        fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                    )
                                },
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
                                alwaysShowLabel = true,
                                interactionSource = NoRippleInteractionSource(),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Gold,
                                    unselectedIconColor = TextTertiary,
                                    // A soft gold pill behind the active tab's icon.
                                    indicatorColor = Gold.copy(alpha = 0.14f),
                                )
                            )

                        }
                    }



                }



            }
        }
    )
}

/** Filled glyph for the active tab, outlined for the rest. */
private fun navIcon(item: Routes, selected: Boolean): ImageVector = when (item) {
    Routes.YtSearch -> if (selected) Icons.Rounded.Explore else Icons.Outlined.Explore
    Routes.Library -> if (selected) Icons.Rounded.LibraryMusic else Icons.Outlined.LibraryMusic
    else -> if (selected) Icons.Rounded.Home else Icons.Outlined.Home
}
