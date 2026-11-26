package com.example.galaxia.data.model

import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType
import com.example.galaxia.data.local.toFavoriteEntity

/**
 * Fontes oficiais consultadas pela Pesquisa Global do GalaxIA.
 */
enum class GlobalSearchSource(val label: String) {
    NASA_MEDIA("NASA Media"),
    APOD("NASA APOD"),
    DONKI("NASA DONKI"),
    EONET("NASA EONET")
}

/**
 * Tipos de conteúdo identificados corretamente nos resultados da Pesquisa Global.
 */
enum class GlobalContentType(val label: String) {
    PHOTO("Fotografia"),
    VIDEO("Vídeo"),
    SPACE_WEATHER("Clima Espacial"),
    NATURAL_EVENT("Evento Natural"),
    SCIENTIFIC_RECORD("Registro Científico")
}

/**
 * Modelo unificado para resultados da Pesquisa Global integrando NASA Image & Video Library,
 * APOD, DONKI e EONET.
 */
data class GlobalSearchItem(
    val id: String,
    val source: GlobalSearchSource,
    val contentType: GlobalContentType,
    val title: String,
    val description: String,
    val date: String,
    val imageUrl: String? = null,
    val thumbnailUrl: String? = null,
    val mediaType: String = "image",
    val sourceUrl: String,
    val authorOrCredit: String? = null,
    val translatedTitle: String? = null,
    val translatedDescription: String? = null,
    val isTranslated: Boolean = false,
    val apod: ApodResponse? = null
) : BaseModel {

    val displayTitle: String
        get() = if (isTranslated && !translatedTitle.isNullOrBlank()) translatedTitle else title

    val displayDescription: String
        get() = if (isTranslated && !translatedDescription.isNullOrBlank()) {
            translatedDescription
        } else {
            description
        }

    val previewImageUrl: String?
        get() = thumbnailUrl?.takeIf { it.isNotBlank() } ?: imageUrl?.takeIf { it.isNotBlank() }
}

/**
 * Converte um [GlobalSearchItem] em [FavoriteEntity] para abrir na tela de detalhes
 * ou salvar nos Favoritos, preservando o modelo original quando vier da APOD.
 */
fun GlobalSearchItem.toFavoriteEntity(): FavoriteEntity {
    if (apod != null) {
        return apod.toFavoriteEntity()
    }
    val isVideo = contentType == GlobalContentType.VIDEO
    val resolvedMediaType = when {
        isVideo -> "video"
        !imageUrl.isNullOrBlank() -> "image"
        else -> "article"
    }
    val favoriteType = if (source == GlobalSearchSource.APOD) {
        FavoriteType.APOD
    } else {
        FavoriteType.NEWS
    }

    return FavoriteEntity(
        id = id,
        itemType = favoriteType.name,
        title = displayTitle,
        subtitleOrDate = date,
        explanationOrBody = displayDescription,
        imageUrl = if (isVideo) thumbnailUrl else (imageUrl ?: thumbnailUrl),
        extraUrl = if (isVideo) sourceUrl else null,
        mediaType = resolvedMediaType,
        thumbnailUrl = thumbnailUrl,
        permalink = sourceUrl,
        authorOrCopyright = authorOrCredit?.takeIf { it.isNotBlank() } ?: source.label
    )
}

data class GlobalSearchPage(
    val items: List<GlobalSearchItem>,
    val page: Int,
    val hasMore: Boolean,
    val totalResults: Int,
    val searchTerm: String
)
