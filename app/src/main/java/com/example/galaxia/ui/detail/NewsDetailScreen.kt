package com.example.galaxia.ui.detail

import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite

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
    modifier: Modifier = Modifier
) {
    val uriHandler = LocalUriHandler.current

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

                item.extraUrl?.takeIf { it.isNotBlank() }?.let { extraUrl ->
                    IconButton(
                        onClick = {
                            try {
                                uriHandler.openUri(extraUrl)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Abrir no site da NASA",
                            tint = GalaxiaCyan,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Conteúdo completo
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
            ) {
                // Imagem ou Placeholder de Vídeo
                if (item.mediaType == "image" && !item.imageUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = item.imageUrl,
                        contentDescription = item.title,
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 220.dp)
                            .background(Color(0xFF161822)),
                        contentScale = ContentScale.FillWidth
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .background(Color.Black),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = item.title,
                            color = GalaxiaWhite,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }

                Column(modifier = Modifier.padding(20.dp)) {
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
                            text = item.subtitleOrDate,
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
