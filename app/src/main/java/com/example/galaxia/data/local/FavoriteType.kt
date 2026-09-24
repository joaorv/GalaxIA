package com.example.galaxia.data.local

/**
 * Categorias de itens favoritos no GalaxIA.
 * Permite filtrar por tipo de conteúdo na tela de favoritos e expandir
 * para Notícias e Curiosidades futuras com uma única tabela.
 */
enum class FavoriteType(val displayName: String) {
    APOD("Fotos do Dia"),
    NEWS("Notícias"),
    CURIOSITY("Curiosidades")
}
