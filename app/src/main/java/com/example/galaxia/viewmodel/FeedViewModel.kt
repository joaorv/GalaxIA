package com.example.galaxia.viewmodel

import android.util.Log
import androidx.lifecycle.viewModelScope
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.repository.ApodRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import retrofit2.HttpException
import java.io.IOException
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class FeedViewModel(
    private val repository: ApodRepository = ApodRepository()
) : BaseViewModel() {

    private val _apodState = MutableStateFlow<ApodState>(ApodState.Loading)
    val apodState: StateFlow<ApodState> = _apodState

    private val _historySelectedDate = MutableStateFlow(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE))
    val historySelectedDate: StateFlow<String> = _historySelectedDate

    private val _historyState = MutableStateFlow<HistoryState>(HistoryState.Loading)
    val historyState: StateFlow<HistoryState> = _historyState

    init {
        fetchApod()
        fetchHistoryByDate(_historySelectedDate.value)
    }

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
