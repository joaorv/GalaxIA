package com.example.galaxia.data.local

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fonte de dados local para persistência de favoritos usando SharedPreferences e Gson.
 * Implementa FavoriteDao sem requerer KSP ou plugins adicionais de compilação.
 */
class FavoritesLocalDataSource private constructor(context: Context) : FavoriteDao {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )
    private val gson = Gson()

    private val _favoritesFlow = MutableStateFlow<List<FavoriteEntity>>(loadFavoritesFromDisk())

    private fun loadFavoritesFromDisk(): List<FavoriteEntity> {
        val json = prefs.getString(KEY_FAVORITES, null) ?: return emptyList()
        return try {
            val type = object : TypeToken<List<FavoriteEntity>>() {}.type
            gson.fromJson<List<FavoriteEntity>>(json, type) ?: emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun persistToDisk(list: List<FavoriteEntity>) {
        val json = gson.toJson(list)
        prefs.edit().putString(KEY_FAVORITES, json).apply()
    }

    override fun getAllFavorites(): Flow<List<FavoriteEntity>> {
        return _favoritesFlow.asStateFlow()
    }

    override fun getFavoritesByType(itemType: String): Flow<List<FavoriteEntity>> {
        return _favoritesFlow.map { list ->
            list.filter { it.itemType == itemType }
        }
    }

    override fun getAllFavoriteIds(): Flow<List<String>> {
        return _favoritesFlow.map { list ->
            list.map { it.id }
        }
    }

    override fun isFavorite(id: String): Flow<Boolean> {
        return _favoritesFlow.map { list ->
            list.any { it.id == id }
        }
    }

    override suspend fun insertFavorite(favorite: FavoriteEntity) {
        val current = _favoritesFlow.value.toMutableList()
        current.removeAll { it.id == favorite.id }
        current.add(0, favorite)
        _favoritesFlow.value = current
        persistToDisk(current)
    }

    override suspend fun deleteFavorite(favorite: FavoriteEntity) {
        deleteFavoriteById(favorite.id)
    }

    override suspend fun deleteFavoriteById(id: String) {
        val current = _favoritesFlow.value.toMutableList()
        val changed = current.removeAll { it.id == id }
        if (changed) {
            _favoritesFlow.value = current
            persistToDisk(current)
        }
    }

    companion object {
        private const val PREFS_NAME = "galaxia_favorites_prefs"
        private const val KEY_FAVORITES = "saved_favorites_list"

        @Volatile
        private var INSTANCE: FavoritesLocalDataSource? = null

        fun getInstance(context: Context): FavoritesLocalDataSource {
            return INSTANCE ?: synchronized(this) {
                val instance = FavoritesLocalDataSource(context)
                INSTANCE = instance
                instance
            }
        }
    }
}
