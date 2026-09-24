package com.example.galaxia.repository

import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.AppDatabase
import com.example.galaxia.data.local.FavoriteDao
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.toFavoriteEntity
import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.RetrofitClient
import kotlinx.coroutines.flow.Flow

class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService,
    private val favoriteDao: FavoriteDao = AppDatabase.getDatabase(GalaxiaApplication.instance).favoriteDao()
) : BaseRepository {

    // --- Remoto (NASA APOD) ---

    suspend fun getApodList(count: Int = 10): List<ApodResponse> {
        return apiService.getApodList(count = count)
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        return apiService.getApodByDate(date = date)
    }

    // --- Local (Favoritos via Room) ---

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
