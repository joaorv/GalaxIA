package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.NewsSortOrder
import com.example.galaxia.data.model.NewsSubTopic
import com.example.galaxia.data.model.SpaceNewsItem
import com.example.galaxia.data.model.subTopics
import com.example.galaxia.data.remote.SpaceNewsFormatters
import com.example.galaxia.repository.SpaceNewsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * ViewModel da seção Notícias e Curiosidades Espaciais.
 * Carrega apenas a categoria, sub-tópico e ordenação ativos sob demanda,
 * com cache, paginação gradual, estado por categoria para deslize fluido
 * e tradução progressiva.
 */
class SpaceNewsViewModel(
    private val repository: SpaceNewsRepository = SpaceNewsRepository()
) : BaseViewModel() {

    private val _selectedCategory = MutableStateFlow(NewsCategory.NASA_HIGHLIGHTS)
    val selectedCategory: StateFlow<NewsCategory> = _selectedCategory.asStateFlow()

    private val _selectedSubTopics = MutableStateFlow(
        NewsCategory.entries
            .mapNotNull { cat -> cat.subTopics.firstOrNull()?.let { cat to it } }
            .toMap()
    )
    val selectedSubTopics: StateFlow<Map<NewsCategory, NewsSubTopic>> = _selectedSubTopics.asStateFlow()

    private val _selectedSortOrder = MutableStateFlow(NewsSortOrder.NEWEST)
    val selectedSortOrder: StateFlow<NewsSortOrder> = _selectedSortOrder.asStateFlow()

    private val _uiState = MutableStateFlow<SpaceNewsUiState>(SpaceNewsUiState.Loading)
    val uiState: StateFlow<SpaceNewsUiState> = _uiState.asStateFlow()

    private val _categoryStates = MutableStateFlow<Map<NewsCategory, SpaceNewsUiState>>(emptyMap())
    val categoryStates: StateFlow<Map<NewsCategory, SpaceNewsUiState>> = _categoryStates.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    private val loadedKeysByCategory = mutableMapOf<NewsCategory, String>()
    private val loadedPagesByCategory = mutableMapOf<NewsCategory, Int>()

    private var hasInitialized = false
    private var currentPage = 1
    private var loadJob: Job? = null
    private var pagingJob: Job? = null
    private var translationJob: Job? = null

    fun ensureLoaded() {
        if (hasInitialized) return
        hasInitialized = true
        val category = _selectedCategory.value
        loadCategory(
            category = category,
            subTopic = _selectedSubTopics.value[category],
            sortOrder = _selectedSortOrder.value,
            forceRefresh = false
        )
    }

    fun selectCategory(category: NewsCategory) {
        val subTopic = _selectedSubTopics.value[category]
        val sortOrder = _selectedSortOrder.value
        val expectedKey = repository.buildCacheKey(category, subTopic, sortOrder)
        val cachedState = _categoryStates.value[category]

        if (_selectedCategory.value == category &&
            loadedKeysByCategory[category] == expectedKey &&
            cachedState !is SpaceNewsUiState.Error
        ) {
            return
        }

        _selectedCategory.value = category
        hasInitialized = true

        if (loadedKeysByCategory[category] == expectedKey && cachedState is SpaceNewsUiState.Success) {
            loadJob?.cancel()
            pagingJob?.cancel()
            translationJob?.cancel()
            currentPage = loadedPagesByCategory[category] ?: 1
            _uiState.value = cachedState
            return
        }

        loadCategory(
            category = category,
            subTopic = subTopic,
            sortOrder = sortOrder,
            forceRefresh = false
        )
    }

    fun selectSubTopic(subTopic: NewsSubTopic) {
        val category = _selectedCategory.value
        if (_selectedSubTopics.value[category]?.id == subTopic.id &&
            _categoryStates.value[category] !is SpaceNewsUiState.Error
        ) {
            return
        }
        _selectedSubTopics.value = _selectedSubTopics.value + (category to subTopic)
        hasInitialized = true
        loadCategory(
            category = category,
            subTopic = subTopic,
            sortOrder = _selectedSortOrder.value,
            forceRefresh = false
        )
    }

    fun selectSortOrder(order: NewsSortOrder) {
        val category = _selectedCategory.value
        if (_selectedSortOrder.value == order &&
            _categoryStates.value[category] !is SpaceNewsUiState.Error
        ) {
            return
        }
        _selectedSortOrder.value = order
        hasInitialized = true
        loadCategory(
            category = category,
            subTopic = _selectedSubTopics.value[category],
            sortOrder = order,
            forceRefresh = false
        )
    }

    fun refresh() {
        if (_isRefreshing.value || _uiState.value is SpaceNewsUiState.Loading) return
        hasInitialized = true
        val category = _selectedCategory.value
        val subTopic = _selectedSubTopics.value[category]
        val sortOrder = _selectedSortOrder.value
        val existingSuccess = _categoryStates.value[category] is SpaceNewsUiState.Success

        if (!existingSuccess) {
            loadCategory(
                category = category,
                subTopic = subTopic,
                sortOrder = sortOrder,
                forceRefresh = true
            )
            return
        }

        loadJob?.cancel()
        pagingJob?.cancel()
        translationJob?.cancel()
        val cacheKey = repository.buildCacheKey(category, subTopic, sortOrder)

        loadJob = viewModelScope.launch {
            _isRefreshing.value = true
            try {
                val result = repository.fetchCategoryPage(
                    category = category,
                    subTopic = subTopic,
                    sortOrder = sortOrder,
                    page = 1,
                    forceRefresh = true,
                    allowStaleFallback = false
                )
                if (_selectedCategory.value != category ||
                    _selectedSubTopics.value[category]?.id != subTopic?.id ||
                    _selectedSortOrder.value != sortOrder
                ) {
                    return@launch
                }

                if (result.items.isEmpty()) {
                    loadedKeysByCategory[category] = cacheKey
                    loadedPagesByCategory[category] = 1
                    setCategoryState(category, SpaceNewsUiState.Empty(category))
                    return@launch
                }

                currentPage = result.page
                loadedKeysByCategory[category] = cacheKey
                loadedPagesByCategory[category] = currentPage
                val prepared = prepareInitialTranslation(result.items)

                setCategoryState(
                    category,
                    SpaceNewsUiState.Success(
                        items = prepared,
                        hasMore = result.hasMore,
                        isLoadingMore = false,
                        isFromOfflineCache = result.isFromOfflineCache
                    )
                )
                _isRefreshing.value = false

                val pending = prepared.filter {
                    !it.isTranslated || (it.fullDescription.isNotBlank() && it.translatedFullDescription.isNullOrBlank())
                }
                if (pending.isNotEmpty()) {
                    launchBackgroundTranslation(
                        category = category,
                        subTopicId = subTopic?.id,
                        sortOrder = sortOrder,
                        cacheKey = cacheKey,
                        pending = pending,
                        pageLoaded = currentPage,
                        hasMore = result.hasMore
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao atualizar $category via Pull to Refresh", e)
                _userMessage.emit(mapRefreshErrorMessage(e))
            } finally {
                _isRefreshing.value = false
            }
        }
    }

    private fun mapRefreshErrorMessage(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            403 -> "Chave de API inválida ou sem permissão."
            429 -> "Limite de requisições da NASA atingido. Tente em instantes."
            else -> "Erro ao atualizar notícias (HTTP ${e.code()})."
        }
        is SocketTimeoutException -> "O servidor da NASA demorou muito para responder."
        is UnknownHostException -> "Sem conexão com a internet. Mantendo conteúdo atual."
        is IOException -> "Falha de rede ao atualizar. Tente novamente."
        else -> "Não foi possível atualizar agora."
    }

    fun loadNextPage() {
        val category = _selectedCategory.value
        val currentState = _categoryStates.value[category] as? SpaceNewsUiState.Success ?: return
        if (!currentState.hasMore || currentState.isLoadingMore || loadJob?.isActive == true) return
        if (pagingJob?.isActive == true) return

        val subTopic = _selectedSubTopics.value[category]
        val sortOrder = _selectedSortOrder.value
        val cacheKey = repository.buildCacheKey(category, subTopic, sortOrder)
        val nextPage = currentPage + 1

        pagingJob = viewModelScope.launch {
            setCategoryState(category, currentState.copy(isLoadingMore = true))
            try {
                val pageResult = repository.fetchCategoryPage(
                    category = category,
                    subTopic = subTopic,
                    sortOrder = sortOrder,
                    page = nextPage,
                    forceRefresh = true
                )
                if (_selectedCategory.value != category ||
                    _selectedSubTopics.value[category]?.id != subTopic?.id ||
                    _selectedSortOrder.value != sortOrder
                ) {
                    return@launch
                }

                val existingIds = currentState.items.mapTo(HashSet()) { it.id }
                val newItems = pageResult.items.filter { it.id !in existingIds }

                val preparedNew = prepareInitialTranslation(newItems)
                val grouped = SpaceNewsFormatters.groupDuplicateTitleItems(currentState.items + preparedNew)
                val combined = sortItemsForDisplay(grouped, category, sortOrder)
                currentPage = pageResult.page
                loadedPagesByCategory[category] = currentPage
                val hasMore = pageResult.hasMore

                setCategoryState(
                    category,
                    SpaceNewsUiState.Success(
                        items = combined,
                        hasMore = hasMore,
                        isLoadingMore = false,
                        isFromOfflineCache = false
                    )
                )

                val remainingToTranslate = combined.filter {
                    it.id !in existingIds &&
                        (!it.isTranslated || (it.fullDescription.isNotBlank() && it.translatedFullDescription.isNullOrBlank()))
                }
                launchBackgroundTranslation(
                    category = category,
                    subTopicId = subTopic?.id,
                    sortOrder = sortOrder,
                    cacheKey = cacheKey,
                    pending = remainingToTranslate,
                    pageLoaded = currentPage,
                    hasMore = hasMore
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Falha ao paginar $category", e)
                val latest = _categoryStates.value[category] as? SpaceNewsUiState.Success ?: return@launch
                setCategoryState(category, latest.copy(isLoadingMore = false))
            }
        }
    }

    private fun sortItemsForDisplay(
        items: List<SpaceNewsItem>,
        category: NewsCategory,
        sortOrder: NewsSortOrder
    ): List<SpaceNewsItem> {
        return if ((category == NewsCategory.MISSIONS_DISCOVERIES ||
                category == NewsCategory.UNIVERSE_CURIOSITIES) &&
            sortOrder == NewsSortOrder.OLDEST
        ) {
            items.sortedBy { it.date.ifBlank { "9999-99-99" } }
        } else {
            items.sortedByDescending { it.date }
        }
    }

    private fun setCategoryState(category: NewsCategory, state: SpaceNewsUiState) {
        _categoryStates.value = _categoryStates.value + (category to state)
        if (_selectedCategory.value == category) {
            _uiState.value = state
        }
    }

    private fun loadCategory(
        category: NewsCategory,
        subTopic: NewsSubTopic?,
        sortOrder: NewsSortOrder,
        forceRefresh: Boolean
    ) {
        loadJob?.cancel()
        pagingJob?.cancel()
        translationJob?.cancel()
        currentPage = 1

        val cacheKey = repository.buildCacheKey(category, subTopic, sortOrder)

        loadJob = viewModelScope.launch {
            setCategoryState(category, SpaceNewsUiState.Loading)
            try {
                val result = repository.fetchCategoryPage(
                    category = category,
                    subTopic = subTopic,
                    sortOrder = sortOrder,
                    page = 1,
                    forceRefresh = forceRefresh
                )
                if (_selectedCategory.value != category ||
                    _selectedSubTopics.value[category]?.id != subTopic?.id ||
                    _selectedSortOrder.value != sortOrder
                ) {
                    return@launch
                }

                if (result.items.isEmpty()) {
                    loadedKeysByCategory[category] = cacheKey
                    loadedPagesByCategory[category] = 1
                    setCategoryState(category, SpaceNewsUiState.Empty(category))
                    return@launch
                }

                currentPage = result.page
                loadedKeysByCategory[category] = cacheKey
                loadedPagesByCategory[category] = currentPage
                val prepared = prepareInitialTranslation(result.items)

                setCategoryState(
                    category,
                    SpaceNewsUiState.Success(
                        items = prepared,
                        hasMore = result.hasMore,
                        isLoadingMore = false,
                        isFromOfflineCache = result.isFromOfflineCache
                    )
                )

                val pending = prepared.filter {
                    !it.isTranslated || (it.fullDescription.isNotBlank() && it.translatedFullDescription.isNullOrBlank())
                }
                if (pending.isNotEmpty()) {
                    launchBackgroundTranslation(
                        category = category,
                        subTopicId = subTopic?.id,
                        sortOrder = sortOrder,
                        cacheKey = cacheKey,
                        pending = pending,
                        pageLoaded = currentPage,
                        hasMore = result.hasMore
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val msg = when (e.code()) {
                    403 -> "Chave de API inválida ou sem permissão para esta consulta."
                    429 -> "Limite de requisições da NASA atingido. Tente novamente em instantes."
                    else -> "Erro ao consultar a NASA (HTTP ${e.code()})."
                }
                Log.e(TAG, "Erro HTTP em $category: $msg", e)
                setCategoryState(category, SpaceNewsUiState.Error(msg))
            } catch (e: SocketTimeoutException) {
                Log.e(TAG, "Timeout em $category", e)
                setCategoryState(
                    category,
                    SpaceNewsUiState.Error(
                        "O servidor da NASA demorou muito para responder. Toque em tentar novamente."
                    )
                )
            } catch (e: UnknownHostException) {
                Log.e(TAG, "Sem conexão em $category", e)
                setCategoryState(
                    category,
                    SpaceNewsUiState.Error(
                        "Sem conexão com a internet e nenhum conteúdo em cache para esta categoria."
                    )
                )
            } catch (e: IOException) {
                Log.e(TAG, "Erro de rede em $category", e)
                setCategoryState(
                    category,
                    SpaceNewsUiState.Error(
                        "Falha na comunicação com a rede. Verifique sua conexão e tente novamente."
                    )
                )
            } catch (e: Exception) {
                Log.e(TAG, "Erro inesperado em $category", e)
                setCategoryState(
                    category,
                    SpaceNewsUiState.Error(
                        "Não foi possível carregar os conteúdos agora. Tente novamente."
                    )
                )
            }
        }
    }

    private suspend fun prepareInitialTranslation(items: List<SpaceNewsItem>): List<SpaceNewsItem> {
        val visible = items.take(FULLY_TRANSLATED_BEFORE_SHOWING)
            .map { repository.translateItem(it) }
        val others = coroutineScope {
            items.drop(FULLY_TRANSLATED_BEFORE_SHOWING)
                .map { async { repository.translateTitleOnly(it) } }
                .awaitAll()
        }
        return visible + others
    }

    private fun launchBackgroundTranslation(
        category: NewsCategory,
        subTopicId: String?,
        sortOrder: NewsSortOrder,
        cacheKey: String,
        pending: List<SpaceNewsItem>,
        pageLoaded: Int,
        hasMore: Boolean
    ) {
        if (pending.isEmpty()) return
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            for (item in pending) {
                if (_selectedCategory.value != category ||
                    _selectedSubTopics.value[category]?.id != subTopicId ||
                    _selectedSortOrder.value != sortOrder
                ) {
                    return@launch
                }
                val translated = repository.translateItem(item)
                val current = _categoryStates.value[category] as? SpaceNewsUiState.Success ?: return@launch
                val updatedList = current.items.map { if (it.id == translated.id) translated else it }
                setCategoryState(category, current.copy(items = updatedList))
            }
            val finalState = _categoryStates.value[category] as? SpaceNewsUiState.Success ?: return@launch
            if (_selectedCategory.value == category &&
                _selectedSubTopics.value[category]?.id == subTopicId &&
                _selectedSortOrder.value == sortOrder
            ) {
                repository.updateCachedItems(
                    cacheKey = cacheKey,
                    items = finalState.items,
                    pageLoaded = pageLoaded,
                    hasMore = hasMore
                )
            }
        }
    }

    sealed class SpaceNewsUiState {
        object Loading : SpaceNewsUiState()
        data class Success(
            val items: List<SpaceNewsItem>,
            val hasMore: Boolean = false,
            val isLoadingMore: Boolean = false,
            val isFromOfflineCache: Boolean = false
        ) : SpaceNewsUiState()
        data class Empty(val category: NewsCategory) : SpaceNewsUiState()
        data class Error(val message: String) : SpaceNewsUiState()
    }

    private companion object {
        const val TAG = "SpaceNewsViewModel"
        const val FULLY_TRANSLATED_BEFORE_SHOWING = 3
    }
}
