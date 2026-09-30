package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.model.AppThemeMode
import com.example.model.ColorPaletteStyle

@Composable
fun MyApplicationTheme(
    themeMode: AppThemeMode = AppThemeMode.LIGHT,
    paletteStyle: ColorPaletteStyle = ColorPaletteStyle.OCEAN_BLUE,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        AppThemeMode.LIGHT -> false
        AppThemeMode.DARK -> true
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }

    val (primary, primaryContainer, onPrimaryContainer) = when (paletteStyle) {
        ColorPaletteStyle.OCEAN_BLUE -> Triple(OceanBluePrimary, OceanBlueContainer, OceanBlueOnContainer)
        ColorPaletteStyle.EMERALD_MINT -> Triple(EmeraldPrimary, EmeraldContainer, EmeraldOnContainer)
        ColorPaletteStyle.VIBRANT_INDIGO -> Triple(IndigoPrimary, IndigoContainer, IndigoOnContainer)
        ColorPaletteStyle.SUNSET_AMBER -> Triple(AmberPrimary, AmberContainer, AmberOnContainer)
    }

    val colorScheme = if (isDark) {
        darkColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer.copy(alpha = 0.3f),
            onPrimaryContainer = Color.White,
            secondary = OceanBlueAccent,
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF164E63),
            onSecondaryContainer = Color(0xFFCFFAFE),
            tertiary = SuccessGreen,
            background = DarkCanvas,
            onBackground = DarkTextPrimary,
            surface = DarkSurface,
            onSurface = DarkTextPrimary,
            surfaceVariant = DarkSurfaceElevated,
            onSurfaceVariant = DarkTextSecondary,
            outline = DarkBorder,
            error = ErrorRose
        )
    } else {
        lightColorScheme(
            primary = primary,
            onPrimary = Color.White,
            primaryContainer = primaryContainer,
            onPrimaryContainer = onPrimaryContainer,
            secondary = OceanBlueAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFF0FDF4),
            onSecondaryContainer = Color(0xFF166534),
            tertiary = SuccessGreen,
            background = SnowWhite,
            onBackground = TextPrimaryLight,
            surface = PureWhite,
            onSurface = TextPrimaryLight,
            surfaceVariant = OffWhiteSurface,
            onSurfaceVariant = TextSecondaryLight,
            outline = BorderCrispLight,
            error = ErrorRose
        )
    }

    // Configure System Status Bar and Navigation Bar colors and icon contrast
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                // Ensure edge-to-edge transparent bars with correct foreground icon contrast
                window.statusBarColor = android.graphics.Color.TRANSPARENT
                window.navigationBarColor = android.graphics.Color.TRANSPARENT

                val insetsController = WindowCompat.getInsetsController(window, view)
                // When isDark is false (Light Theme), status bar icons (battery, wifi, clock)
                // MUST BE DARK/BLACK so they are clearly visible against the light background!
                insetsController.isAppearanceLightStatusBars = !isDark
                insetsController.isAppearanceLightNavigationBars = !isDark
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
