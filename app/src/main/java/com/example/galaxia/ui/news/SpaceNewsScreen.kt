package com.example.galaxia.ui.news

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.NewsSortOrder
import com.example.galaxia.data.model.SpaceNewsItem
import com.example.galaxia.data.model.subTopics
import com.example.galaxia.ui.components.CollapsibleHeaderContainer
import com.example.galaxia.ui.components.CollapsibleHeaderState
import com.example.galaxia.ui.components.OptimizedAsyncImage
import com.example.galaxia.ui.components.ScrollToTopButton
import com.example.galaxia.ui.components.SpaceNewsSkeletonList
import com.example.galaxia.ui.components.rememberCollapsibleHeaderState
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaSurfaceElevated
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.theme.LocalUiMetrics
import com.example.galaxia.ui.theme.responsiveContentWidth
import com.example.galaxia.ui.util.formatDateBr
import com.example.galaxia.viewmodel.SpaceNewsViewModel
import com.example.galaxia.viewmodel.SpaceNewsViewModel.SpaceNewsUiState
import kotlinx.coroutines.launch

@Composable
fun SpaceNewsScreen(
    favoriteIds: Set<String>,
    onToggleFavorite: (SpaceNewsItem) -> Unit,
    onOpenDetail: (SpaceNewsItem, Int) -> Unit,
    headerState: CollapsibleHeaderState,
    newsViewModel: SpaceNewsViewModel = viewModel()
) {
    val selectedCategory by newsViewModel.selectedCategory.collectAsState()
    val selectedSubTopics by newsViewModel.selectedSubTopics.collectAsState()
    val selectedSortOrder by newsViewModel.selectedSortOrder.collectAsState()
    val uiState by newsViewModel.uiState.collectAsState()
    val categoryStates by newsViewModel.categoryStates.collectAsState()
    val isRefreshing by newsViewModel.isRefreshing.collectAsState()
    val metrics = LocalUiMetrics.current

    val categories = NewsCategory.entries
    val categoryPagerState = rememberPagerState(
        initialPage = selectedCategory.ordinal,
        pageCount = { categories.size }
    )
    val categoryChipsState = rememberLazyListState()
    val subFiltersHeaderState = rememberCollapsibleHeaderState()
    val coroutineScope = rememberCoroutineScope()

    val displayedCategory = categories[categoryPagerState.currentPage]
    val currentSubTopics = displayedCategory.subTopics
    val activeSubTopic = selectedSubTopics[displayedCategory] ?: currentSubTopics.firstOrNull()

    LaunchedEffect(categoryPagerState.currentPage) {
        subFiltersHeaderState.expand()
    }

    LaunchedEffect(categoryPagerState.settledPage) {
        val pageCategory = categories[categoryPagerState.settledPage]
        if (pageCategory != selectedCategory) {
            newsViewModel.selectCategory(pageCategory)
        } else {
            newsViewModel.ensureLoaded()
        }
        categoryChipsState.animateScrollToItem(categoryPagerState.settledPage)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(GalaxiaBackground)
    ) {
        // Cabeçalho compacto da seção (atualização via Pull to Refresh)
        Text(
            text = "Notícias e Curiosidades",
            color = GalaxiaWhite,
            fontSize = if (metrics.isCompactScreen) 18.sp else 20.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(
                start = metrics.horizontalPadding,
                end = metrics.horizontalPadding,
                top = 2.dp,
                bottom = 6.dp
            )
        )

        // Chips de Categoria principal sincronizados com o deslize
        LazyRow(
            state = categoryChipsState,
            contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(bottom = if (currentSubTopics.isNotEmpty()) 4.dp else 8.dp)
        ) {
            itemsIndexed(categories) { index, category ->
                val selected = index == categoryPagerState.currentPage
                FilterChip(
                    selected = selected,
                    onClick = {
                        coroutineScope.launch {
                            categoryPagerState.animateScrollToPage(index)
                        }
                        newsViewModel.selectCategory(category)
                    },
                    label = { Text(category.label) },
                    leadingIcon = {
                        Icon(
                            imageVector = categoryIcon(category),
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
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }

        // Chips secundários de temas + ordenação (Mais recentes / Mais antigas)
        if (currentSubTopics.isNotEmpty()) {
            CollapsibleHeaderContainer(state = subFiltersHeaderState) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
                    ) {
                        items(currentSubTopics, key = { it.id }) { subTopic ->
                            val isSubSelected = subTopic.id == activeSubTopic?.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(
                                        if (isSubSelected) GalaxiaCyan.copy(alpha = 0.2f) else GalaxiaCardBackground
                                    )
                                    .clickable { newsViewModel.selectSubTopic(subTopic) }
                                    .padding(horizontal = 11.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = subTopic.label,
                                    color = if (isSubSelected) GalaxiaCyan else GalaxiaGray,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSubSelected) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }
                    }

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = metrics.horizontalPadding),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        items(NewsSortOrder.entries, key = { it.name }) { order ->
                            val isSelected = selectedSortOrder == order
                            FilterChip(
                                selected = isSelected,
                                onClick = { newsViewModel.selectSortOrder(order) },
                                label = {
                                    Text(
                                        text = order.label,
                                        fontSize = 12.sp
                                    )
                                },
                                leadingIcon = {
                                    Icon(
                                        imageVector = when (order) {
                                            NewsSortOrder.NEWEST -> Icons.Default.ArrowDownward
                                            NewsSortOrder.OLDEST -> Icons.Default.ArrowUpward
                                        },
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                },
                                colors = FilterChipDefaults.filterChipColors(
                                    containerColor = GalaxiaCardBackground,
                                    labelColor = GalaxiaGray,
                                    iconColor = GalaxiaGray,
                                    selectedContainerColor = GalaxiaCyan.copy(alpha = 0.2f),
                                    selectedLabelColor = GalaxiaCyan,
                                    selectedLeadingIconColor = GalaxiaCyan
                                ),
                                border = null,
                                shape = RoundedCornerShape(10.dp)
                            )
                        }
                    }
                }
            }
        }

        // Conteúdo com navegação por deslize entre categorias
        HorizontalPager(
            state = categoryPagerState,
            beyondViewportPageCount = 0,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { pageIndex ->
            val pageCategory = categories[pageIndex]
            val pageState = categoryStates[pageCategory]
                ?: if (pageCategory == selectedCategory) uiState else SpaceNewsUiState.Loading
            val activeSubTopicId = selectedSubTopics[pageCategory]?.id

            CategoryNewsPage(
                pageCategory = pageCategory,
                pageState = pageState,
                isActiveCategory = pageCategory == selectedCategory,
                isRefreshing = isRefreshing && pageCategory == selectedCategory,
                activeSubTopicId = activeSubTopicId,
                selectedSortOrder = selectedSortOrder,
                favoriteIds = favoriteIds,
                headerState = headerState,
                subFiltersHeaderState = subFiltersHeaderState,
                onRefresh = { newsViewModel.refresh() },
                onLoadNextPage = { newsViewModel.loadNextPage() },
                onToggleFavorite = onToggleFavorite,
                onOpenDetail = onOpenDetail
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CategoryNewsPage(
    pageCategory: NewsCategory,
    pageState: SpaceNewsUiState,
    isActiveCategory: Boolean,
    isRefreshing: Boolean,
    activeSubTopicId: String?,
    selectedSortOrder: NewsSortOrder,
    favoriteIds: Set<String>,
    headerState: CollapsibleHeaderState,
    subFiltersHeaderState: CollapsibleHeaderState,
    onRefresh: () -> Unit,
    onLoadNextPage: () -> Unit,
    onToggleFavorite: (SpaceNewsItem) -> Unit,
    onOpenDetail: (SpaceNewsItem, Int) -> Unit
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    val metrics = LocalUiMetrics.current

    LaunchedEffect(activeSubTopicId, selectedSortOrder) {
        if (isActiveCategory) {
            listState.scrollToItem(0)
        }
    }

    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 2 }
    }

    val shouldLoadMore by remember(pageState, isActiveCategory) {
        derivedStateOf {
            if (!isActiveCategory) return@derivedStateOf false
            val success = pageState as? SpaceNewsUiState.Success ?: return@derivedStateOf false
            if (!success.hasMore || success.isLoadingMore) return@derivedStateOf false
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            val total = listState.layoutInfo.totalItemsCount
            total > 0 && lastVisible >= total - 3
        }
    }

    val itemCount = (pageState as? SpaceNewsUiState.Success)?.items?.size ?: 0

    LaunchedEffect(shouldLoadMore, itemCount) {
        if (shouldLoadMore) {
            onLoadNextPage()
        }
    }

    val hasSubFilters = pageCategory.subTopics.isNotEmpty()
    val isSuccessState = pageState is SpaceNewsUiState.Success

    LaunchedEffect(isActiveCategory, isSuccessState) {
        if (isActiveCategory && !isSuccessState) {
            subFiltersHeaderState.expand()
        }
    }

    val newsNestedScroll = remember(headerState, listState, pageState, isActiveCategory) {
        headerState.createNestedScrollConnection(
            scope = coroutineScope,
            canCollapse = {
                isActiveCategory &&
                    pageState is SpaceNewsUiState.Success &&
                    (listState.canScrollForward || listState.canScrollBackward)
            },
            isAtTop = {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            }
        )
    }
    val subFiltersNestedScroll = remember(subFiltersHeaderState, listState, pageState, isActiveCategory, hasSubFilters) {
        subFiltersHeaderState.createNestedScrollConnection(
            scope = coroutineScope,
            canCollapse = {
                hasSubFilters &&
                    isActiveCategory &&
                    pageState is SpaceNewsUiState.Success &&
                    (listState.canScrollForward || listState.canScrollBackward)
            },
            isAtTop = {
                listState.firstVisibleItemIndex == 0 && listState.firstVisibleItemScrollOffset == 0
            }
        )
    }
    val pullToRefreshState = rememberPullToRefreshState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(newsNestedScroll)
            .then(if (hasSubFilters) Modifier.nestedScroll(subFiltersNestedScroll) else Modifier)
    ) {
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            state = pullToRefreshState,
            modifier = Modifier.fillMaxSize(),
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
            when (pageState) {
                is SpaceNewsUiState.Loading -> {
                    SpaceNewsSkeletonList(modifier = Modifier.fillMaxSize())
                }
                is SpaceNewsUiState.Error -> {
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
                                text = pageState.message,
                                color = GalaxiaGray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(
                                onClick = onRefresh,
                                colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, tint = GalaxiaBackground)
                                Spacer(Modifier.size(8.dp))
                                Text("Tentar Novamente", color = GalaxiaBackground)
                            }
                        }
                    }
                }
                is SpaceNewsUiState.Empty -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            modifier = Modifier.padding(horizontal = 32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = categoryIcon(pageCategory),
                                contentDescription = null,
                                tint = GalaxiaGray.copy(alpha = 0.6f),
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Nenhum conteúdo disponível em ${pageCategory.label} no momento.",
                                color = GalaxiaGray,
                                fontSize = 15.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
                is SpaceNewsUiState.Success -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        LazyColumn(
                            state = listState,
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
                            if (pageState.isFromOfflineCache) {
                                item(key = "offline_banner") {
                                    OfflineCacheBanner()
                                }
                            }

                            itemsIndexed(
                                items = pageState.items,
                                key = { _, item -> item.id }
                            ) { index, item ->
                                val isHero = index == 0 &&
                                    item.category == NewsCategory.NASA_HIGHLIGHTS &&
                                    !item.imageUrl.isNullOrBlank()

                                SpaceNewsCard(
                                    item = item,
                                    isHero = isHero,
                                    isFavorite = item.id in favoriteIds,
                                    onToggleFavorite = { onToggleFavorite(item) },
                                    onClick = { selectedItem, pageIndex ->
                                        onOpenDetail(selectedItem, pageIndex)
                                    }
                                )
                            }

                            if (pageState.isLoadingMore) {
                                item(key = "loading_more") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        CircularProgressIndicator(
                                            color = GalaxiaCyan,
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 2.5.dp
                                        )
                                    }
                                }
                            } else if (pageState.hasMore) {
                                item(key = "load_more_button") {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        TextButton(onClick = onLoadNextPage) {
                                            Text(
                                                text = "Carregar mais",
                                                color = GalaxiaCyan,
                                                fontWeight = FontWeight.SemiBold
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        ScrollToTopButton(
                            visible = showScrollToTop,
                            onClick = {
                                coroutineScope.launch {
                                    listState.animateScrollToItem(0)
                                    launch { headerState.expand() }
                                    launch { subFiltersHeaderState.expand() }
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

@Composable
private fun OfflineCacheBanner() {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GalaxiaCyan.copy(alpha = 0.12f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CloudOff,
            contentDescription = null,
            tint = GalaxiaCyan,
            modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = "Exibindo conteúdo em cache (sem conexão)",
            color = GalaxiaWhite,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SpaceNewsCard(
    item: SpaceNewsItem,
    isHero: Boolean,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: (SpaceNewsItem, Int) -> Unit
) {
    val uriHandler = LocalUriHandler.current
    val metrics = LocalUiMetrics.current
    val accent = accentColorFor(item)
    val badgeIcon = iconForItem(item)
    val images = item.allImageUrls
    val pagerState = if (images.size > 1) rememberPagerState(pageCount = { images.size }) else null
    val currentPage = pagerState?.currentPage ?: 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(metrics.cardCornerRadius))
            .background(GalaxiaCardBackground)
            .clickable { onClick(item, currentPage) }
    ) {
        if (images.isNotEmpty()) {
            val imageHeight = if (isHero) metrics.heroMediaHeight else metrics.cardMediaHeight
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(imageHeight)
            ) {
                if (pagerState == null) {
                    OptimizedAsyncImage(
                        lowResUrl = images.first(),
                        highResUrl = null,
                        contentDescription = item.displayTitle,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(GalaxiaSurfaceElevated),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        OptimizedAsyncImage(
                            lowResUrl = images[page],
                            highResUrl = null,
                            contentDescription = "${item.displayTitle} (${page + 1} de ${images.size})",
                            modifier = Modifier
                                .fillMaxSize()
                                .background(GalaxiaSurfaceElevated),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Contador de fotos no canto superior direito
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(12.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.65f))
                            .padding(horizontal = 9.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${pagerState.currentPage + 1} / ${images.size}",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }

                    // Indicadores de página na base da foto
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
                        repeat(images.size) { idx ->
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

                if (isHero) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0xCC0D0E13)),
                                    startY = 220f
                                )
                            )
                    )
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(14.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(GalaxiaCyan)
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "DESTAQUE NASA",
                            color = GalaxiaBackground,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                    }
                }
            }
        } else {
            // Faixa superior discreta para alertas sem imagem (DONKI)
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .background(accent.copy(alpha = 0.85f))
            )
        }

        Column(modifier = Modifier.padding(metrics.cardContentPadding)) {
            // Linha de categoria e data
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(accent.copy(alpha = 0.16f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = null,
                            tint = accent,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(accent.copy(alpha = 0.14f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = item.category.label.uppercase(),
                            color = accent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if (item.date.isNotBlank()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = GalaxiaGray,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDateBr(item.date),
                            color = GalaxiaGray,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Título
            Text(
                text = item.displayTitle,
                color = GalaxiaWhite,
                fontSize = if (isHero) 21.sp else 18.sp,
                lineHeight = if (isHero) 27.sp else 24.sp,
                fontWeight = FontWeight.Bold
            )

            // Subtítulo explicativo casual (DONKI) ou crédito da fonte
            val casualExplanation = item.subtitleTag
                ?.takeIf { item.category == NewsCategory.SPACE_WEATHER }
            if (!casualExplanation.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = casualExplanation,
                    color = accent.copy(alpha = 0.9f),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )
            } else {
                val credit = item.authorOrCredit?.takeIf { it.isNotBlank() } ?: item.sourceName
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = credit,
                    color = GalaxiaGray.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Pills de métricas e dados factuais estruturados
            val chips = item.safeMetadataChips
            if (chips.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    chips.forEach { chip ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(GalaxiaSurfaceElevated)
                                .padding(horizontal = 9.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = chip,
                                color = GalaxiaWhite.copy(alpha = 0.85f),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Descrição resumida (apenas quando disponível)
            val summary = item.displaySummary
            if (summary.isNotBlank()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = summary,
                    color = GalaxiaGray,
                    fontSize = 14.sp,
                    lineHeight = 21.sp,
                    maxLines = if (isHero) metrics.cardDescriptionMaxLines + 1 else metrics.cardDescriptionMaxLines,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Ações: Favorito e Acessar fonte
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                        tint = if (isFavorite) GalaxiaCyan else GalaxiaGray,
                        modifier = Modifier.size(26.dp)
                    )
                }

                if (item.sourceUrl.isNotBlank()) {
                    TextButton(
                        onClick = {
                            try {
                                uriHandler.openUri(item.sourceUrl)
                            } catch (_: Exception) {}
                        }
                    ) {
                        Text(
                            text = "Acessar fonte",
                            color = GalaxiaCyan,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = "Acessar fonte original",
                            tint = GalaxiaCyan,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

private fun categoryIcon(category: NewsCategory): ImageVector = when (category) {
    NewsCategory.NASA_HIGHLIGHTS -> Icons.AutoMirrored.Filled.Article
    NewsCategory.MISSIONS_DISCOVERIES -> Icons.Default.RocketLaunch
    NewsCategory.UNIVERSE_CURIOSITIES -> Icons.Default.AutoAwesome
    NewsCategory.SPACE_WEATHER -> Icons.Default.WbSunny
}

private fun iconForItem(item: SpaceNewsItem): ImageVector = when (item.accentKey) {
    "gst" -> Icons.Default.Bolt
    "flr", "cme", "sep", "ips", "hss" -> Icons.Default.WbSunny
    else -> categoryIcon(item.category)
}

@Composable
private fun accentColorFor(item: SpaceNewsItem): Color = when (item.accentKey) {
    "flr", "cme", "sep" -> Color(0xFFFFB454)
    "gst", "rbe", "mpc" -> Color(0xFFBD93F9)
    else -> GalaxiaCyan
}
