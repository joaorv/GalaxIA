package com.example.galaxia.data.local

import kotlinx.coroutines.flow.Flow

/**
 * Contrato de acesso a dados locais para operações de favoritos no GalaxIA.
 * Interface agnóstica de persistência (SharedPreferences, DataStore, Room, etc.).
 */
interface FavoritesDataSource {

    fun getAllFavorites(): Flow<List<FavoriteEntity>>

    fun getFavoritesByType(itemType: String): Flow<List<FavoriteEntity>>

    fun isFavorite(id: String): Flow<Boolean>

    fun getAllFavoriteIds(): Flow<List<String>>

    suspend fun insertFavorite(favorite: FavoriteEntity)

    suspend fun deleteFavorite(favorite: FavoriteEntity)

    suspend fun deleteFavoriteById(id: String)
}
