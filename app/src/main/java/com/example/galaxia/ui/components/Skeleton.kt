package com.example.galaxia.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.example.galaxia.ui.theme.GalaxiaCardBackground

/**
 * Modificador customizado que aplica um efeito Shimmer (brilho deslizante) contínuo e suave,
 * respeitando as cores e a identidade visual escura do app GalaxIA.
 */
@Composable
fun Modifier.shimmerEffect(
    baseColor: Color = Color(0xFF161926),
    highlightColor: Color = Color(0xFF282D44),
    shape: Shape = RoundedCornerShape(8.dp)
): Modifier {
    var size by remember { mutableStateOf(IntSize.Zero) }
    val transition = rememberInfiniteTransition(label = "ShimmerTransition")

    val widthPx = if (size.width > 0) size.width.toFloat() else 800f
    val heightPx = if (size.height > 0) size.height.toFloat() else 400f

    val startOffsetX by transition.animateFloat(
        initialValue = -2 * widthPx,
        targetValue = 2 * widthPx,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "ShimmerOffset"
    )

    return this
        .onGloballyPositioned { size = it.size }
        .clip(shape)
        .background(
            brush = Brush.linearGradient(
                colors = listOf(
                    baseColor,
                    highlightColor,
                    baseColor
                ),
                start = Offset(startOffsetX, 0f),
                end = Offset(startOffsetX + widthPx, heightPx.coerceAtLeast(1f))
            ),
            shape = shape
        )
}

/**
 * Bloco Skeleton genérico para imagens ou caixas com cantos arredondados.
 */
@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(12.dp)
) {
    Box(
        modifier = modifier.shimmerEffect(shape = shape)
    )
}

/**
 * Linha Skeleton genérica para simular textos (títulos, datas, descrições).
 */
@Composable
fun SkeletonTextLine(
    modifier: Modifier = Modifier,
    height: Dp = 14.dp,
    widthFraction: Float = 1f,
    shape: Shape = RoundedCornerShape(6.dp)
) {
    Box(
        modifier = modifier
            .fillMaxWidth(widthFraction)
            .height(height)
            .shimmerEffect(shape = shape)
    )
}

/**
 * Skeleton que reproduz fielmente a estrutura visual do [FeedCard].
 */
@Composable
fun FeedCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
    ) {
        // Placeholder da Imagem principal (230dp)
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Data do Item
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(
                    modifier = Modifier.size(14.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(6.dp))
                SkeletonTextLine(height = 12.dp, widthFraction = 0.35f)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título
            SkeletonTextLine(height = 20.dp, widthFraction = 0.85f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 20.dp, widthFraction = 0.5f)

            Spacer(modifier = Modifier.height(10.dp))

            // Autor / Copyright
            Row(verticalAlignment = Alignment.CenterVertically) {
                SkeletonBox(
                    modifier = Modifier.size(12.dp),
                    shape = CircleShape
                )
                Spacer(modifier = Modifier.width(6.dp))
                SkeletonTextLine(height = 11.dp, widthFraction = 0.4f)
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Explicação / Descrição (3 linhas)
            SkeletonTextLine(height = 13.dp, widthFraction = 1f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 13.dp, widthFraction = 0.92f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 13.dp, widthFraction = 0.65f)

            Spacer(modifier = Modifier.height(18.dp))

            // Ações na parte inferior
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                SkeletonBox(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                )
                SkeletonBox(
                    modifier = Modifier.size(36.dp),
                    shape = CircleShape
                )
            }
        }
    }
}

/**
 * Lista de Skeletons para a aba do Feed de fotos (APOD).
 */
@Composable
fun FeedSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            FeedCardSkeleton()
        }
    }
}

/**
 * Skeleton que reproduz fielmente a estrutura da tela de Histórico ([HistoryCard]).
 */
@Composable
fun HistoryCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
    ) {
        // Imagem principal (260dp)
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Título
            SkeletonTextLine(height = 22.dp, widthFraction = 0.8f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 22.dp, widthFraction = 0.45f)

            Spacer(modifier = Modifier.height(12.dp))

            // Linha de Data e Autor
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(modifier = Modifier.size(16.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(6.dp))
                    SkeletonTextLine(height = 13.dp, widthFraction = 0.3f)
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    SkeletonBox(modifier = Modifier.size(16.dp), shape = CircleShape)
                    Spacer(modifier = Modifier.width(6.dp))
                    SkeletonTextLine(height = 13.dp, widthFraction = 0.35f)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Descrição (4 linhas)
            SkeletonTextLine(height = 14.dp, widthFraction = 1f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 14.dp, widthFraction = 0.95f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 14.dp, widthFraction = 0.88f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 14.dp, widthFraction = 0.5f)

            Spacer(modifier = Modifier.height(20.dp))

            // Botão de Favorito
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SkeletonBox(modifier = Modifier.size(40.dp), shape = CircleShape)
            }
        }
    }
}

/**
 * Skeleton que reproduz o card compacto da pesquisa ([SearchResultCard]).
 */
@Composable
fun SearchResultCardSkeleton(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GalaxiaCardBackground)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Thumbnail da Imagem (88x88)
        SkeletonBox(
            modifier = Modifier.size(88.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Data
            SkeletonTextLine(height = 11.dp, widthFraction = 0.4f)
            Spacer(modifier = Modifier.height(6.dp))
            // Título (2 linhas)
            SkeletonTextLine(height = 15.dp, widthFraction = 0.9f)
            Spacer(modifier = Modifier.height(4.dp))
            SkeletonTextLine(height = 15.dp, widthFraction = 0.6f)
            Spacer(modifier = Modifier.height(8.dp))
            // Descrição (1 linha)
            SkeletonTextLine(height = 12.dp, widthFraction = 0.95f)
        }
    }
}

/**
 * Lista de Skeletons para o resultado de pesquisa.
 */
@Composable
fun SearchResultSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 4
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        repeat(count) {
            SearchResultCardSkeleton()
        }
    }
}

/**
 * Skeleton que reproduz a estrutura da lista de Favoritos ([FavoriteCard]).
 */
@Composable
fun FavoriteCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
    ) {
        // Imagem principal (230dp)
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(230.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            // Linha com Tag do tipo (FOTO DO DIA / NOTICIA) e Data
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonTextLine(height = 12.dp, widthFraction = 0.3f)
                SkeletonTextLine(height = 12.dp, widthFraction = 0.25f)
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            SkeletonTextLine(height = 20.dp, widthFraction = 0.85f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 20.dp, widthFraction = 0.55f)

            Spacer(modifier = Modifier.height(12.dp))

            // Descrição (2 linhas)
            SkeletonTextLine(height = 13.dp, widthFraction = 1f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 13.dp, widthFraction = 0.75f)

            Spacer(modifier = Modifier.height(16.dp))

            // Botão de desfavoritar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                SkeletonBox(modifier = Modifier.size(36.dp), shape = CircleShape)
            }
        }
    }
}

/**
 * Lista de Skeletons para a aba de Favoritos.
 */
@Composable
fun FavoriteSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            FavoriteCardSkeleton()
        }
    }
}

/**
 * Skeleton que reproduz a estrutura de um card de Notícias e Curiosidades ([SpaceNewsCard]).
 */
@Composable
fun SpaceNewsCardSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
    ) {
        SkeletonBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(210.dp),
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp, bottomStart = 0.dp, bottomEnd = 0.dp)
        )

        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonTextLine(height = 14.dp, widthFraction = 0.38f)
                SkeletonTextLine(height = 12.dp, widthFraction = 0.24f)
            }

            Spacer(modifier = Modifier.height(12.dp))

            SkeletonTextLine(height = 19.dp, widthFraction = 0.88f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 19.dp, widthFraction = 0.52f)

            Spacer(modifier = Modifier.height(12.dp))

            SkeletonTextLine(height = 13.dp, widthFraction = 1f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 13.dp, widthFraction = 0.92f)
            Spacer(modifier = Modifier.height(6.dp))
            SkeletonTextLine(height = 13.dp, widthFraction = 0.65f)

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                SkeletonBox(modifier = Modifier.size(34.dp), shape = CircleShape)
                SkeletonTextLine(height = 14.dp, widthFraction = 0.32f)
            }
        }
    }
}

/**
 * Lista de Skeletons para a aba de Notícias e Curiosidades.
 */
@Composable
fun SpaceNewsSkeletonList(
    modifier: Modifier = Modifier,
    count: Int = 3
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        repeat(count) {
            SpaceNewsCardSkeleton()
        }
    }
}
