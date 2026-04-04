package com.psychoai.app.ui.theme

import android.app.Activity
import androidx.compose.foundation.LocalIndication
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = Purple,
    onPrimary = TextPrimary,
    primaryContainer = PurpleDark,
    onPrimaryContainer = TextPrimary,
    secondary = Teal,
    onSecondary = TextPrimary,
    tertiary = PurpleLight,
    background = BackgroundBlack,
    onBackground = TextPrimary,
    surface = SurfaceDark,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceMedium,
    onSurfaceVariant = TextSecondary,
    outline = InputBorder,
    error = ErrorRed,
    onError = TextPrimary
)

@Composable
fun PsychoAITheme(
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = Color.Black.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
        }
    }

    // This explicitly provides Material3's ripple as LocalIndication,
    // preventing the old material PlatformRipple from causing the crash
    CompositionLocalProvider(
        LocalIndication provides ripple()
    ) {
        MaterialTheme(
            colorScheme = DarkColorScheme,
            typography = Typography,
            content = content
        )
    }
}