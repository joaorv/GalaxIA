package com.example.galaxia.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import com.example.galaxia.data.local.AppThemeMode
import com.example.galaxia.data.local.UserPreferences

private val DarkColorScheme = darkColorScheme(
    primary = DarkGalaxiaColors.cyan,
    secondary = DarkGalaxiaColors.secondaryText,
    tertiary = Pink80,
    background = DarkGalaxiaColors.background,
    surface = DarkGalaxiaColors.cardBackground,
    onPrimary = DarkGalaxiaColors.background,
    onBackground = DarkGalaxiaColors.onBackground,
    onSurface = DarkGalaxiaColors.onBackground
)

private val LightColorScheme = lightColorScheme(
    primary = LightGalaxiaColors.cyan,
    secondary = LightGalaxiaColors.secondaryText,
    tertiary = Pink40,
    background = LightGalaxiaColors.background,
    surface = LightGalaxiaColors.cardBackground,
    onPrimary = LightGalaxiaColors.cardBackground,
    onBackground = LightGalaxiaColors.onBackground,
    onSurface = LightGalaxiaColors.onBackground
)

@Composable
fun GalaxIATheme(
    userPreferences: UserPreferences = UserPreferences(),
    content: @Composable () -> Unit
) {
    val isDarkTheme = when (userPreferences.themeMode) {
        AppThemeMode.DARK -> true
        AppThemeMode.LIGHT -> false
        AppThemeMode.SYSTEM -> isSystemInDarkTheme()
    }
    val galaxiaColors = if (isDarkTheme) DarkGalaxiaColors else LightGalaxiaColors
    val colorScheme = if (isDarkTheme) DarkColorScheme else LightColorScheme

    val baseDensity = LocalDensity.current
    val scaledDensity = remember(baseDensity, userPreferences.fontSize) {
        Density(
            density = baseDensity.density,
            fontScale = (baseDensity.fontScale * userPreferences.fontSize.scale).coerceIn(0.75f, 1.9f)
        )
    }
    val uiMetrics = rememberGalaxiaUiMetrics(userPreferences.cardSize)

    CompositionLocalProvider(
        LocalGalaxiaColors provides galaxiaColors,
        LocalDensity provides scaledDensity,
        LocalUiMetrics provides uiMetrics
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
