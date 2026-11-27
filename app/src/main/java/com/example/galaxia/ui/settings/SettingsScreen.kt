package com.example.galaxia.ui.settings

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FormatSize
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.ViewAgenda
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxia.data.local.AppCardSize
import com.example.galaxia.data.local.AppFontSize
import com.example.galaxia.data.local.AppThemeMode
import com.example.galaxia.data.local.UserPreferences
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaSurfaceElevated
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.theme.LocalGalaxiaColors
import com.example.galaxia.ui.theme.LocalUiMetrics
import com.example.galaxia.ui.theme.responsiveContentWidth

/**
 * Tela de Configurações e personalização do GalaxIA:
 * tamanho da fonte, tamanho dos cards e gerenciamento da chave de API da NASA.
 */
@Composable
fun SettingsScreen(
    preferences: UserPreferences,
    isUsingCustomApiKey: Boolean,
    onSelectThemeMode: (AppThemeMode) -> Unit,
    onSelectFontSize: (AppFontSize) -> Unit,
    onSelectCardSize: (AppCardSize) -> Unit,
    onOpenApiKeyDialog: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val metrics = LocalUiMetrics.current

    Surface(
        modifier = modifier.fillMaxSize(),
        color = GalaxiaBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Voltar",
                        tint = GalaxiaWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }

                Text(
                    text = "Configurações",
                    color = GalaxiaWhite,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .responsiveContentWidth(metrics.maxContentWidth)
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = metrics.horizontalPadding)
                        .navigationBarsPadding()
                        .padding(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Pré-visualização ao vivo
                    SettingsSectionHeader(
                        icon = Icons.Default.ViewAgenda,
                        title = "PRÉ-VISUALIZAÇÃO"
                    )

                    LivePreviewCard()

                    // Categoria: Tema do aplicativo
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Palette,
                            title = "TEMA DO APLICATIVO"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(GalaxiaCardBackground)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppThemeMode.entries.forEach { option ->
                                ThemeModeOptionRow(
                                    option = option,
                                    selected = preferences.themeMode == option,
                                    onClick = { onSelectThemeMode(option) }
                                )
                            }
                        }
                    }

                    // Categoria: Tamanho da fonte
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            icon = Icons.Default.FormatSize,
                            title = "TAMANHO DA FONTE"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(GalaxiaCardBackground)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppFontSize.entries.forEach { option ->
                                FontSizeOptionRow(
                                    option = option,
                                    selected = preferences.fontSize == option,
                                    onClick = { onSelectFontSize(option) }
                                )
                            }
                        }
                    }

                    // Categoria: Tamanho dos cards
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            icon = Icons.Default.ViewAgenda,
                            title = "TAMANHO DOS CARDS"
                        )

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(18.dp))
                                .background(GalaxiaCardBackground)
                                .padding(10.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            AppCardSize.entries.forEach { option ->
                                CardSizeOptionRow(
                                    option = option,
                                    selected = preferences.cardSize == option,
                                    onClick = { onSelectCardSize(option) }
                                )
                            }
                        }
                    }

                    // Categoria: API da NASA
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        SettingsSectionHeader(
                            icon = Icons.Default.Key,
                            title = "API DA NASA"
                        )

                        NasaApiKeySettingsCard(
                            isUsingCustomApiKey = isUsingCustomApiKey,
                            onClick = onOpenApiKeyDialog
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsSectionHeader(
    icon: ImageVector,
    title: String
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(horizontal = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = GalaxiaCyan,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = title,
            color = GalaxiaGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
private fun LivePreviewCard() {
    val metrics = LocalUiMetrics.current
    val previewBannerHeight = when (metrics.cardSize) {
        AppCardSize.COMPACT -> 62.dp
        AppCardSize.DEFAULT -> 84.dp
        AppCardSize.LARGE -> 110.dp
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.cardCornerRadius))
            .background(GalaxiaCardBackground)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(previewBannerHeight)
                .background(GalaxiaSurfaceElevated),
            contentAlignment = Alignment.BottomStart
        ) {
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(GalaxiaCyan.copy(alpha = 0.16f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Text(
                    text = "${metrics.cardSize.label.uppercase()} • PREVIEW",
                    color = GalaxiaCyan,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Column(modifier = Modifier.padding(metrics.cardContentPadding)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = GalaxiaGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "26 de nov. de 2026",
                    color = GalaxiaGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Nebulosa de Órion (M42)",
                color = GalaxiaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Uma das regiões de formação estelar mais brilhantes do céu noturno, visível a olho nu logo abaixo do cinturão de Órion e repleta de gás ionizado e estrelas jovens.",
                color = GalaxiaGray,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                maxLines = metrics.cardDescriptionMaxLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun ThemeModeOptionRow(
    option: AppThemeMode,
    selected: Boolean,
    onClick: () -> Unit
) {
    val icon = when (option) {
        AppThemeMode.DARK -> Icons.Default.DarkMode
        AppThemeMode.LIGHT -> Icons.Default.LightMode
        AppThemeMode.SYSTEM -> Icons.Default.BrightnessAuto
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) GalaxiaCyan.copy(alpha = 0.12f) else GalaxiaSurfaceElevated,
        border = if (selected) BorderStroke(1.dp, GalaxiaCyan.copy(alpha = 0.65f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) GalaxiaCyan.copy(alpha = 0.2f) else GalaxiaCardBackground),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (selected) GalaxiaCyan else GalaxiaWhite,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    color = GalaxiaWhite,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = option.description,
                    color = GalaxiaGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun FontSizeOptionRow(
    option: AppFontSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    val sampleSp = when (option) {
        AppFontSize.SMALL -> 13.sp
        AppFontSize.DEFAULT -> 16.sp
        AppFontSize.LARGE -> 19.sp
        AppFontSize.EXTRA_LARGE -> 22.sp
    }

    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) GalaxiaCyan.copy(alpha = 0.12f) else GalaxiaSurfaceElevated,
        border = if (selected) BorderStroke(1.dp, GalaxiaCyan.copy(alpha = 0.65f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (selected) GalaxiaCyan.copy(alpha = 0.2f) else GalaxiaCardBackground),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Aa",
                    color = if (selected) GalaxiaCyan else GalaxiaWhite,
                    fontSize = sampleSp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    color = GalaxiaWhite,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = "${(option.scale * 100).toInt()}% da escala padrão",
                    color = GalaxiaGray,
                    fontSize = 12.sp
                )
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

@Composable
private fun CardSizeOptionRow(
    option: AppCardSize,
    selected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) GalaxiaCyan.copy(alpha = 0.12f) else GalaxiaSurfaceElevated,
        border = if (selected) BorderStroke(1.dp, GalaxiaCyan.copy(alpha = 0.65f)) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            CardSizeGlyph(option = option, selected = selected)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = option.label,
                    color = GalaxiaWhite,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                )
                Text(
                    text = option.description,
                    color = GalaxiaGray,
                    fontSize = 12.sp,
                    lineHeight = 16.sp
                )
            }

            if (selected) {
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Selecionado",
                    tint = GalaxiaCyan,
                    modifier = Modifier.size(22.dp)
                )
            }
        }
    }
}

/**
 * Ícone ilustrativo proporcional representando visualmente o formato Compacto, Padrão ou Grande.
 */
@Composable
private fun CardSizeGlyph(
    option: AppCardSize,
    selected: Boolean
) {
    val accent = if (selected) GalaxiaCyan else GalaxiaGray
    val imageHeight = when (option) {
        AppCardSize.COMPACT -> 10.dp
        AppCardSize.DEFAULT -> 16.dp
        AppCardSize.LARGE -> 22.dp
    }
    val lineCount = when (option) {
        AppCardSize.COMPACT -> 1
        AppCardSize.DEFAULT -> 2
        AppCardSize.LARGE -> 2
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (selected) GalaxiaCyan.copy(alpha = 0.18f) else GalaxiaCardBackground)
            .padding(6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(5.dp))
                .background(GalaxiaSurfaceElevated)
                .padding(3.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
                    .clip(RoundedCornerShape(3.dp))
                    .background(accent.copy(alpha = 0.75f))
            )
            repeat(lineCount) { idx ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (idx == 0) 0.85f else 0.55f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent.copy(alpha = 0.45f))
                )
            }
        }
    }
}

@Composable
private fun NasaApiKeySettingsCard(
    isUsingCustomApiKey: Boolean,
    onClick: () -> Unit
) {
    val palette = LocalGalaxiaColors.current
    val statusColor = if (isUsingCustomApiKey) palette.statusSuccessText else palette.statusWarningText
    val statusBg = if (isUsingCustomApiKey) palette.statusSuccessBg else palette.statusWarningBg

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(GalaxiaCardBackground)
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isUsingCustomApiKey) GalaxiaCyan.copy(alpha = 0.16f) else GalaxiaSurfaceElevated
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Key,
                    contentDescription = null,
                    tint = if (isUsingCustomApiKey) GalaxiaCyan else GalaxiaGray,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Chave de API da NASA",
                    color = GalaxiaWhite,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(statusBg)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isUsingCustomApiKey) "Chave pessoal ativa" else "Usando DEMO_KEY (padrão)",
                        color = statusColor,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = "Configurar chave de API",
                tint = GalaxiaCyan,
                modifier = Modifier.size(22.dp)
            )
        }

        Text(
            text = "Configure sua chave gratuita da NASA (até 1.000 req/hora) ou alterne para a DEMO_KEY padrão (30 req/hora).",
            color = GalaxiaGray,
            fontSize = 13.sp,
            lineHeight = 18.sp
        )
    }
}
