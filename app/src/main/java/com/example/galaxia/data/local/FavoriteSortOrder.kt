package com.example.galaxia.data.local

/**
 * Critérios de ordenação disponíveis para a tela de favoritos no GalaxIA.
 */
enum class FavoriteSortOrder(val displayName: String) {
    NEWEST("Mais recentes"),
    OLDEST("Mais antigos"),
    ALPHABETICAL("Título (A-Z)")
}
