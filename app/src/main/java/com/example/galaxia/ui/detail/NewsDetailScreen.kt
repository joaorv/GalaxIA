package com.example.galaxia.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.draw.clip
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.galaxia.ui.components.OptimizedAsyncImage
import com.example.galaxia.ui.components.ScrollToTopButton
import com.example.galaxia.ui.components.VideoMediaCard
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.allImageUrls
import com.example.galaxia.data.local.displayHdUrl
import com.example.galaxia.data.local.displayThumbnailUrl
import com.example.galaxia.data.local.officialNasaUrl
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.remote.SpaceNewsFormatters
import kotlinx.coroutines.CancellationException
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaSurfaceElevated
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.theme.LocalUiMetrics
import com.example.galaxia.ui.theme.responsiveContentWidth
import com.example.galaxia.ui.util.formatDateBr
import kotlinx.coroutines.launch

/**
 * Tela de detalhe de uma notícia/foto, com o conteúdo completo e seta para voltar.
 * Usa a entidade unificada de favoritos como modelo de exibição para servir
 * tanto itens do Feed/Histórico (APOD) quanto itens salvos nos Favoritos.
 */
@Composable
fun NewsDetailScreen(
    item: FavoriteEntity,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    initialImageIndex: Int = 0
) {
    val uriHandler = LocalUriHandler.current
    val scrollState = rememberScrollState()
    val coroutineScope = rememberCoroutineScope()
    val metrics = LocalUiMetrics.current

    // Surface bloqueia toques para a tela que fica por baixo
    Surface(
        modifier = modifier.fillMaxSize(),
        color = GalaxiaBackground
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // Cabeçalho com seta de voltar e ações
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
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
                    text = tagLabel(item.itemType),
                    color = GalaxiaCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorite) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (item.officialNasaUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                uriHandler.openUri(item.officialNasaUrl)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Abrir fonte original",
                            tint = GalaxiaCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Conteúdo completo com botão discreto de retorno ao topo
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .responsiveContentWidth(metrics.maxContentWidth)
                        .verticalScroll(scrollState)
                ) {
                // Imagem, Galeria em Carrossel, Vídeo ou Placeholder (artigos sem imagem não exibem placeholder preto)
                val isVideoContent = item.mediaType == "video" || item.mediaType == "iframe" || item.mediaType == "embed"
                val galleryImages = item.allImageUrls
                if (isVideoContent && !item.extraUrl.isNullOrBlank()) {
                    var resolvedVideoUrl by remember(item.extraUrl) { mutableStateOf(item.extraUrl) }
                    val nasaMediaId = remember(item.extraUrl) {
                        SpaceNewsFormatters.extractNasaMediaDetailsId(item.extraUrl)
                    }

                    LaunchedEffect(nasaMediaId) {
                        if (nasaMediaId != null) {
                            try {
                                val manifest = RetrofitClient.nasaImagesApiService.getAssetManifest(nasaMediaId)
                                val hrefs = manifest.collection?.items.orEmpty().mapNotNull { it.href }
                                SpaceNewsFormatters.selectBestNasaVideoUrl(hrefs)?.let { mp4Url ->
                                    resolvedVideoUrl = mp4Url
                                }
                            } catch (e: CancellationException) {
                                throw e
                            } catch (_: Exception) {
                                // Mantém a URL da página oficial da NASA como fallback
                            }
                        }
                    }

                    VideoMediaCard(
                        mediaUrl = resolvedVideoUrl,
                        thumbnailUrl = item.displayThumbnailUrl,
                        title = item.title,
                        permalinkUrl = item.officialNasaUrl,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(260.dp)
                    )
                } else if (item.mediaType == "image" && galleryImages.size > 1) {
                    val pagerState = rememberPagerState(
                        initialPage = initialImageIndex.coerceIn(0, galleryImages.lastIndex),
                        pageCount = { galleryImages.size }
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(GalaxiaSurfaceElevated)
                    ) {
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier.fillMaxSize()
                        ) { page ->
                            OptimizedAsyncImage(
                                lowResUrl = galleryImages[page],
                                highResUrl = null,
                                contentDescription = "${item.title} (${page + 1} de ${galleryImages.size})",
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(GalaxiaSurfaceElevated),
                                contentScale = ContentScale.Fit
                            )
                        }

                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.65f))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / ${galleryImages.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Row(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.45f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            repeat(galleryImages.size) { idx ->
                                val isCurrent = idx == pagerState.currentPage
                                Box(
                                    modifier = Modifier
                                        .size(if (isCurrent) 7.dp else 5.dp)
                                        .clip(CircleShape)
                                        .background(
                                            if (isCurrent) GalaxiaCyan else Color.White.copy(alpha = 0.55f)
                                        )
                                )
                            }
                        }
                    }
                } else if (item.mediaType == "image" && !item.imageUrl.isNullOrBlank()) {
                    OptimizedAsyncImage(
                        lowResUrl = item.imageUrl,
                        highResUrl = item.displayHdUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .background(GalaxiaSurfaceElevated),
                        contentScale = ContentScale.FillWidth
                    )
                } else if (item.mediaType != "article") {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            color = Color.White,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(metrics.horizontalPadding)) {
                    // Data
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GalaxiaGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDateBr(item.subtitleOrDate),
                            color = GalaxiaGray,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Título
                    Text(
                        text = item.title,
                        color = GalaxiaWhite,
                        fontSize = 24.sp,
                        lineHeight = 30.sp,
                        fontWeight = FontWeight.Bold
                    )

                    // Autor / Copyright se houver
                    item.authorOrCopyright?.let { copyright ->
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Copyright,
                                contentDescription = null,
                                tint = GalaxiaGray,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = copyright.trim().replace("\n", " "),
                                color = GalaxiaGray,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Texto completo da Explicação
                    Text(
                        text = item.explanationOrBody,
                        color = GalaxiaWhite.copy(alpha = 0.85f),
                        fontSize = 16.sp,
                        lineHeight = 25.sp
                    )
                }
            }

            ScrollToTopButton(
                visible = scrollState.value > 600,
                onClick = {
                    coroutineScope.launch { scrollState.animateScrollTo(0) }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = 16.dp, bottom = 16.dp)
            )
            }
        }
    }
}

private fun tagLabel(itemType: String): String = when (itemType) {
    FavoriteType.APOD.name -> "FOTO DO DIA"
    FavoriteType.NEWS.name -> "NOTÍCIA"
    FavoriteType.CURIOSITY.name -> "CURIOSIDADE"
    else -> itemType
}

@Preview(showBackground = true)
@Composable
fun NewsDetailScreenPreview() {
    NewsDetailScreen(
        item = FavoriteEntity(
            id = "apod_2026-10-24",
            itemType = FavoriteType.APOD.name,
            title = "A Nebulosa de Órion",
            subtitleOrDate = "2026-10-24",
            explanationOrBody = "Uma das nebulosas mais brilhantes do céu no turno, localizada logo ao sul do cinturão de Órion.",
            imageUrl = null
        ),
        isFavorite = false,
        onToggleFavorite = {},
        onBack = {}
    )
}
