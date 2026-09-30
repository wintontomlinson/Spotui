package com.music.spotui.ui.theme

import android.app.Activity
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// One deliberate dark scheme, so every Material component (switches, sliders, sheets,
// dialogs, menus) picks up the Solo palette instead of a stray default or wallpaper tint.
private val SoloColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = OnGold,
    primaryContainer = Color(0xFF3A2C12),
    onPrimaryContainer = GoldLight,
    secondary = Lilac,
    onSecondary = Ink,
    secondaryContainer = Amethyst,
    onSecondaryContainer = Ivory,
    tertiary = GoldDeep,
    onTertiary = OnGold,
    background = Ink,
    onBackground = Ivory,
    surface = Dusk,
    onSurface = Ivory,
    surfaceVariant = Velvet,
    onSurfaceVariant = TextSecondary,
    surfaceTint = Color.Transparent,
    inverseSurface = Ivory,
    inverseOnSurface = Ink,
    inversePrimary = GoldDeep,
    error = Danger,
    onError = Ink,
    errorContainer = DangerSurface,
    onErrorContainer = Color(0xFFFFDADA),
    outline = Color(0xFF4A3F5E),
    outlineVariant = Amethyst,
    scrim = Color.Black,
    surfaceBright = Amethyst,
    surfaceDim = Ink,
    surfaceContainerLowest = Ink,
    surfaceContainerLow = Night,
    surfaceContainer = Dusk,
    surfaceContainerHigh = Velvet,
    surfaceContainerHighest = Amethyst,
)

/** Solo is a single, always-dark experience; dynamic (wallpaper) colour stays off. */
@Composable
fun SoloTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            // Light (ivory) status bar icons on the dark UI.
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    MaterialTheme(
        colorScheme = SoloColorScheme,
        typography = Typography,
        shapes = SoloShapes,
    ) {
        // Bare Text() reads LocalTextStyle, which MaterialTheme does not derive from the
        // typography, so Manrope is provided as the global default here.
        CompositionLocalProvider(LocalContentColor provides Ivory) {
            ProvideTextStyle(
                value = LocalTextStyle.current.merge(Typography.bodyMedium),
                content = content,
            )
        }
    }
}
