package com.example.galaxia.ui.favorites

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.viewmodel.FeedViewModel

@Composable
fun FavoritesScreen(
    viewModel: FeedViewModel
) {
    val favorites by viewModel.filteredFavorites.collectAsState()
    val selectedFilter by viewModel.selectedFavoriteFilter.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalaxiaBackground)
    ) {
        // Cabeçalho da Tela de Favoritos
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Meus Favoritos",
                    color = GalaxiaWhite,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "${favorites.size} ${if (favorites.size == 1) "item" else "itens"}",
                    color = GalaxiaGray,
                    fontSize = 14.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chips de Filtro por Categoria (Estratégia A)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    val isSelected = selectedFilter == null
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFavoriteFilter(null) },
                        label = { Text("Todos") },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GalaxiaCardBackground,
                            labelColor = GalaxiaGray,
                            selectedContainerColor = GalaxiaCyan,
                            selectedLabelColor = GalaxiaBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                items(FavoriteType.values()) { type ->
                    val isSelected = selectedFilter == type
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.setFavoriteFilter(if (isSelected) null else type) },
                        label = { Text(type.displayName) },
                        colors = FilterChipDefaults.filterChipColors(
                            containerColor = GalaxiaCardBackground,
                            labelColor = GalaxiaGray,
                            selectedContainerColor = GalaxiaCyan,
                            selectedLabelColor = GalaxiaBackground
                        ),
                        border = null,
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        // Conteúdo da Lista ou Estado Vazio
        if (favorites.isEmpty()) {
            EmptyFavoritesState(selectedFilter = selectedFilter)
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp, top = 4.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(
                    items = favorites,
                    key = { it.id }
                ) { item ->
                    FavoriteCard(
                        favorite = item,
                        onRemoveFavorite = { viewModel.removeFavoriteById(item.id) }
                    )
                }
            }
        }
    }
}

@Composable
private fun FavoriteCard(
    favorite: FavoriteEntity,
    onRemoveFavorite: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
            .clickable { expanded = !expanded }
    ) {
        // Imagem ou Placeholder de Vídeo
        if (favorite.mediaType == "image" && !favorite.imageUrl.isNullOrBlank()) {
            AsyncImage(
                model = favorite.imageUrl,
                contentDescription = favorite.title,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = favorite.title,
                    color = GalaxiaWhite,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // Tag de Categoria
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val tagLabel = when (favorite.itemType) {
                    FavoriteType.APOD.name -> "FOTO DO DIA"
                    FavoriteType.NEWS.name -> "NOTÍCIA"
                    FavoriteType.CURIOSITY.name -> "CURIOSIDADE"
                    else -> favorite.itemType
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(GalaxiaCyan.copy(alpha = 0.15f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = tagLabel,
                        color = GalaxiaCyan,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Data de Salvamento ou Subtítulo
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        tint = GalaxiaGray,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = favorite.subtitleOrDate,
                        color = GalaxiaGray,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            Text(
                text = favorite.title,
                color = GalaxiaWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Autor / Copyright se houver
            favorite.authorOrCopyright?.let { copyright ->
                Spacer(modifier = Modifier.height(4.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

            // Texto da Explicação
            Text(
                text = favorite.explanationOrBody,
                color = GalaxiaGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = if (expanded) Int.MAX_VALUE else 3
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ações: Botão de Remover dos Favoritos e Link HD
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onRemoveFavorite) {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = "Remover dos favoritos",
                            tint = GalaxiaCyan,
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    favorite.extraUrl?.let { extraUrl ->
                        IconButton(
                            onClick = {
                                try {
                                    uriHandler.openUri(extraUrl)
                                } catch (_: Exception) {}
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                                contentDescription = "Abrir original em HD",
                                tint = GalaxiaCyan,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (expanded) "MOSTRAR MENOS" else "LER MAIS →",
                    color = GalaxiaCyan,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.clickable { expanded = !expanded }
                )
            }
        }
    }
}

@Composable
private fun EmptyFavoritesState(selectedFilter: FavoriteType?) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.FavoriteBorder,
                contentDescription = null,
                tint = GalaxiaGray.copy(alpha = 0.5f),
                modifier = Modifier.size(72.dp)
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = if (selectedFilter == null) {
                    "Nenhum favorito salvo"
                } else {
                    "Nenhum item em \"${selectedFilter.displayName}\""
                },
                color = GalaxiaWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Toque no ícone de coração em qualquer foto no Feed ou no Histórico para salvá-la aqui para acesso rápido.",
                color = GalaxiaGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}
