package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.ApodMockData
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.translation.MlKitTranslationService
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

/**
 * Repositório responsável pela comunicação com a API NASA APOD e tradução automática dos conteúdos.
 */
class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val translationService: TranslationService = MlKitTranslationService()
) : BaseRepository {

    // Cache em memória para otimizar e evitar re-traduções do mesmo item durante a execução
    private val translationCache = ConcurrentHashMap<String, Pair<String, String>>()

    // Total de APODs publicados, usado para sortear páginas no modo aleatório
    @Volatile
    private var totalApods: Int? = null

    // --- Remoto (NASA APOD + Tradução Automática + Fallback de Contingência) ---

    suspend fun getApodList(count: Int = 10): List<ApodResponse> = coroutineScope {
        try {
            val originalList = apiService.getApodList(perPage = count)

            // Traduz os itens em paralelo utilizando corrotinas
            originalList.map { apod ->
                async {
                    translateApod(apod)
                }
            }.awaitAll()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Em caso de limitação da API da NASA ou erro de conexão,
            // retorna dados de contingência para que o app continue funcionando normalmente.
            ApodMockData.sampleApods
        }
    }

    /**
     * Sorteia [count] APODs de todo o acervo (desde 1995).
     * Como a API não oferece ordenação aleatória, cada item é buscado em uma página
     * sorteada com 'per_page=1', usando o total informado no cabeçalho X-WP-Total.
     */
    suspend fun getRandomApods(count: Int = 10): List<ApodResponse> = coroutineScope {
        try {
            val total = totalApods ?: fetchTotalApods().also { totalApods = it }
            val pages = generateSequence { Random.nextInt(1, total + 1) }
                .distinct()
                .take(minOf(count, total))
                .toList()

            pages.map { page ->
                async {
                    // Uma página que falhar não derruba as demais
                    try {
                        apiService.getApodList(perPage = 1, page = page)
                            .firstOrNull()
                            ?.let { translateApod(it) }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                }
            }.awaitAll()
                .filterNotNull()
                .ifEmpty { ApodMockData.sampleApods.shuffled() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            ApodMockData.sampleApods.shuffled()
        }
    }

    private suspend fun fetchTotalApods(): Int {
        val response = apiService.getApodListResponse()
        return response.headers()["X-WP-Total"]?.toIntOrNull()
            ?: throw IllegalStateException("Cabeçalho X-WP-Total ausente (HTTP ${response.code()})")
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        return try {
            val dateCode = formatDateToCode(date)
            val original = apiService.getApodByDate(dateCode = dateCode)
            translateApod(original)
        } catch (e: Exception) {
            ApodMockData.getMockByDate(date)
        }
    }

    private suspend fun translateApod(apod: ApodResponse): ApodResponse {
        val cacheKey = if (apod.date.isNotBlank()) apod.date else apod.title
        translationCache[cacheKey]?.let { (translatedTitle, translatedExplanation) ->
            return apod.copy(
                translatedTitle = translatedTitle,
                translatedExplanation = translatedExplanation,
                isTranslated = true
            )
        }

        return try {
            val titlePt = translationService.translateText(apod.title)
            val explanationPt = translationService.translateText(apod.cleanExplanation)

            translationCache[cacheKey] = Pair(titlePt, explanationPt)

            apod.copy(
                translatedTitle = titlePt,
                translatedExplanation = explanationPt,
                isTranslated = true
            )
        } catch (e: Exception) {
            // Em caso de falha de tradução, mantém o item original com fallback transparente
            apod.copy(isTranslated = false)
        }
    }

    private fun formatDateToCode(dateString: String): String {
        return try {
            val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            localDate.format(DateTimeFormatter.ofPattern("yyMMdd"))
        } catch (_: Exception) {
            dateString.replace("-", "").takeLast(6)
        }
    }
}
