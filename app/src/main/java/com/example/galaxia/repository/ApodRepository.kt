package com.example.galaxia.repository

import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.AppDatabase
import com.example.galaxia.data.local.FavoriteDao
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.toFavoriteEntity
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.ApodMockData
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.translation.MlKitTranslationService
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import java.util.concurrent.ConcurrentHashMap

class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val translationService: TranslationService = MlKitTranslationService(),
    private val favoriteDao: FavoriteDao = AppDatabase.getDatabase(GalaxiaApplication.instance).favoriteDao()
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
            // Em caso de limitação da API da NASA (HTTP 429 DEMO_KEY) ou erro de conexão,
            // retorna dados de contingência para que o app continue funcionando normalmente.
            ApodMockData.sampleApods
        }
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        return try {
            val original = apiService.getApodByDate(date = date)
            translateApod(original)
        } catch (e: Exception) {
            ApodMockData.getMockByDate(date)
        }
    }

    private suspend fun translateApod(apod: ApodResponse): ApodResponse {
        val cacheKey = apod.date
        translationCache[cacheKey]?.let { (translatedTitle, translatedExplanation) ->
            return apod.copy(
                translatedTitle = translatedTitle,
                translatedExplanation = translatedExplanation,
                isTranslated = true
            )
        }

        return try {
            val titlePt = translationService.translateText(apod.title)
            val explanationPt = translationService.translateText(apod.explanation)

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

    // --- Local (Favoritos via Room / Persistence) ---

    fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return favoriteDao.getAllFavorites()
    }

    fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteEntity>> {
        return favoriteDao.getFavoritesByType(type.name)
    }

    fun getAllFavoriteIds(): Flow<List<String>> {
        return favoriteDao.getAllFavoriteIds()
    }

    fun isFavorite(id: String): Flow<Boolean> {
        return favoriteDao.isFavorite(id)
    }

    suspend fun saveFavoriteApod(apod: ApodResponse) {
        favoriteDao.insertFavorite(apod.toFavoriteEntity())
    }

    suspend fun removeFavoriteById(id: String) {
        favoriteDao.deleteFavoriteById(id)
    }

    suspend fun toggleFavoriteApod(apod: ApodResponse, isCurrentlyFavorite: Boolean) {
        val favoriteId = "apod_${apod.date}"
        if (isCurrentlyFavorite) {
            favoriteDao.deleteFavoriteById(favoriteId)
        } else {
            favoriteDao.insertFavorite(apod.toFavoriteEntity())
        }
    }
}
