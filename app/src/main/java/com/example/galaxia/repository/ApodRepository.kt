package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.ApodMockData
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.translation.MlKitTranslationService
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositório responsável pela comunicação com a API NASA APOD e tradução automática dos conteúdos.
 */
class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val translationService: TranslationService = MlKitTranslationService()
) : BaseRepository {

    // Cache em memória para otimizar e evitar re-traduções do mesmo item durante a execução
    private val translationCache = ConcurrentHashMap<String, Pair<String, String>>()

    // --- Remoto (NASA APOD + Tradução Automática + Fallback de Contingência) ---

    suspend fun getApodList(count: Int = 10): List<ApodResponse> = coroutineScope {
        try {
            val originalList = apiService.getApodList(count = count)

            // Traduz os itens em paralelo utilizando corrotinas
            originalList.map { apod ->
                async {
                    translateApod(apod)
                }
            }.awaitAll()
        } catch (e: Exception) {
            // Em caso de limitação da API da NASA ou erro de conexão,
            // retorna dados de contingência para que o app continue funcionando normalmente.
            ApodMockData.sampleApods
        }
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
