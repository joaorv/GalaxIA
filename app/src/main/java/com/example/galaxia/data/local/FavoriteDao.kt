package com.example.galaxia.data.local

/**
 * Interface legada mantida para compatibilidade reversa.
 * Redireciona diretamente para FavoritesDataSource.
 */
@Deprecated(
    message = "Utilize FavoritesDataSource para evitar confusão conceitual com Room DAO",
    replaceWith = ReplaceWith("FavoritesDataSource")
)
typealias FavoriteDao = FavoritesDataSource
