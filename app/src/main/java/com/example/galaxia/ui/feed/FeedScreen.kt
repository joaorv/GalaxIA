package com.example.galaxia.ui.feed

import com.example.galaxia.ui.detail.NewsDetailScreen
import com.example.galaxia.ui.history.HistoryScreen
import com.example.galaxia.ui.favorites.FavoritesScreen

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil3.compose.AsyncImage
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.toApodResponse
import com.example.galaxia.data.local.toFavoriteEntity
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.util.formatDateBr
import com.example.galaxia.viewmodel.FeedViewModel
import kotlin.time.Duration.Companion.milliseconds

@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
) {
    val apodState by viewModel.apodState.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val feedFilter by viewModel.feedFilter.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }
    var detailTarget by remember { mutableStateOf<DetailTarget?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Seta "voltar" do sistema fecha o detalhe antes de sair do app
    BackHandler(enabled = detailTarget != null) {
        detailTarget = null
    }

    LaunchedEffect(viewModel) {
        viewModel.userMessage.collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope {
                val showJob = launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Indefinite
                    )
                }
                delay(1200.milliseconds) // Exibe por 1.2s e desaparece rapidamente
                showJob.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = GalaxiaCardBackground,
                    contentColor = GalaxiaWhite,
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        containerColor = GalaxiaBackground
    ) { innerPadding ->
      // A lista continua composta por baixo do detalhe para preservar a posição de rolagem
      Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(GalaxiaBackground)
        ) {

        // Cabeçalho
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = 24.dp,
                    top = 20.dp,
                    end = 16.dp,
                    bottom = 12.dp
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {

            Text(
                text = "GalaxIA",
                color = GalaxiaCyan,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            IconButton(
                onClick = { }
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = "Pesquisar",
                    tint = GalaxiaWhite,
                    modifier = Modifier.size(28.dp)
                )
            }
        }

        // Conteúdo
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            when (selectedTab) {
                0 -> Column(modifier = Modifier.fillMaxSize()) {
                    FeedFilterRow(
                        selectedFilter = feedFilter,
                        isLoading = apodState is FeedViewModel.ApodState.Loading,
                        onFilterSelected = { viewModel.setFeedFilter(it) }
                    )

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                    ) {
                        when (val state = apodState) {
                            is FeedViewModel.ApodState.Loading -> {
                                CircularProgressIndicator(
                                    modifier = Modifier.align(Alignment.Center),
                                    color = GalaxiaCyan
                                )
                            }
                            is FeedViewModel.ApodState.Error -> {
                                Column(
                                    modifier = Modifier
                                        .align(Alignment.Center)
                                        .padding(horizontal = 24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = state.message,
                                        color = Color.Red,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.padding(bottom = 16.dp)
                                    )
                                    Button(
                                        onClick = { viewModel.fetchApod() },
                                        colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                                    ) {
                                        Icon(Icons.Default.Refresh, contentDescription = null)
                                        Spacer(Modifier.size(8.dp))
                                        Text("Tentar Novamente", color = GalaxiaBackground)
                                    }
                                }
                            }
                            is FeedViewModel.ApodState.Success -> {
                                LazyColumn(
                                    modifier = Modifier.fillMaxSize(),
                                    contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 4.dp, bottom = 20.dp),
                                    verticalArrangement = Arrangement.spacedBy(16.dp)
                                ) {
                                    items(
                                        items = state.apods,
                                        key = { it.date }
                                    ) { item ->
                                        FeedCard(
                                            apod = item,
                                            isFavorite = "apod_${item.date}" in favoriteIds,
                                            onToggleFavorite = { viewModel.toggleFavoriteApod(item) },
                                            onClick = { detailTarget = DetailTarget(item.toFavoriteEntity(), item) }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                1 -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onOpenDetail = { apod ->
                            detailTarget = DetailTarget(apod.toFavoriteEntity(), apod)
                        }
                    )
                }
                2 -> {
                    FavoritesScreen(
                        viewModel = viewModel,
                        onOpenDetail = { favorite ->
                            detailTarget = DetailTarget(favorite)
                        }
                    )
                }
            }
        }

        // Barra inferior
        BottomNavigation(
            selectedTab = selectedTab,
            onTabSelected = { selectedTab = it }
        )
    }

        // Tela de detalhe da notícia, sobreposta com animação de deslize
        AnimatedContent(
            targetState = detailTarget,
            transitionSpec = {
                if (targetState != null) {
                    (slideInHorizontally { it } + fadeIn()) togetherWith fadeOut()
                } else {
                    fadeIn() togetherWith (slideOutHorizontally { it } + fadeOut())
                }
            },
            label = "NewsDetailTransition"
        ) { target ->
            if (target != null) {
                NewsDetailScreen(
                    item = target.item,
                    isFavorite = target.item.id in favoriteIds,
                    onToggleFavorite = {
                        when {
                            target.apod != null -> viewModel.toggleFavoriteApod(target.apod)
                            target.item.itemType == FavoriteType.APOD.name ->
                                viewModel.toggleFavoriteApod(target.item.toApodResponse())
                            target.item.id in favoriteIds -> viewModel.removeFavoriteById(target.item.id)
                        }
                    },
                    onBack = { detailTarget = null },
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
      }
}
}

/**
 * Item aberto na tela de detalhe. [apod] fica disponível quando o item veio do
 * Feed/Histórico, para que o favorito seja salvo com os dados originais da API.
 */
private data class DetailTarget(
    val item: FavoriteEntity,
    val apod: ApodResponse? = null
)

@Composable
private fun FeedFilterRow(
    selectedFilter: FeedViewModel.FeedFilter,
    isLoading: Boolean,
    onFilterSelected: (FeedViewModel.FeedFilter) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 12.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedViewModel.FeedFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = { Text(filter.label) },
                leadingIcon = {
                    Icon(
                        imageVector = when (filter) {
                            FeedViewModel.FeedFilter.LATEST -> Icons.Default.Schedule
                            FeedViewModel.FeedFilter.RANDOM -> Icons.Default.Shuffle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = GalaxiaCardBackground,
                    labelColor = GalaxiaGray,
                    iconColor = GalaxiaGray,
                    selectedContainerColor = GalaxiaCyan,
                    selectedLabelColor = GalaxiaBackground,
                    selectedLeadingIconColor = GalaxiaBackground
                ),
                border = null,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.padding(end = 8.dp)
            )
        }

        Spacer(modifier = Modifier.weight(1f))

        // No modo aleatório, permite sortear um novo conjunto de fotos
        if (selectedFilter == FeedViewModel.FeedFilter.RANDOM) {
            IconButton(
                onClick = { onFilterSelected(FeedViewModel.FeedFilter.RANDOM) },
                enabled = !isLoading
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Sortear outras fotos",
                    tint = if (isLoading) GalaxiaGray else GalaxiaCyan
                )
            }
        }
    }
}

@Composable
private fun FeedCard(
    apod: ApodResponse,
    isFavorite: Boolean = false,
    onToggleFavorite: () -> Unit = {},
    onClick: () -> Unit = {}
) {
    val uriHandler = LocalUriHandler.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(GalaxiaCardBackground)
            .clickable(onClick = onClick)
    ) {

        // Imagem ou Placeholder de Vídeo
        if (apod.mediaType == "image" && apod.displayImageUrl.isNotBlank()) {
            AsyncImage(
                model = apod.displayImageUrl,
                contentDescription = apod.alt ?: apod.displayTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(230.dp)
                    .background(Color(0xFF161822)),
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
                    text = apod.displayTitle,
                    color = GalaxiaWhite,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        Column(modifier = Modifier.padding(20.dp)) {
            // Data do Item
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = GalaxiaGray,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = formatDateBr(apod.date),
                    color = GalaxiaGray,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Título
            Text(
                text = apod.displayTitle,
                color = GalaxiaWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            // Autor / Copyright se houver
            apod.author?.let { copyright ->
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
                        fontSize = 12.sp,
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Texto da Explicação
            Text(
                text = apod.displayExplanation,
                color = GalaxiaGray,
                fontSize = 15.sp,
                lineHeight = 22.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Ações: Botão de Favorito e Link NASA
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorite) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(28.dp)
                    )
                }

                val targetUrl = apod.permalink ?: apod.url
                if (targetUrl.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                uriHandler.openUri(targetUrl)
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
        }
    }
}

@Composable
private fun BottomNavigation(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0B0C11))
            .navigationBarsPadding()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {

        BottomNavigationItem(
            icon = Icons.Default.Home,
            label = "Feed",
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) }
        )

        BottomNavigationItem(
            icon = Icons.Default.CalendarMonth,
            label = "Histórico",
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) }
        )

        BottomNavigationItem(
            icon = Icons.Default.FavoriteBorder,
            label = "Favoritos",
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) }
        )
    }
}

@Composable
private fun BottomNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {

    val color = if (selected) GalaxiaCyan else GalaxiaWhite

    Column(
        modifier = Modifier
            .clickable(onClick = onClick)
            .padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(28.dp)
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = label,
            color = color,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeedCardPreview() {
    FeedCard(
        apod = ApodResponse(
            date = "2026-10-24",
            explanation = "Uma das nebulosas mais brilhantes do céu no turno, localizada logo ao sul do cinturão de Órion.",
            hdurl = null,
            mediaType = "image",
            serviceVersion = "v1",
            title = "A Nebulosa de Órion",
            url = "https://example.com/image.jpg"
        )
    )
}
