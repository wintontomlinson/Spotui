package com.music.spotui.ui.theme

import android.app.Activity
import android.graphics.Color.toArgb
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat

// A single, deliberate dark scheme built around the amber accent, so every Material
// component (buttons, switches, sliders, indicators) picks up the app's colour instead
// of a stray purple or a wallpaper tint.
private val DarkColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    secondary = Accent,
    onSecondary = OnAccent,
    tertiary = AccentDark,
    background = AppBackground,
    onBackground = Color(0xFFF2F2F5),
    surface = SurfaceElevated,
    onSurface = Color(0xFFF2F2F5),
    surfaceVariant = SurfaceCard,
    onSurfaceVariant = Color(0xFFB6B6BE),
    outline = Hairline,
)

private val LightColorScheme = lightColorScheme(
    primary = Purple40,
    secondary = PurpleGrey40,
    tertiary = Pink40 ,
    background = AppBackground,
    /* Other default colors to override

    surface = Color(0xFFFFFBFE),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onTertiary = Color.White,
    onBackground = Color(0xFF1C1B1F),
    onSurface = Color(0xFF1C1B1F),
    */
)

@Composable
fun SpotuiTheme(
    // The app is a single, always dark, amber themed experience by design.
    darkTheme: Boolean = true,
    // Dynamic color is intentionally OFF. Letting Android 12+ retint the UI from the
    // user's wallpaper overrode the amber accent and the deliberate dark surfaces, so the
    // premium look changed from phone to phone. The app owns its palette now.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Transparent.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false // white icons for dark‑mode‑only app
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
    ) {
        // Bare Text() uses LocalTextStyle, which MaterialTheme does NOT derive
        // from the typography, so provide Montserrat as the global default so
        // every screen picks it up without touching each Text call.
        androidx.compose.material3.ProvideTextStyle(
            value = androidx.compose.material3.LocalTextStyle.current.copy(
                fontFamily = Montserrat,
                letterSpacing = (-0.2).sp,
            ),
            content = content,
        )
    }
}