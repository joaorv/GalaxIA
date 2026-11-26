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

/**
 * Filtro de pesquisa de mídias (Todos, Imagens).
 */
enum class SearchFilter {
    ALL,
    IMAGES
}

/**
 * Verifica se um APOD atende ao filtro de mídia selecionado.
 */
fun ApodResponse.matchesFilter(filter: SearchFilter): Boolean {
    val isVideo = mediaType == "video" || mediaType == "iframe" || mediaType == "embed" ||
            ApodResponse.extractYouTubeVideoId(effectiveMediaUrl) != null ||
            ApodResponse.extractVimeoVideoId(effectiveMediaUrl) != null ||
            ApodResponse.isDirectVideoFileUrl(effectiveMediaUrl)

    return when (filter) {
        SearchFilter.ALL -> true
        SearchFilter.IMAGES -> !isVideo
    }
}
