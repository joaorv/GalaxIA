package com.example.galaxia.ui.search

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxia.data.model.GlobalContentType
import com.example.galaxia.data.model.GlobalSearchItem
import com.example.galaxia.ui.components.OptimizedAsyncImage
import com.example.galaxia.ui.components.SearchResultCardSkeleton
import com.example.galaxia.ui.components.SearchResultSkeletonList
import com.example.galaxia.ui.theme.GalaxiaBackground
import com.example.galaxia.ui.theme.GalaxiaCardBackground
import com.example.galaxia.ui.theme.GalaxiaCyan
import com.example.galaxia.ui.theme.GalaxiaGray
import com.example.galaxia.ui.theme.GalaxiaWhite
import com.example.galaxia.ui.util.formatDateBr
import com.example.galaxia.viewmodel.SearchViewModel
import com.example.galaxia.viewmodel.SearchViewModel.SearchState
import java.text.NumberFormat
import java.util.Locale

private val SUGGESTIONS = listOf(
    "Nebulosa de Órion",
    "Aurora boreal",
    "Saturno",
    "Explosão solar",
    "Via Láctea",
    "Buraco negro",
    "James Webb",
    "Marte",
    "Incêndio florestal",
    "Vulcão"
)

/**
 * Tela de Pesquisa Global integrada ao acervo oficial da NASA:
 * NASA Image & Video Library, APOD, DONKI (clima espacial) e EONET (eventos naturais).
 */
@Composable
fun SearchScreen(
    favoriteIds: Set<String>,
    onToggleFavorite: (GlobalSearchItem) -> Unit,
    onOpenDetail: (GlobalSearchItem) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel()
) {
    val query by searchViewModel.query.collectAsState()
    val state by searchViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Abre com o teclado pronto apenas quando não há pesquisa ativa
    LaunchedEffect(Unit) {
        if (query.isBlank()) focusRequester.requestFocus()
    }

    Surface(
        modifier = modifier.fillMaxSize(),
        color = GalaxiaBackground
    ) {
        Column(modifier = Modifier.fillMaxSize().imePadding()) {
            SearchBar(
                query = query,
                onQueryChange = searchViewModel::onQueryChange,
                onSearch = {
                    searchViewModel.submit()
                    focusManager.clearFocus()
                },
                onClear = {
                    searchViewModel.clear()
                    focusRequester.requestFocus()
                },
                onBack = onBack,
                focusRequester = focusRequester
            )

            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when (val current = state) {
                    is SearchState.Idle -> IdleContent(
                        onSuggestionClick = { suggestion ->
                            searchViewModel.submit(suggestion)
                            focusManager.clearFocus()
                        }
                    )

                    is SearchState.Loading -> SearchResultSkeletonList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                    )

                    is SearchState.Empty -> MessageContent(
                        icon = Icons.Default.SearchOff,
                        title = "Nenhum resultado para “${current.query}”",
                        message = "Tente outro termo, como o nome de um planeta, missão, fenômeno solar ou evento natural.",
                        modifier = Modifier.align(Alignment.Center)
                    )

                    is SearchState.Error -> MessageContent(
                        icon = Icons.Default.SearchOff,
                        title = current.message,
                        messageColor = Color.Red,
                        modifier = Modifier.align(Alignment.Center),
                        action = {
                            Button(
                                onClick = searchViewModel::retry,
                                colors = ButtonDefaults.buttonColors(containerColor = GalaxiaCyan)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null)
                                Spacer(Modifier.size(8.dp))
                                Text("Tentar novamente", color = GalaxiaBackground)
                            }
                        }
                    )

                    is SearchState.Results -> ResultsContent(
                        state = current,
                        favoriteIds = favoriteIds,
                        onToggleFavorite = onToggleFavorite,
                        onOpenDetail = { item ->
                            focusManager.clearFocus()
                            onOpenDetail(item)
                        },
                        onLoadMore = searchViewModel::loadMore,
                        onRetryLoadMore = searchViewModel::retry
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onClear: () -> Unit,
    onBack: () -> Unit,
    focusRequester: FocusRequester
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 4.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
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

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester),
            placeholder = { Text("Buscar conteúdo", color = GalaxiaGray) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null, tint = GalaxiaGray)
            },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Close, contentDescription = "Limpar pesquisa", tint = GalaxiaGray)
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { onSearch() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = GalaxiaCardBackground,
                unfocusedContainerColor = GalaxiaCardBackground,
                focusedTextColor = GalaxiaWhite,
                unfocusedTextColor = GalaxiaWhite,
                cursorColor = GalaxiaCyan,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun IdleContent(onSuggestionClick: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 8.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Default.TravelExplore,
                contentDescription = null,
                tint = GalaxiaCyan,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Explore o acervo da NASA",
                color = GalaxiaWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Busque fotografias, vídeos, missões espaciais, clima solar e eventos naturais em todas as fontes oficiais da NASA. Pode digitar em português.",
            color = GalaxiaGray,
            fontSize = 14.sp,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = "SUGESTÕES",
            color = GalaxiaGray,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(10.dp))

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            SUGGESTIONS.forEach { suggestion ->
                Text(
                    text = suggestion,
                    color = GalaxiaWhite,
                    fontSize = 14.sp,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(GalaxiaCardBackground)
                        .clickable { onSuggestionClick(suggestion) }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageContent(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    messageColor: Color = GalaxiaWhite,
    action: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = GalaxiaGray, modifier = Modifier.size(48.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = messageColor,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            textAlign = TextAlign.Center
        )
        message?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(text = it, color = GalaxiaGray, fontSize = 14.sp, textAlign = TextAlign.Center)
        }
        action?.let {
            Spacer(modifier = Modifier.height(16.dp))
            it()
        }
    }
}

@Composable
private fun ResultsContent(
    state: SearchState.Results,
    favoriteIds: Set<String>,
    onToggleFavorite: (GlobalSearchItem) -> Unit,
    onOpenDetail: (GlobalSearchItem) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    LaunchedEffect(state.query) { listState.scrollToItem(0) }

    val nearEnd by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val lastVisible = info.visibleItemsInfo.lastOrNull()?.index ?: 0
            info.totalItemsCount > 0 && lastVisible >= info.totalItemsCount - 3
        }
    }
    LaunchedEffect(nearEnd, state.page, state.hasMore) {
        if (nearEnd && state.hasMore) onLoadMore()
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item(key = "header") {
            val total = NumberFormat.getIntegerInstance(Locale.forLanguageTag("pt-BR")).format(state.totalResults)
            Text(
                text = if (state.totalResults == 1) "1 resultado" else "$total resultados",
                color = GalaxiaGray,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
            )
        }

        items(items = state.items, key = { it.id }) { item ->
            SearchResultCard(
                item = item,
                isFavorite = item.id in favoriteIds,
                onToggleFavorite = { onToggleFavorite(item) },
                onClick = { onOpenDetail(item) }
            )
        }

        item(key = "footer") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoadingMore -> SearchResultCardSkeleton(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                    state.loadMoreFailed -> TextButton(onClick = onRetryLoadMore) {
                        Text("Falha ao carregar mais. Tocar para tentar de novo", color = GalaxiaCyan)
                    }
                    !state.hasMore && state.items.isNotEmpty() -> Text(
                        text = "Fim dos resultados",
                        color = GalaxiaGray,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SearchResultCard(
    item: GlobalSearchItem,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
    val uriHandler = LocalUriHandler.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GalaxiaCardBackground)
            .clickable(onClick = onClick)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val thumbModifier = Modifier
            .size(88.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF161822))

        val previewUrl = item.previewImageUrl
        when {
            item.contentType == GlobalContentType.VIDEO -> {
                Box(modifier = thumbModifier) {
                    if (!previewUrl.isNullOrBlank()) {
                        OptimizedAsyncImage(
                            lowResUrl = previewUrl,
                            highResUrl = null,
                            contentDescription = item.displayTitle,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(Color(0xFF161822)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Videocam, contentDescription = null, tint = GalaxiaCyan)
                        }
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .background(GalaxiaCyan, RoundedCornerShape(14.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Vídeo",
                                tint = GalaxiaBackground,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            !previewUrl.isNullOrBlank() -> {
                OptimizedAsyncImage(
                    lowResUrl = previewUrl,
                    highResUrl = null,
                    contentDescription = item.displayTitle,
                    modifier = thumbModifier,
                    contentScale = ContentScale.Crop
                )
            }

            else -> {
                val placeholderIcon = when (item.contentType) {
                    GlobalContentType.SPACE_WEATHER -> Icons.Default.WbSunny
                    GlobalContentType.NATURAL_EVENT -> Icons.Default.Public
                    GlobalContentType.SCIENTIFIC_RECORD -> Icons.Default.Science
                    else -> Icons.Default.Image
                }
                val tintColor = when (item.contentType) {
                    GlobalContentType.SPACE_WEATHER -> Color(0xFFFFB454)
                    GlobalContentType.NATURAL_EVENT -> Color(0xFF4CC3FF)
                    else -> GalaxiaCyan
                }
                Box(modifier = thumbModifier, contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = placeholderIcon,
                        contentDescription = item.contentType.label,
                        tint = tintColor,
                        modifier = Modifier.size(30.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(GalaxiaCyan.copy(alpha = 0.14f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.source.label} • ${item.contentType.label}",
                        color = GalaxiaCyan,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            if (item.date.isNotBlank()) {
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = formatDateBr(item.date),
                    color = GalaxiaGray,
                    fontSize = 11.sp
                )
            }

            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = item.displayTitle,
                color = GalaxiaWhite,
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            if (item.displayDescription.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.displayDescription,
                    color = GalaxiaGray,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                    tint = if (isFavorite) GalaxiaCyan else GalaxiaGray
                )
            }

            if (item.sourceUrl.isNotBlank()) {
                IconButton(
                    onClick = {
                        try {
                            uriHandler.openUri(item.sourceUrl)
                        } catch (_: Exception) {}
                    }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                        contentDescription = "Abrir conteúdo original",
                        tint = GalaxiaCyan,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
