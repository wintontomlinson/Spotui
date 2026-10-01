package com.music.spotui.ui.theme

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
// dialogs, menus) picks up the Midnight Azure palette instead of a stray default or wallpaper tint.
private val SonvraColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = Color(0xFF12264D),
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
    onErrorContainer = Color(0xFFFFDADF),
    outline = Color(0xFF3A4254),
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

/** Sonvra is a single, always-dark experience; dynamic (wallpaper) colour stays off. */
@Composable
fun SonvraTheme(content: @Composable () -> Unit) {
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
        colorScheme = SonvraColorScheme,
        typography = Typography,
        shapes = SonvraShapes,
    ) {
        // Bare Text() reads LocalTextStyle, so Inter is provided as the global default here.
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
