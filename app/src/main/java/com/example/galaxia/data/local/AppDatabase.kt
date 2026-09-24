package com.example.galaxia.data.local

import android.content.Context

/**
 * Ponto de acesso central aos dados locais do GalaxIA (camada Data - local).
 * Fornece o DAO de favoritos utilizando o armazenamento local limpo (SharedPreferences + Gson).
 */
class AppDatabase private constructor(private val context: Context) {

    fun favoriteDao(): FavoriteDao {
        return FavoritesLocalDataSource.getInstance(context)
    }

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = AppDatabase(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
