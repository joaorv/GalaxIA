package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.model.GlobalSearchItem
import com.example.galaxia.repository.GlobalSearchRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Estado da Pesquisa Global do GalaxIA (NASA Image & Video Library, DONKI, EONET e APOD).
 * Dispara com debounce ao digitar, cancela buscas anteriores e traduz progressivamente.
 */
class SearchViewModel(
    private val globalSearchRepository: GlobalSearchRepository = GlobalSearchRepository()
) : BaseViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null
    private var translationJob: Job? = null

    fun onQueryChange(text: String) {
        _query.value = text
        startSearch(debounceMs = TYPING_DEBOUNCE_MS)
    }

    /** Ação "buscar" do teclado ou toque numa sugestão: pesquisa na hora. */
    fun submit(text: String = _query.value) {
        _query.value = text
        startSearch(debounceMs = 0, force = true)
    }

    fun clear() {
        searchJob?.cancel()
        translationJob?.cancel()
        _query.value = ""
        _state.value = SearchState.Idle
    }

    fun retry() {
        val current = _state.value
        if (current is SearchState.Results && current.loadMoreFailed) loadMore()
        else startSearch(debounceMs = 0, force = true)
    }

    private fun startSearch(debounceMs: Long, force: Boolean = false) {
        val text = _query.value.trim()

        val current = _state.value
        if (!force && current is SearchState.Results && current.query == text) return

        searchJob?.cancel()
        translationJob?.cancel()
        if (text.length < MIN_QUERY_LENGTH) {
            _state.value = SearchState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(debounceMs)
            _state.value = SearchState.Loading(text)
            try {
                val page = globalSearchRepository.search(query = text, page = 1)
                if (_query.value.trim() != text) return@launch

                if (page.items.isEmpty() && !page.hasMore) {
                    _state.value = SearchState.Empty(text)
                } else {
                    val prepared = prepareInitialTranslation(page.items)
                    if (_query.value.trim() != text) return@launch

                    _state.value = SearchState.Results(
                        query = text,
                        searchTerm = page.searchTerm,
                        items = prepared,
                        totalResults = page.totalResults,
                        page = page.page,
                        hasMore = page.hasMore
                    )

                    val pending = prepared.filter {
                        !it.isTranslated || (it.description.isNotBlank() && it.translatedDescription.isNullOrBlank())
                    }
                    launchBackgroundTranslation(text, pending)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao pesquisar '$text'", e)
                _state.value = SearchState.Error(text, errorMessage(e))
            }
        }
    }

    /** Carrega a próxima página quando a lista chega perto do fim. */
    fun loadMore() {
        val current = _state.value as? SearchState.Results ?: return
        if (!current.hasMore || current.isLoadingMore || searchJob?.isActive == true) return

        _state.value = current.copy(isLoadingMore = true, loadMoreFailed = false)
        searchJob = viewModelScope.launch {
            try {
                val next = globalSearchRepository.search(
                    query = current.query,
                    page = current.page + 1,
                    searchTerm = current.searchTerm
                )
                if (_query.value.trim() != current.query) return@launch

                val existingIds = current.items.mapTo(HashSet()) { it.id }
                val uniqueNew = next.items.filter { it.id !in existingIds }
                val preparedNew = prepareInitialTranslation(uniqueNew)
                val merged = (current.items + preparedNew).sortedByDescending { it.date }

                _state.value = current.copy(
                    items = merged,
                    page = next.page,
                    hasMore = next.hasMore && uniqueNew.isNotEmpty(),
                    isLoadingMore = false
                )

                val pending = preparedNew.filter {
                    !it.isTranslated || (it.description.isNotBlank() && it.translatedDescription.isNullOrBlank())
                }
                launchBackgroundTranslation(current.query, pending)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar mais resultados", e)
                _state.value = current.copy(isLoadingMore = false, loadMoreFailed = true)
            }
        }
    }

    private suspend fun prepareInitialTranslation(items: List<GlobalSearchItem>): List<GlobalSearchItem> {
        val visible = items.take(FULLY_TRANSLATED_BEFORE_SHOWING)
            .map { globalSearchRepository.translateItem(it) }
        val others = coroutineScope {
            items.drop(FULLY_TRANSLATED_BEFORE_SHOWING)
                .map { async { globalSearchRepository.translateTitleOnly(it) } }
                .awaitAll()
        }
        return visible + others
    }

    private fun launchBackgroundTranslation(expectedQuery: String, pending: List<GlobalSearchItem>) {
        if (pending.isEmpty()) return
        translationJob?.cancel()
        translationJob = viewModelScope.launch {
            for (item in pending) {
                if (_query.value.trim() != expectedQuery) return@launch
                val translated = globalSearchRepository.translateItem(item)
                val current = _state.value as? SearchState.Results ?: return@launch
                if (current.query != expectedQuery) return@launch
                _state.value = current.copy(
                    items = current.items.map { if (it.id == translated.id) translated else it }
                )
            }
        }
    }

    private fun errorMessage(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            429 -> "Muitas pesquisas em pouco tempo. Aguarde um instante e tente de novo."
            else -> "Erro nos servidores da NASA (${e.code()}). Tente novamente."
        }
        is SocketTimeoutException -> "Os servidores da NASA demoraram muito para responder."
        is UnknownHostException -> "Sem conexão com a internet. Verifique seu Wi-Fi ou dados móveis."
        is IOException -> "Falha na comunicação com a rede. Tente novamente."
        else -> "Ocorreu um erro inesperado: ${e.message}"
    }

    sealed class SearchState {
        object Idle : SearchState()
        data class Loading(val query: String) : SearchState()
        data class Empty(val query: String) : SearchState()
        data class Error(val query: String, val message: String) : SearchState()
        data class Results(
            val query: String,
            val searchTerm: String,
            val items: List<GlobalSearchItem>,
            val totalResults: Int,
            val page: Int,
            val hasMore: Boolean,
            val isLoadingMore: Boolean = false,
            val loadMoreFailed: Boolean = false
        ) : SearchState()
    }

    companion object {
        private const val TAG = "SearchViewModel"
        private const val TYPING_DEBOUNCE_MS = 600L
        private const val FULLY_TRANSLATED_BEFORE_SHOWING = 3
        const val MIN_QUERY_LENGTH = 3
    }
}
