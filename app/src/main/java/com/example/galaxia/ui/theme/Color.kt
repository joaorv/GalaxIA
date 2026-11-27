package com.example.galaxia.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

@Immutable
data class GalaxiaColors(
    val isDark: Boolean,
    val background: Color,
    val cardBackground: Color,
    val surfaceElevated: Color,
    val bottomBarBackground: Color,
    val cyan: Color,
    val onBackground: Color,
    val secondaryText: Color,
    val shimmerBase: Color,
    val shimmerHighlight: Color,
    val statusSuccessText: Color,
    val statusSuccessBg: Color,
    val statusWarningText: Color,
    val statusWarningBg: Color
)

val DarkGalaxiaColors = GalaxiaColors(
    isDark = true,
    background = Color(0xFF050609),
    cardBackground = Color(0xFF0D0E13),
    surfaceElevated = Color(0xFF161822),
    bottomBarBackground = Color(0xFF0B0C11),
    cyan = Color(0xFF00B8E6),
    onBackground = Color(0xFFF5F5F5),
    secondaryText = Color(0xFF9A9AA2),
    shimmerBase = Color(0xFF161926),
    shimmerHighlight = Color(0xFF282D44),
    statusSuccessText = Color(0xFF5EEB9B),
    statusSuccessBg = Color(0xFF1F3B2C),
    statusWarningText = Color(0xFFFFB454),
    statusWarningBg = Color(0xFF332615)
)

val LightGalaxiaColors = GalaxiaColors(
    isDark = false,
    background = Color(0xFFF3F6FA),
    cardBackground = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFE7EDF5),
    bottomBarBackground = Color(0xFFFFFFFF),
    cyan = Color(0xFF007EA7),
    onBackground = Color(0xFF0F172A),
    secondaryText = Color(0xFF526071),
    shimmerBase = Color(0xFFE2E8F0),
    shimmerHighlight = Color(0xFFF8FAFC),
    statusSuccessText = Color(0xFF137333),
    statusSuccessBg = Color(0xFFD9F5E3),
    statusWarningText = Color(0xFFB45309),
    statusWarningBg = Color(0xFFFEF3C7)
)

val LocalGalaxiaColors = staticCompositionLocalOf { DarkGalaxiaColors }

// Paleta dinâmica GalaxIA (adapta automaticamente entre tema escuro e claro)
val GalaxiaBackground: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.background

val GalaxiaCardBackground: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.cardBackground

val GalaxiaSurfaceElevated: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.surfaceElevated

val GalaxiaBottomBarBackground: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.bottomBarBackground

val GalaxiaCyan: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.cyan

val GalaxiaWhite: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.onBackground

val GalaxiaGray: Color
    @Composable @ReadOnlyComposable get() = LocalGalaxiaColors.current.secondaryText
