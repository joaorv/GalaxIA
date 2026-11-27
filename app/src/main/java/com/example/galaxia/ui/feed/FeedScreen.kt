package com.example.galaxia.ui.feed

import com.example.galaxia.ui.detail.NewsDetailScreen
import com.example.galaxia.ui.history.HistoryScreen
import com.example.galaxia.ui.favorites.FavoritesScreen
import com.example.galaxia.ui.news.SpaceNewsScreen
import com.example.galaxia.ui.search.SearchScreen
import com.example.galaxia.ui.settings.SettingsScreen
import com.example.galaxia.data.model.toFavoriteEntity
import com.example.galaxia.viewmodel.SettingsViewModel
import com.example.galaxia.viewmodel.SpaceNewsViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing

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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Copyright
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Newspaper
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import com.example.galaxia.ui.components.CollapsibleHeaderContainer
import com.example.galaxia.ui.components.FeedSkeletonList
import com.example.galaxia.ui.components.ScrollToTopButton
import com.example.galaxia.ui.components.VideoMediaCard
import com.example.galaxia.ui.components.rememberCollapsibleHeaderState
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.runtime.derivedStateOf
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.R
import com.example.galaxia.data.local.NasaApiKeyManager
import com.example.galaxia.ui.components.ApiKeyDialog
import com.example.galaxia.ui.components.OptimizedAsyncImage
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.toApodResponse
import com.example.galaxia.data.local.toFavoriteEntity
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaBottomBarBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaSurfaceElevated
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.theme.LocalGalaxiaColors
import com.example.galaxia.ui.theme.LocalUiMetrics
import com.example.galaxia.ui.theme.responsiveContentWidth
import com.example.galaxia.ui.util.formatDateBr
import com.example.galaxia.viewmodel.FeedViewModel
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedScreen(
    viewModel: FeedViewModel = viewModel(),
    spaceNewsViewModel: SpaceNewsViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
) {
    val apodState by viewModel.apodState.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val favoriteIds by viewModel.favoriteIds.collectAsState()
    val feedFilter by viewModel.feedFilter.collectAsState()
    val userPreferences by settingsViewModel.preferences.collectAsState()
    val metrics = LocalUiMetrics.current

    val tabPagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })
    val coroutineScope = rememberCoroutineScope()
    val headerState = rememberCollapsibleHeaderState()
    val apodListState = rememberLazyListState()
    val showApodScrollToTop by remember {
        derivedStateOf { apodListState.firstVisibleItemIndex >= 2 }
    }

    val apiKeyManager = remember { NasaApiKeyManager.getInstance(GalaxiaApplication.instance) }
    val shouldShowApiKeyPrompt by apiKeyManager.shouldShowOnboardingPrompt.collectAsState()
    val isUsingCustomKey by apiKeyManager.isUsingCustomKey.collectAsState()
    val savedCustomKey by apiKeyManager.customApiKey.collectAsState()
    var showApiKeyDialog by remember { mutableStateOf(false) }
    var detailTarget by remember { mutableStateOf<DetailTarget?>(null) }
    var showSearch by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Seta "voltar" do sistema fecha detalhe, configurações ou pesquisa antes de sair do app
    BackHandler(enabled = detailTarget != null || showSettings || showSearch) {
        when {
            detailTarget != null -> detailTarget = null
            showSettings -> showSettings = false
            else -> showSearch = false
        }
    }

    LaunchedEffect(tabPagerState.currentPage) {
        headerState.expand()
    }

    LaunchedEffect(viewModel, spaceNewsViewModel) {
        merge(viewModel.userMessage, spaceNewsViewModel.userMessage).collectLatest { message ->
            snackbarHostState.currentSnackbarData?.dismiss()
            coroutineScope {
                val showJob = launch {
                    snackbarHostState.showSnackbar(
                        message = message,
                        duration = SnackbarDuration.Indefinite
                    )
                }
                delay(1800.milliseconds)
                showJob.cancel()
                snackbarHostState.currentSnackbarData?.dismiss()
            }
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
        snackbarHost = {
            SnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.navigationBarsPadding()
            ) { data ->
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

        // Cabeçalho dinâmico (estilo Twitter/X — exceto na aba Histórico)
        CollapsibleHeaderContainer(state = headerState) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = metrics.horizontalPadding,
                        top = 8.dp,
                        end = (metrics.horizontalPadding - 8.dp).coerceAtLeast(4.dp),
                        bottom = 6.dp
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "GalaxIA",
                    color = GalaxiaCyan,
                    fontSize = if (metrics.isCompactScreen) 24.sp else 26.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { showSearch = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Pesquisar",
                            tint = GalaxiaWhite,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    IconButton(
                        onClick = { showSettings = true }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações",
                            tint = GalaxiaWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }

        // Conteúdo com navegação por deslize entre as abas principais
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            HorizontalPager(
                state = tabPagerState,
                beyondViewportPageCount = 0,
                userScrollEnabled = detailTarget == null && !showSearch && !showSettings,
                modifier = Modifier.fillMaxSize()
            ) { page ->
                when (page) {
                    0 -> {
                        val apodNestedScroll = remember(headerState, apodListState, apodState) {
                            headerState.createNestedScrollConnection(
                                scope = coroutineScope,
                                canCollapse = {
                                    apodState is FeedViewModel.ApodState.Success &&
                                        (apodListState.canScrollForward || apodListState.canScrollBackward)
                                },
                                isAtTop = {
                                    apodListState.firstVisibleItemIndex == 0 &&
                                        apodListState.firstVisibleItemScrollOffset == 0
                                }
                            )
                        }
                        val pullToRefreshState = rememberPullToRefreshState()

                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .nestedScroll(apodNestedScroll)
                        ) {
                            FeedFilterRow(
                                selectedFilter = feedFilter,
                                onFilterSelected = { viewModel.setFeedFilter(it) }
                            )

                            PullToRefreshBox(
                                isRefreshing = isRefreshing,
                                onRefresh = { viewModel.refreshApod() },
                                state = pullToRefreshState,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                indicator = {
                                    PullToRefreshDefaults.Indicator(
                                        state = pullToRefreshState,
                                        isRefreshing = isRefreshing,
                                        containerColor = GalaxiaCardBackground,
                                        color = GalaxiaCyan,
                                        modifier = Modifier.align(Alignment.TopCenter)
                                    )
                                }
                            ) {
                                when (val state = apodState) {
                                    is FeedViewModel.ApodState.Loading -> {
                                        FeedSkeletonList(
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                    is FeedViewModel.ApodState.Error -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .verticalScroll(rememberScrollState()),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(horizontal = 24.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally
                                            ) {
                                                Text(
                                                    text = state.message,
                                                    color = Color(0xFFFF6B6B),
                                                    textAlign = TextAlign.Center,
                                                    modifier = Modifier.padding(bottom = 16.dp)
                                                )
                                                Button(
                                                    onClick = { viewModel.fetchApod() },
                                                    colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                                                ) {
                                                    Icon(Icons.Default.Refresh, contentDescription = null, tint = GalaxiaBackground)
                                                    Spacer(Modifier.size(8.dp))
                                                    Text("Tentar Novamente", color = GalaxiaBackground)
                                                }
                                            }
                                        }
                                    }
                                    is FeedViewModel.ApodState.Success -> {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.TopCenter
                                        ) {
                                            LazyColumn(
                                                state = apodListState,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .responsiveContentWidth(metrics.maxContentWidth),
                                                contentPadding = PaddingValues(
                                                    start = metrics.horizontalPadding,
                                                    end = metrics.horizontalPadding,
                                                    top = 4.dp,
                                                    bottom = 16.dp
                                                ),
                                                verticalArrangement = Arrangement.spacedBy(metrics.cardSpacing)
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

                                            ScrollToTopButton(
                                                visible = showApodScrollToTop,
                                                onClick = {
                                                    coroutineScope.launch {
                                                        apodListState.animateScrollToItem(0)
                                                        headerState.expand()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .align(Alignment.BottomEnd)
                                                    .padding(end = 16.dp, bottom = 16.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                1 -> {
                    SpaceNewsScreen(
                        favoriteIds = favoriteIds,
                        onToggleFavorite = { item ->
                            viewModel.toggleFavoriteItem(item.toFavoriteEntity())
                        },
                        onOpenDetail = { item, pageIndex ->
                            detailTarget = DetailTarget(
                                item = item.toFavoriteEntity(),
                                initialImageIndex = pageIndex
                            )
                        },
                        headerState = headerState,
                        newsViewModel = spaceNewsViewModel
                    )
                }
                2 -> {
                    HistoryScreen(
                        viewModel = viewModel,
                        onOpenDetail = { apod ->
                            detailTarget = DetailTarget(apod.toFavoriteEntity(), apod)
                        }
                    )
                }
                3 -> {
                    FavoritesScreen(
                        viewModel = viewModel,
                        headerState = headerState,
                        onOpenDetail = { favorite ->
                            detailTarget = DetailTarget(favorite)
                        }
                    )
                }
                }
            }
        }

        // Barra inferior
        BottomNavigation(
            selectedTab = tabPagerState.currentPage,
            onTabSelected = { targetTab ->
                coroutineScope.launch {
                    tabPagerState.animateScrollToPage(targetTab)
                }
            }
        )
    }

        // Tela de pesquisa, sobreposta ao feed; o detalhe abre por cima dela
        AnimatedVisibility(
            visible = showSearch,
            enter = slideInVertically { it / 8 } + fadeIn(),
            exit = slideOutVertically { it / 8 } + fadeOut()
        ) {
            SearchScreen(
                favoriteIds = favoriteIds,
                onToggleFavorite = { item ->
                    if (item.apod != null) {
                        viewModel.toggleFavoriteApod(item.apod)
                    } else {
                        viewModel.toggleFavoriteItem(item.toFavoriteEntity())
                    }
                },
                onOpenDetail = { item ->
                    detailTarget = DetailTarget(item.toFavoriteEntity(), item.apod)
                },
                onBack = { showSearch = false },
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
                    .navigationBarsPadding()
            )
        }

        // Tela de Configurações
        AnimatedVisibility(
            visible = showSettings,
            enter = slideInHorizontally { it } + fadeIn(),
            exit = slideOutHorizontally { it } + fadeOut()
        ) {
            SettingsScreen(
                preferences = userPreferences,
                isUsingCustomApiKey = isUsingCustomKey,
                onSelectThemeMode = settingsViewModel::updateThemeMode,
                onSelectFontSize = settingsViewModel::updateFontSize,
                onSelectCardSize = settingsViewModel::updateCardSize,
                onOpenApiKeyDialog = { showApiKeyDialog = true },
                onBack = { showSettings = false },
                modifier = Modifier
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding)
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
                    initialImageIndex = target.initialImageIndex,
                    onToggleFavorite = {
                        when {
                            target.apod != null -> viewModel.toggleFavoriteApod(target.apod)
                            target.item.itemType == FavoriteType.APOD.name ->
                                viewModel.toggleFavoriteApod(target.item.toApodResponse())
                            else -> viewModel.toggleFavoriteItem(target.item)
                        }
                    },
                    onBack = { detailTarget = null },
                    modifier = Modifier
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .navigationBarsPadding()
                )
            }
        }

        if (shouldShowApiKeyPrompt || showApiKeyDialog) {
            ApiKeyDialog(
                currentCustomKey = savedCustomKey,
                isUsingCustomKey = isUsingCustomKey,
                onSaveKey = { key ->
                    if (apiKeyManager.saveApiKey(key)) {
                        showApiKeyDialog = false
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar(
                                message = "Chave de API da NASA salva!",
                                duration = SnackbarDuration.Short
                            )
                        }
                        if (apodState is FeedViewModel.ApodState.Error) {
                            viewModel.fetchApod()
                        }
                    }
                },
                onUseDemoKey = {
                    apiKeyManager.useDemoKey()
                    showApiKeyDialog = false
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar(
                            message = "Usando DEMO_KEY padrão",
                            duration = SnackbarDuration.Short
                        )
                    }
                },
                onDismiss = {
                    apiKeyManager.dismissPrompt()
                    showApiKeyDialog = false
                }
            )
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
    val apod: ApodResponse? = null,
    val initialImageIndex: Int = 0
)

@Composable
private fun FeedFilterRow(
    selectedFilter: FeedViewModel.FeedFilter,
    onFilterSelected: (FeedViewModel.FeedFilter) -> Unit
) {
    val metrics = LocalUiMetrics.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                bottom = 6.dp
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        FeedViewModel.FeedFilter.entries.forEach { filter ->
            FilterChip(
                selected = filter == selectedFilter,
                onClick = { onFilterSelected(filter) },
                label = {
                    Text(
                        text = filter.label,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
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
    val metrics = LocalUiMetrics.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.cardCornerRadius))
            .background(GalaxiaCardBackground)
            .clickable(onClick = onClick)
    ) {

        // Imagem ou Vídeo / iFrame ou Placeholder
        val isVideoContent = apod.mediaType == "video" || apod.mediaType == "iframe" || apod.mediaType == "embed"
        if (isVideoContent) {
            VideoMediaCard(
                mediaUrl = apod.effectiveMediaUrl,
                thumbnailUrl = apod.displayThumbnailUrl,
                title = apod.displayTitle,
                permalinkUrl = apod.officialNasaUrl,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.cardMediaHeight)
            )
        } else if (apod.mediaType == "image" && apod.displayImageUrl.isNotBlank()) {
            OptimizedAsyncImage(
                lowResUrl = apod.lowResImageUrl,
                highResUrl = apod.highResImageUrl,
                contentDescription = apod.alt ?: apod.displayTitle,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(metrics.cardMediaHeight)
                    .background(GalaxiaSurfaceElevated),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height((metrics.cardMediaHeight - 30.dp).coerceAtLeast(120.dp))
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = apod.displayTitle,
                    color = Color.White,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        Column(modifier = Modifier.padding(metrics.cardContentPadding)) {
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

            Spacer(modifier = Modifier.height(8.dp))

            // Título
            Text(
                text = apod.displayTitle,
                color = GalaxiaWhite,
                fontSize = 19.sp,
                lineHeight = 25.sp,
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Texto da Explicação
            Text(
                text = apod.displayExplanation,
                color = GalaxiaGray,
                fontSize = 14.sp,
                lineHeight = 21.sp,
                maxLines = metrics.cardDescriptionMaxLines,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Ações: Botão de Favorito e Link NASA
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorite) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(26.dp)
                    )
                }

                val targetUrl = apod.officialNasaUrl
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

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(GalaxiaBottomBarBackground)
    ) {
        if (!LocalGalaxiaColors.current.isDark) {
            HorizontalDivider(color = GalaxiaGray.copy(alpha = 0.18f), thickness = 1.dp)
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {

        BottomNavigationItem(
            icon = ImageVector.vectorResource(R.drawable.ic_telescope),
            label = "APOD",
            selected = selectedTab == 0,
            onClick = { onTabSelected(0) },
            modifier = Modifier.weight(1f)
        )

        BottomNavigationItem(
            icon = Icons.Default.Newspaper,
            label = "Notícias",
            selected = selectedTab == 1,
            onClick = { onTabSelected(1) },
            modifier = Modifier.weight(1f)
        )

        BottomNavigationItem(
            icon = Icons.Default.CalendarMonth,
            label = "Histórico",
            selected = selectedTab == 2,
            onClick = { onTabSelected(2) },
            modifier = Modifier.weight(1f)
        )

        BottomNavigationItem(
            icon = Icons.Default.FavoriteBorder,
            label = "Favoritos",
            selected = selectedTab == 3,
            onClick = { onTabSelected(3) },
            modifier = Modifier.weight(1f)
        )
        }
    }
}

@Composable
private fun BottomNavigationItem(
    icon: ImageVector,
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {

    val color = if (selected) GalaxiaCyan else GalaxiaWhite

    Column(
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = color,
            modifier = Modifier.size(24.dp)
        )

        Spacer(modifier = Modifier.height(3.dp))

        Text(
            text = label,
            color = color,
            fontSize = 11.sp,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
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
