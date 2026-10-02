package com.example.galaxia.data.local

import android.content.Context

/**
 * Provedor de acesso centralizado aos recursos de armazenamento local do GalaxIA.
 */
class AppDatabase private constructor(private val context: Context) {

    fun favoritesDataSource(): FavoritesDataSource {
        return FavoritesLocalDataSource.getInstance(context)
    }

    @Deprecated("Utilize favoritesDataSource()", ReplaceWith("favoritesDataSource()"))
    fun favoriteDao(): FavoritesDataSource {
        return favoritesDataSource()
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
