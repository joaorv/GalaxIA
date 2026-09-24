package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.repository.ApodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeedViewModel(
    private val repository: ApodRepository = ApodRepository()
) : BaseViewModel() {

    // --- Estados do Feed e Histórico (NASA APOD) ---
    private val _apodState = MutableStateFlow<ApodState>(ApodState.Loading)
    val apodState: StateFlow<ApodState> = _apodState

    private val _historySelectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val historySelectedDate: StateFlow<String> = _historySelectedDate

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState

    // --- Estados de Favoritos (Room Database) ---
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _selectedFavoriteFilter = MutableStateFlow<FavoriteType?>(null)
    val selectedFavoriteFilter: StateFlow<FavoriteType?> = _selectedFavoriteFilter

    private val _allFavorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    /**
     * Lista de favoritos filtrada de acordo com o filtro selecionado (Todos, Fotos, etc.).
     */
    val filteredFavorites: StateFlow<List<FavoriteEntity>> = combine(
        _allFavorites,
        _selectedFavoriteFilter
    ) { favorites, filter ->
        if (filter == null) {
            favorites
        } else {
            favorites.filter { it.itemType == filter.name }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    init {
        fetchApod()
        fetchHistoryByDate(_historySelectedDate.value)
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            repository.getAllFavorites().collect { favorites ->
                _allFavorites.value = favorites
            }
        }

        viewModelScope.launch {
            repository.getAllFavoriteIds().collect { ids ->
                _favoriteIds.value = ids.toSet()
            }
        }
    }

    // --- Ações de Favoritos ---

    fun setFavoriteFilter(filter: FavoriteType?) {
        _selectedFavoriteFilter.value = filter
    }

    fun isApodFavorite(date: String): Boolean {
        return "apod_$date" in _favoriteIds.value
    }

    fun toggleFavoriteApod(apod: ApodResponse) {
        viewModelScope.launch {
            try {
                val isCurrentlyFavorite = isApodFavorite(apod.date)
                repository.toggleFavoriteApod(apod, isCurrentlyFavorite)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro ao alternar favorito", e)
            }
        }
    }

    fun removeFavoriteById(id: String) {
        viewModelScope.launch {
            try {
                repository.removeFavoriteById(id)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro ao remover favorito por id: $id", e)
            }
        }
    }

    // --- Ações da API NASA APOD ---

    fun fetchApod() {
        viewModelScope.launch {
            Log.d("FeedViewModel", "Iniciando fetchApod...")
            _apodState.value = ApodState.Loading
            try {
                val response = repository.getApodList()
                Log.d("FeedViewModel", "Sucesso: ${response.size} itens recebidos")
                _apodState.value = ApodState.Success(response)
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    403 -> "Chave de API inválida ou expirada."
                    429 -> "Limite de requisições da NASA atingido (DEMO_KEY). Tente mais tarde."
                    else -> "Erro na API da NASA: ${e.code()}"
                }
                Log.e("FeedViewModel", "Erro HTTP: $errorMsg", e)
                _apodState.value = ApodState.Error(errorMsg)
            } catch (e: IOException) {
                Log.e("FeedViewModel", "Erro de conexão", e)
                _apodState.value = ApodState.Error("Sem conexão com a internet. Verifique seu Wi-Fi ou dados móveis.")
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro inesperado", e)
                _apodState.value = ApodState.Error("Ocorreu um erro inesperado: ${e.message}")
            }
        }
    }

    fun selectHistoryDate(dateString: String) {
        _historySelectedDate.value = dateString
        fetchHistoryByDate(dateString)
    }

    fun fetchHistoryByDate(dateString: String) {
        viewModelScope.launch {
            Log.d("FeedViewModel", "Iniciando fetchHistoryByDate para data: $dateString")
            _historyState.value = HistoryState.Loading
            try {
                val response = repository.getApodByDate(dateString)
                Log.d("FeedViewModel", "Sucesso foto do dia $dateString: ${response.title}")
                _historyState.value = HistoryState.Success(response)
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    400 -> "Nenhuma foto publicada para a data selecionada ($dateString)."
                    403 -> "Chave de API inválida ou expirada."
                    429 -> "Limite de requisições da NASA atingido (DEMO_KEY). Tente mais tarde."
                    else -> "Erro na API da NASA (${e.code()})"
                }
                Log.e("FeedViewModel", "Erro HTTP ao buscar histórico: $errorMsg", e)
                _historyState.value = HistoryState.Error(errorMsg)
            } catch (e: IOException) {
                Log.e("FeedViewModel", "Erro de conexão ao buscar histórico", e)
                _historyState.value = HistoryState.Error("Sem conexão com a internet. Verifique seu Wi-Fi ou dados móveis.")
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro inesperado ao buscar histórico", e)
                _historyState.value = HistoryState.Error("Ocorreu um erro inesperado: ${e.message}")
            }
        }
    }

    sealed class ApodState {
        object Loading : ApodState()
        data class Success(val apods: List<ApodResponse>) : ApodState()
        data class Error(val message: String) : ApodState()
    }

    sealed class HistoryState {
        object Loading : HistoryState()
        data class Success(val apod: ApodResponse) : HistoryState()
        data class Error(val message: String) : HistoryState()
    }
}
