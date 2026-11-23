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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.ui.components.OptimizedAsyncImage
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
    "Nebulosa de Órion", "Aurora boreal", "Saturno", "Eclipse solar",
    "Via Láctea", "Buraco negro", "Cometa", "Lua"
)

/**
 * Tela de pesquisa de notícias (fotos do dia/APOD de todo o acervo da NASA).
 * O usuário pode digitar em português: o termo é traduzido para inglês antes da busca
 * e os resultados voltam traduzidos.
 *
 * @param favoriteIds ids salvos nos favoritos, para marcar o coração de cada resultado.
 */
@Composable
fun SearchScreen(
    favoriteIds: Set<String>,
    onToggleFavorite: (ApodResponse) -> Unit,
    onOpenDetail: (ApodResponse) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    searchViewModel: SearchViewModel = viewModel()
) {
    val query by searchViewModel.query.collectAsState()
    val state by searchViewModel.state.collectAsState()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // Abre com o teclado pronto, a não ser que já exista uma pesquisa anterior na tela
    LaunchedEffect(Unit) {
        if (query.isBlank()) focusRequester.requestFocus()
    }

    // Surface bloqueia toques para o feed que fica por baixo
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

                    is SearchState.Loading -> Column(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        CircularProgressIndicator(color = GalaxiaCyan)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Pesquisando “${current.query}”…",
                            color = GalaxiaGray,
                            fontSize = 14.sp
                        )
                    }

                    is SearchState.Empty -> MessageContent(
                        icon = Icons.Default.SearchOff,
                        title = "Nenhum resultado para “${current.query}”",
                        message = "Tente outro termo, como o nome de um planeta, nebulosa ou fenômeno.",
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
                        onOpenDetail = { apod ->
                            focusManager.clearFocus()
                            onOpenDetail(apod)
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
            placeholder = { Text("Pesquisar notícias", color = GalaxiaGray) },
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
            text = "Pesquise entre milhares de fotos e notícias astronômicas publicadas desde 1995. " +
                "Pode escrever em português: a gente traduz para você.",
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
    icon: androidx.compose.ui.graphics.vector.ImageVector,
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
    onToggleFavorite: (ApodResponse) -> Unit,
    onOpenDetail: (ApodResponse) -> Unit,
    onLoadMore: () -> Unit,
    onRetryLoadMore: () -> Unit
) {
    val listState = rememberLazyListState()

    // Rola de volta ao topo quando uma nova pesquisa substitui os resultados
    LaunchedEffect(state.query) { listState.scrollToItem(0) }

    // Pede a próxima página quando faltam poucos itens para o fim da lista
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

        items(items = state.items, key = { it.date }) { apod ->
            SearchResultCard(
                apod = apod,
                isFavorite = "apod_${apod.date}" in favoriteIds,
                onToggleFavorite = { onToggleFavorite(apod) },
                onClick = { onOpenDetail(apod) }
            )
        }

        item(key = "footer") {
            Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                when {
                    state.isLoadingMore -> CircularProgressIndicator(
                        color = GalaxiaCyan,
                        modifier = Modifier.size(28.dp),
                        strokeWidth = 3.dp
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
    apod: ApodResponse,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit
) {
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

        if (apod.mediaType == "image" && apod.displayImageUrl.isNotBlank()) {
            OptimizedAsyncImage(
                lowResUrl = apod.lowResImageUrl,
                highResUrl = null,
                contentDescription = apod.alt ?: apod.displayTitle,
                modifier = thumbModifier,
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = thumbModifier, contentAlignment = Alignment.Center) {
                Icon(Icons.Default.Image, contentDescription = null, tint = GalaxiaGray)
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = formatDateBr(apod.date),
                color = GalaxiaGray,
                fontSize = 12.sp
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = apod.displayTitle,
                color = GalaxiaWhite,
                fontSize = 16.sp,
                lineHeight = 21.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (apod.displayExplanation.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = apod.displayExplanation,
                    color = GalaxiaGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = onToggleFavorite) {
            Icon(
                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                contentDescription = if (isFavorite) "Remover dos favoritos" else "Adicionar aos favoritos",
                tint = if (isFavorite) GalaxiaCyan else GalaxiaGray
            )
        }
    }
}
