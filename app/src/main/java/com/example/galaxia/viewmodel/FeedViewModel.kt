package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteSortOrder
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.repository.ApodRepository
import com.example.galaxia.repository.FavoriteRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeedViewModel(
    private val apodRepository: ApodRepository = ApodRepository(),
    private val favoriteRepository: FavoriteRepository = FavoriteRepository()
) : BaseViewModel() {

    // --- Estados do Feed e Histórico (NASA APOD) ---
    private val _apodState = MutableStateFlow<ApodState>(ApodState.Loading)
    val apodState: StateFlow<ApodState> = _apodState

    private val _feedFilter = MutableStateFlow(FeedFilter.LATEST)
    val feedFilter: StateFlow<FeedFilter> = _feedFilter.asStateFlow()

    private var fetchApodJob: Job? = null

    private val _historySelectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val historySelectedDate: StateFlow<String> = _historySelectedDate

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState

    // --- Eventos de Notificação/Feedback (Snackbar) ---
    private val _userMessage = MutableSharedFlow<String>()
    val userMessage: SharedFlow<String> = _userMessage.asSharedFlow()

    // --- Estados de Favoritos (Persistência Local) ---
    private val _favoriteIds = MutableStateFlow<Set<String>>(emptySet())
    val favoriteIds: StateFlow<Set<String>> = _favoriteIds

    private val _selectedFavoriteFilter = MutableStateFlow<FavoriteType?>(null)
    val selectedFavoriteFilter: StateFlow<FavoriteType?> = _selectedFavoriteFilter

    private val _favoriteSortOrder = MutableStateFlow<FavoriteSortOrder>(FavoriteSortOrder.NEWEST)
    val favoriteSortOrder: StateFlow<FavoriteSortOrder> = _favoriteSortOrder

    private val _isFavoritesLoading = MutableStateFlow(false)
    val isFavoritesLoading: StateFlow<Boolean> = _isFavoritesLoading.asStateFlow()

    private var filterJob: Job? = null

    private val _allFavorites = MutableStateFlow<List<FavoriteEntity>>(emptyList())

    /**
     * Lista de favoritos filtrada por categoria e ordenada de acordo com a preferência do usuário.
     */
    val filteredFavorites: StateFlow<List<FavoriteEntity>> = combine(
        _allFavorites,
        _selectedFavoriteFilter,
        _favoriteSortOrder
    ) { favorites, filter, sortOrder ->
        val filtered = if (filter == null) {
            favorites
        } else {
            favorites.filter { it.itemType == filter.name }
        }

        when (sortOrder) {
            FavoriteSortOrder.NEWEST -> filtered.sortedWith(
                compareByDescending<FavoriteEntity> { parseItemDate(it.subtitleOrDate) }
                    .thenByDescending { it.subtitleOrDate }
                    .thenByDescending { it.savedAtTimestamp }
            )
            FavoriteSortOrder.OLDEST -> filtered.sortedWith(
                compareBy<FavoriteEntity> { parseItemDate(it.subtitleOrDate) }
                    .thenBy { it.subtitleOrDate }
                    .thenBy { it.savedAtTimestamp }
            )
            FavoriteSortOrder.ALPHABETICAL -> filtered.sortedBy { it.title.lowercase().trim() }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private fun parseItemDate(subtitleOrDate: String): LocalDate {
        return try {
            LocalDate.parse(subtitleOrDate, DateTimeFormatter.ISO_LOCAL_DATE)
        } catch (_: Exception) {
            LocalDate.MIN
        }
    }

    init {
        fetchApod()
        fetchHistoryByDate(_historySelectedDate.value)
        observeFavorites()
    }

    private fun observeFavorites() {
        viewModelScope.launch {
            favoriteRepository.getAllFavorites().collect { favorites ->
                _allFavorites.value = favorites
            }
        }

        viewModelScope.launch {
            favoriteRepository.getAllFavoriteIds().collect { ids ->
                _favoriteIds.value = ids.toSet()
            }
        }
    }

    // --- Ações de Favoritos ---

    fun setFavoriteFilter(filter: FavoriteType?) {
        if (_selectedFavoriteFilter.value == filter) return
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _isFavoritesLoading.value = true
            _selectedFavoriteFilter.value = filter
            delay(200)
            _isFavoritesLoading.value = false
        }
    }

    fun setFavoriteSortOrder(order: FavoriteSortOrder) {
        if (_favoriteSortOrder.value == order) return
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            _isFavoritesLoading.value = true
            _favoriteSortOrder.value = order
            delay(200)
            _isFavoritesLoading.value = false
        }
    }

    fun isApodFavorite(date: String): Boolean {
        return "apod_$date" in _favoriteIds.value
    }

    fun toggleFavoriteApod(apod: ApodResponse) {
        viewModelScope.launch {
            try {
                val isCurrentlyFavorite = isApodFavorite(apod.date)
                val added = favoriteRepository.toggleFavoriteApod(apod, isCurrentlyFavorite)
                val message = if (added) "Foto adicionada aos favoritos" else "Foto removida dos favoritos"
                _userMessage.emit(message)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro ao alternar favorito", e)
                _userMessage.emit("Erro ao atualizar favoritos")
            }
        }
    }

    fun toggleFavoriteItem(entity: FavoriteEntity) {
        viewModelScope.launch {
            try {
                val isCurrentlyFavorite = entity.id in _favoriteIds.value
                val added = favoriteRepository.toggleFavoriteEntity(entity, isCurrentlyFavorite)
                val message = if (added) "Item adicionado aos favoritos" else "Item removido dos favoritos"
                _userMessage.emit(message)
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro ao alternar favorito", e)
                _userMessage.emit("Erro ao atualizar favoritos")
            }
        }
    }

    fun removeFavoriteById(id: String) {
        viewModelScope.launch {
            try {
                favoriteRepository.removeFavoriteById(id)
                _userMessage.emit("Foto removida dos favoritos")
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro ao remover favorito por id: $id", e)
                _userMessage.emit("Erro ao remover favorito")
            }
        }
    }

    // --- Ações da API NASA APOD ---

    /**
     * Troca o filtro do feed e recarrega. Selecionar "Aleatórias" de novo sorteia outras fotos.
     */
    fun setFeedFilter(filter: FeedFilter) {
        if (filter == _feedFilter.value && filter == FeedFilter.LATEST) return
        _feedFilter.value = filter
        fetchApod()
    }

    /**
     * Carrega o feed em etapas, porque a tradução no aparelho é lenta (um texto por vez):
     * 1. busca as fotos na API (rápido);
     * 2. traduz por completo as primeiras fotos e só o título das demais, e mostra o feed;
     * 3. traduz o restante das explicações em segundo plano.
     * As traduções ficam em cache em disco, então nas próximas aberturas tudo isso é imediato.
     */
    fun fetchApod() {
        // Cancela uma busca anterior para que o resultado de outro filtro não sobrescreva o atual
        fetchApodJob?.cancel()
        fetchApodJob = viewModelScope.launch {
            val filter = _feedFilter.value
            Log.d("FeedViewModel", "Iniciando fetchApod ($filter)...")
            _apodState.value = ApodState.Loading
            try {
                val originals = when (filter) {
                    FeedFilter.LATEST -> apodRepository.fetchLatestApods()
                    FeedFilter.RANDOM -> apodRepository.fetchRandomApods()
                }

                // Primeiro o que aparece na tela: texto completo das primeiras fotos, uma de cada vez
                val visible = originals.take(FULLY_TRANSLATED_BEFORE_SHOWING)
                    .map { apodRepository.translateApod(it) }
                // Das demais, só o título (curto) antes de mostrar
                val others = coroutineScope {
                    originals.drop(FULLY_TRANSLATED_BEFORE_SHOWING)
                        .map { async { apodRepository.translateTitle(it) } }
                        .awaitAll()
                }

                Log.d("FeedViewModel", "Sucesso: ${originals.size} itens recebidos")
                _apodState.value = ApodState.Success(visible + others)

                translateRemainingExplanations(others)
            } catch (e: CancellationException) {
                throw e
            } catch (e: HttpException) {
                val errorMsg = when (e.code()) {
                    403 -> "Chave de API inválida ou expirada."
                    429 -> "Limite de requisições da NASA atingido (DEMO_KEY). Tente mais tarde."
                    else -> "Erro na API da NASA: ${e.code()}"
                }
                Log.e("FeedViewModel", "Erro HTTP: $errorMsg", e)
                _apodState.value = ApodState.Error(errorMsg)
            } catch (e: SocketTimeoutException) {
                Log.e("FeedViewModel", "Timeout na resposta da NASA", e)
                _apodState.value = ApodState.Error("O servidor da NASA demorou muito para responder. Toque em tentar novamente.")
            } catch (e: UnknownHostException) {
                Log.e("FeedViewModel", "Sem conexão com o servidor", e)
                _apodState.value = ApodState.Error("Sem conexão com a internet. Verifique seu Wi-Fi ou dados móveis.")
            } catch (e: IOException) {
                Log.e("FeedViewModel", "Erro de E/S ou rede", e)
                _apodState.value = ApodState.Error("Falha na comunicação com a rede. Tente novamente.")
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro inesperado", e)
                _apodState.value = ApodState.Error("Ocorreu um erro inesperado: ${e.message}")
            }
        }
    }

    /**
     * Traduz as explicações que ficaram para depois, na ordem do feed, trocando cada card
     * assim que a tradução fica pronta. É cancelada junto com o carregamento se o filtro mudar.
     */
    private suspend fun translateRemainingExplanations(pending: List<ApodResponse>) {
        pending.forEach { partial ->
            val translated = apodRepository.translateApod(partial)
            val current = _apodState.value as? ApodState.Success ?: return
            _apodState.value = ApodState.Success(
                current.apods.map { if (it.date == translated.date) translated else it }
            )
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
                val response = apodRepository.getApodByDate(dateString)
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
            } catch (e: SocketTimeoutException) {
                Log.e("FeedViewModel", "Timeout ao buscar histórico", e)
                _historyState.value = HistoryState.Error("O servidor da NASA demorou muito para responder. Toque em tentar novamente.")
            } catch (e: UnknownHostException) {
                Log.e("FeedViewModel", "Sem conexão com o servidor ao buscar histórico", e)
                _historyState.value = HistoryState.Error("Sem conexão com a internet. Verifique seu Wi-Fi ou dados móveis.")
            } catch (e: IOException) {
                Log.e("FeedViewModel", "Erro de conexão ao buscar histórico", e)
                _historyState.value = HistoryState.Error("Falha na comunicação com a rede. Tente novamente.")
            } catch (e: Exception) {
                Log.e("FeedViewModel", "Erro inesperado ao buscar histórico", e)
                _historyState.value = HistoryState.Error("Ocorreu um erro inesperado: ${e.message}")
            }
        }
    }

    enum class FeedFilter(val label: String) {
        LATEST("Últimas"),
        RANDOM("Aleatórias")
    }

    private companion object {
        /** Fotos traduzidas por completo antes de o feed aparecer (as que cabem na primeira tela). */
        const val FULLY_TRANSLATED_BEFORE_SHOWING = 2
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
