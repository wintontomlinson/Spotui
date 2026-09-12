package com.music.spotui.ui.navigation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import com.music.spotui.ui.components.MiniPlayer
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
            Box(
                contentAlignment = Alignment.BottomCenter,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        // Light fade behind the floating bar so content shows THROUGH
                        // (the bar itself is translucent) — a glassy, transparent look.
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color(0xFF000000).copy(alpha = 0.25f),
                                Color(0xFF000000).copy(alpha = 0.55f)
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

                    Spacer(modifier = Modifier.height(10.dp))

                    // Compress the bar while the user scrolls down through content:
                    // it shrinks and drops its labels, then expands again on scroll up.
                    val compressed by NavBarScrollState.compressed
                    val barHeight by androidx.compose.animation.core.animateDpAsState(
                        targetValue = if (compressed) 52.dp else 74.dp,
                        animationSpec = androidx.compose.animation.core.tween(220),
                        label = "navBarHeight",
                    )
                    val barAlpha by androidx.compose.animation.core.animateFloatAsState(
                        targetValue = if (compressed) 0.42f else 0.62f,
                        animationSpec = androidx.compose.animation.core.tween(220),
                        label = "navBarAlpha",
                    )

                    NavigationBar(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = if (compressed) 3.dp else 6.dp)
                            .fillMaxWidth()
                            .height(barHeight)
                            // A floating, rounded nav bar that reads as a raised control
                            // surface over the content, rather than icons sitting loose on
                            // the gradient.
                            .shadow(
                                elevation = if (compressed) 10.dp else 20.dp,
                                shape = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
                                clip = false,
                                ambientColor = Color.Black,
                                spotColor = Color.Black,
                            )
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(26.dp))
                            // Transparent, glassy nav bar — a translucent black tint so
                            // content shows through (even more see-through when
                            // compressed), with a soft gold hairline.
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF141414).copy(alpha = barAlpha - 0.10f),
                                        Color(0xFF000000).copy(alpha = barAlpha),
                                    ),
                                )
                            )
                            .border(
                                width = 1.dp,
                                color = Color(0xFFE8C15A).copy(alpha = 0.30f),
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
                                        painter = painterResource(
                                            id = item.icon
                                        ), contentDescription = "home"
                                    )
                                },
                                label = if (compressed) null else {
                                    {
                                        if (currentTab == item.route) {
                                            Text(
                                                color = Color(0xFFE8C15A),
                                                text = item.label,
                                                fontSize = 11.sp,
                                                fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                                            )
                                        } else {
                                            Text(
                                                color = Color(0xFFBFB49A),
                                                text = item.label,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
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
                                alwaysShowLabel = false,
                                interactionSource = NoRippleInteractionSource(),
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = Color(0xFFE8C15A),
                                    unselectedIconColor = Color(0xFFBFB49A),
                                    // A gold pill behind the active tab's icon so the
                                    // selection reads clearly on the royal-purple bar.
                                    indicatorColor = Color(0xFFE8C15A).copy(alpha = 0.22f),
                                )
                            )

                        }
                    }



                }



            }
        }
    )
}
