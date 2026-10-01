package com.music.spotui.ui.theme

/**
 * App-wide Compose theme entry point.
 *
 * Maps the [Color] tokens into a single, always-dark Material 3 colour scheme (dynamic colour is
 * intentionally off) and wires in the SOLO typography, shapes and motion. Wrap the app content in
 * this theme so every screen shares one palette and type scale.
 */

import android.app.Activity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.TextUnit
import androidx.core.view.WindowCompat

// One deliberate dark scheme, so every Material component (switches, sliders, sheets,
// dialogs, menus) picks up the Graphite & Azure palette instead of a stray default or wallpaper tint.
private val SoloColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Elevated,
    onPrimaryContainer = AccentSoft,
    secondary = AccentSoft,
    onSecondary = Canvas,
    secondaryContainer = Surface4,
    onSecondaryContainer = TextPrimary,
    tertiary = AccentDeep,
    onTertiary = OnAccent,
    background = Canvas,
    onBackground = TextPrimary,
    surface = Surface2,
    onSurface = TextPrimary,
    surfaceVariant = Surface3,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = TextPrimary,
    inverseOnSurface = Canvas,
    inversePrimary = AccentDeep,
    error = Danger,
    onError = Canvas,
    errorContainer = DangerSurface,
    onErrorContainer = Color(0xFFFFDADA),
    outline = Color(0xFF3A4049),
    outlineVariant = Surface4,
    scrim = Color.Black,
    surfaceBright = Surface4,
    surfaceDim = Canvas,
    surfaceContainerLowest = Canvas,
    surfaceContainerLow = Surface1,
    surfaceContainer = Surface2,
    surfaceContainerHigh = Surface3,
    surfaceContainerHighest = Surface4,
)

/** Solo is a single, always-dark experience; dynamic (wallpaper) colour stays off. */
@Composable
fun SoloTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            // Light status bar icons on the dark UI.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SoloColorScheme,
        typography = Typography,
        shapes = SoloShapes,
    ) {
        // Bare Text() reads LocalTextStyle, so Plus Jakarta Sans is the global default here.
        // Provided directly (not merged over M3's bodyLarge) and without a fixed line height,
        // so a Text that only sets a larger fontSize (lyrics, hero titles) gets line boxes
        // that scale with it instead of overlapping when it wraps.
        CompositionLocalProvider(
            LocalContentColor provides TextPrimary,
            LocalTextStyle provides Typography.bodyMedium.copy(lineHeight = TextUnit.Unspecified),
            content = content,
        )
    }
}
