package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.repository.ApodRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
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
 * Estado da tela de pesquisa de notícias (APODs de todo o acervo da NASA).
 * A pesquisa dispara sozinha após uma pausa na digitação ou ao tocar em "buscar" no teclado.
 */
class SearchViewModel(
    private val apodRepository: ApodRepository = ApodRepository()
) : BaseViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _state = MutableStateFlow<SearchState>(SearchState.Idle)
    val state: StateFlow<SearchState> = _state.asStateFlow()

    private var searchJob: Job? = null

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

        // Evita repetir a mesma pesquisa quando o texto não mudou de fato (ex.: espaço no fim)
        val current = _state.value
        if (!force && current is SearchState.Results && current.query == text) return

        searchJob?.cancel()
        if (text.length < MIN_QUERY_LENGTH) {
            _state.value = SearchState.Idle
            return
        }

        searchJob = viewModelScope.launch {
            delay(debounceMs)
            _state.value = SearchState.Loading(text)
            try {
                val page = apodRepository.searchApods(text)
                _state.value = if (page.items.isEmpty() && !page.hasMore) {
                    SearchState.Empty(text)
                } else {
                    SearchState.Results(
                        query = text,
                        searchTerm = page.searchTerm,
                        items = page.items,
                        totalResults = page.totalResults,
                        page = page.page,
                        hasMore = page.hasMore
                    )
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
                val next = apodRepository.searchApods(
                    query = current.query,
                    page = current.page + 1,
                    searchTerm = current.searchTerm
                )
                val merged = (current.items + next.items).distinctBy { it.date }
                _state.value = current.copy(
                    items = merged,
                    page = next.page,
                    hasMore = next.hasMore,
                    isLoadingMore = false
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "Erro ao carregar mais resultados", e)
                _state.value = current.copy(isLoadingMore = false, loadMoreFailed = true)
            }
        }
    }

    private fun errorMessage(e: Exception): String = when (e) {
        is HttpException -> when (e.code()) {
            429 -> "Muitas pesquisas em pouco tempo. Aguarde um instante e tente de novo."
            else -> "Erro no site da NASA (${e.code()}). Tente novamente."
        }
        is SocketTimeoutException -> "O servidor da NASA demorou muito para responder."
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
            val items: List<ApodResponse>,
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
        const val MIN_QUERY_LENGTH = 3
    }
}
