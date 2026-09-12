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

// Royal Edition — royal-purple primary with royal-gold accents on a deep
// purple canvas. Kept deterministic (dynamicColor off) so the premium look is
// consistent across devices instead of being replaced by the wallpaper palette.
private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    onPrimary = Color(0xFF241540),
    secondary = RoyalPurpleLight,
    onSecondary = Color(0xFF1A0F30),
    tertiary = RoyalGoldLight,
    background = AppBackground,
    onBackground = Color(0xFFF3ECFF),
    surface = Color(0xFF1A0F30),
    onSurface = Color(0xFFF3ECFF),
    surfaceVariant = GridBackground,
    onSurfaceVariant = Color(0xFFD6C9F2),
    primaryContainer = RoyalPurpleDeep,
    onPrimaryContainer = Color(0xFFF5E09A),
    secondaryContainer = Color(0xFF2B1D4A),
    outline = Color(0x33D4AF37),
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
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Royal Edition ships a fixed premium palette; wallpaper-derived dynamic
    // colour is off by default so the royal-purple/gold look is guaranteed.
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
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
        // from the typography — so provide Montserrat as the global default so
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