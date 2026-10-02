package com.example.galaxia.repository

import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.FavoritesDataSource
import com.example.galaxia.data.local.FavoritesLocalDataSource
import com.example.galaxia.data.local.toFavoriteEntity
import com.example.galaxia.data.model.ApodResponse
import kotlinx.coroutines.flow.Flow

/**
 * Repositório dedicado à gestão de itens favoritos no GalaxIA.
 * Isola as regras de negócio de persistência da API remota.
 */
class FavoriteRepository(
    private val dataSource: FavoritesDataSource = FavoritesLocalDataSource.getInstance(GalaxiaApplication.instance)
) : BaseRepository {

    fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return dataSource.getAllFavorites()
    }

    fun getFavoritesByType(type: FavoriteType): Flow<List<FavoriteEntity>> {
        return dataSource.getFavoritesByType(type.name)
    }

    fun getAllFavoriteIds(): Flow<List<String>> {
        return dataSource.getAllFavoriteIds()
    }

    fun isFavorite(id: String): Flow<Boolean> {
        return dataSource.isFavorite(id)
    }

    suspend fun saveFavoriteApod(apod: ApodResponse) {
        dataSource.insertFavorite(apod.toFavoriteEntity())
    }

    suspend fun removeFavoriteById(id: String) {
        dataSource.deleteFavoriteById(id)
    }

    /**
     * Alterna o estado de favorito de um APOD.
     * @return true se foi adicionado aos favoritos, false se foi removido.
     */
    suspend fun toggleFavoriteApod(apod: ApodResponse, isCurrentlyFavorite: Boolean): Boolean {
        val favoriteId = "apod_${apod.date}"
        return if (isCurrentlyFavorite) {
            dataSource.deleteFavoriteById(favoriteId)
            false
        } else {
            dataSource.insertFavorite(apod.toFavoriteEntity())
            true
        }
    }
}
