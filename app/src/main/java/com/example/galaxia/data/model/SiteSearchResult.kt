package com.example.galaxia.data.model

/**
 * Item da busca do WordPress (science.nasa.gov/wp-json/wp/v2/search).
 * Para APODs, [title] vem como "APOD: 2026 October 26 &#8211; M42: The Great Nebula in Orion".
 */
data class SiteSearchResult(
    val id: Long? = null,
    val title: String? = null,
    val url: String? = null
)

/**
 * Uma página de resultados da pesquisa de APODs.
 *
 * @param searchTerm termo realmente enviado à API (em inglês, após a tradução da
 * consulta), reutilizado ao carregar as próximas páginas.
 */
data class ApodSearchPage(
    val items: List<ApodResponse>,
    val page: Int,
    val totalPages: Int,
    val totalResults: Int,
    val searchTerm: String
) {
    val hasMore: Boolean get() = page < totalPages
}
