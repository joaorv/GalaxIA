package com.example.galaxia.ui.theme

import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.galaxia.data.local.AppCardSize

@Immutable
data class GalaxiaUiMetrics(
    val isCompactScreen: Boolean,
    val isExpandedScreen: Boolean,
    val horizontalPadding: Dp,
    val maxContentWidth: Dp,
    val cardSpacing: Dp,
    val cardContentPadding: Dp,
    val cardCornerRadius: Dp,
    val cardMediaHeight: Dp,
    val heroMediaHeight: Dp,
    val historyMediaHeight: Dp,
    val cardDescriptionMaxLines: Int,
    val searchThumbSize: Dp,
    val cardSize: AppCardSize
)

fun computeUiMetrics(
    screenWidthDp: Int,
    screenHeightDp: Int,
    cardSize: AppCardSize
): GalaxiaUiMetrics {
    val isCompactScreen = screenWidthDp < 360
    val isExpandedScreen = screenWidthDp >= 600

    val horizontalPadding = when {
        isCompactScreen -> 12.dp
        isExpandedScreen -> 24.dp
        else -> 16.dp
    }
    val maxContentWidth = if (isExpandedScreen) 680.dp else Dp.Unspecified

    val baseSpacing = when (cardSize) {
        AppCardSize.COMPACT -> 10.dp
        AppCardSize.DEFAULT -> 14.dp
        AppCardSize.LARGE -> 18.dp
    }
    val cardSpacing = if (isCompactScreen) (baseSpacing - 2.dp).coerceAtLeast(8.dp) else baseSpacing

    val cardContentPadding = when (cardSize) {
        AppCardSize.COMPACT -> 12.dp
        AppCardSize.DEFAULT -> 16.dp
        AppCardSize.LARGE -> 20.dp
    }
    val cardCornerRadius = when (cardSize) {
        AppCardSize.COMPACT -> 16.dp
        AppCardSize.DEFAULT -> 18.dp
        AppCardSize.LARGE -> 22.dp
    }

    val safeHeight = screenHeightDp.coerceAtLeast(520)
    val cardMediaHeight = when (cardSize) {
        AppCardSize.COMPACT -> (safeHeight * 0.21f).dp.coerceIn(135.dp, 175.dp)
        AppCardSize.DEFAULT -> (safeHeight * 0.27f).dp.coerceIn(185.dp, 230.dp)
        AppCardSize.LARGE -> (safeHeight * 0.34f).dp.coerceIn(235.dp, 295.dp)
    }
    val heroMediaHeight = cardMediaHeight + 20.dp
    val historyMediaHeight = cardMediaHeight + 25.dp

    val cardDescriptionMaxLines = when (cardSize) {
        AppCardSize.COMPACT -> 2
        AppCardSize.DEFAULT -> 3
        AppCardSize.LARGE -> 5
    }

    val searchThumbSize = when (cardSize) {
        AppCardSize.COMPACT -> if (isCompactScreen) 66.dp else 72.dp
        AppCardSize.DEFAULT -> if (isCompactScreen) 78.dp else 88.dp
        AppCardSize.LARGE -> if (isCompactScreen) 96.dp else 108.dp
    }

    return GalaxiaUiMetrics(
        isCompactScreen = isCompactScreen,
        isExpandedScreen = isExpandedScreen,
        horizontalPadding = horizontalPadding,
        maxContentWidth = maxContentWidth,
        cardSpacing = cardSpacing,
        cardContentPadding = cardContentPadding,
        cardCornerRadius = cardCornerRadius,
        cardMediaHeight = cardMediaHeight,
        heroMediaHeight = heroMediaHeight,
        historyMediaHeight = historyMediaHeight,
        cardDescriptionMaxLines = cardDescriptionMaxLines,
        searchThumbSize = searchThumbSize,
        cardSize = cardSize
    )
}

val LocalUiMetrics = staticCompositionLocalOf {
    computeUiMetrics(screenWidthDp = 390, screenHeightDp = 844, cardSize = AppCardSize.DEFAULT)
}

@Composable
fun rememberGalaxiaUiMetrics(cardSize: AppCardSize): GalaxiaUiMetrics {
    val configuration = LocalConfiguration.current
    return remember(configuration.screenWidthDp, configuration.screenHeightDp, cardSize) {
        computeUiMetrics(
            screenWidthDp = configuration.screenWidthDp,
            screenHeightDp = configuration.screenHeightDp,
            cardSize = cardSize
        )
    }
}

fun Modifier.responsiveContentWidth(maxWidth: Dp): Modifier {
    return if (maxWidth != Dp.Unspecified) this.widthIn(max = maxWidth) else this
}
