package com.example.galaxia.data.local

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.model.BaseModel

/**
 * Entidade unificada para persistência de favoritos no GalaxIA.
 * Armazena metadados de itens de qualquer categoria (APOD, Notícias, Curiosidades),
 * mantendo as imagens referenciadas por URL com cache em disco via Coil.
 */
data class FavoriteEntity(
    val id: String,
    val itemType: String,
    val title: String,
    val subtitleOrDate: String,
    val explanationOrBody: String,
    val imageUrl: String?,
    val galleryImageUrls: List<String> = emptyList(),
    val extraUrl: String? = null,
    val hdUrl: String? = null,
    val mediaType: String = "image",
    val thumbnailUrl: String? = null,
    val permalink: String? = null,
    val authorOrCopyright: String? = null,
    val savedAtTimestamp: Long = System.currentTimeMillis()
) : BaseModel

@Suppress("USELESS_ELVIS")
val FavoriteEntity.safeGalleryImageUrls: List<String>
    get() = galleryImageUrls ?: emptyList()

val FavoriteEntity.allImageUrls: List<String>
    get() = buildList {
        imageUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
        safeGalleryImageUrls.forEach { url ->
            if (url.isNotBlank() && url !in this) add(url)
        }
    }

/**
 * Retorna a URL HD se disponível e diferente da imagem padrão.
 */
val FavoriteEntity.displayHdUrl: String?
    get() {
        val candidate = hdUrl ?: extraUrl?.takeIf {
            it != imageUrl && (
                it.endsWith(".jpg", ignoreCase = true) ||
                it.endsWith(".png", ignoreCase = true) ||
                it.endsWith(".jpeg", ignoreCase = true) ||
                it.endsWith(".webp", ignoreCase = true) ||
                it.contains("/apod/image/", ignoreCase = true)
            )
        }
        return candidate?.takeIf { it.isNotBlank() && it != imageUrl }
    }

/**
 * Retorna a URL da miniatura para exibição nos favoritos.
 */
val FavoriteEntity.displayThumbnailUrl: String?
    get() {
        if (!thumbnailUrl.isNullOrBlank()) return thumbnailUrl
        if (mediaType == "video" && !extraUrl.isNullOrBlank()) {
            val ytId = ApodResponse.extractYouTubeVideoId(extraUrl)
            if (ytId != null) {
                return "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
            }
        }
        return imageUrl?.takeIf { it.isNotBlank() }
    }

/**
 * Retorna o permalink oficial da publicação na NASA.
 * Utilizado EXCLUSIVAMENTE para o botão de ação "Abrir no site da NASA".
 */
val FavoriteEntity.officialNasaUrl: String
    get() {
        if (itemType != FavoriteType.APOD.name) {
            return permalink?.takeIf {
                it.startsWith("http://") || it.startsWith("https://")
            } ?: ""
        }
        val candidate = permalink?.takeIf { it.isNotBlank() } ?: extraUrl ?: imageUrl ?: ""
        return if (candidate.contains("science.nasa.gov") || candidate.contains("apod.nasa.gov")) {
            candidate
        } else {
            "https://science.nasa.gov/picture-of-the-day/"
        }
    }

/**
 * Converte um ApodResponse para a entidade unificada de favoritos com dados traduzidos.
 */
fun ApodResponse.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        id = "apod_$date",
        itemType = FavoriteType.APOD.name,
        title = displayTitle,
        subtitleOrDate = date,
        explanationOrBody = displayExplanation,
        imageUrl = displayImageUrl,
        extraUrl = effectiveMediaUrl.takeIf { it.isNotBlank() && it != displayImageUrl },
        hdUrl = hdurl,
        mediaType = mediaType,
        thumbnailUrl = displayThumbnailUrl,
        permalink = officialNasaUrl,
        authorOrCopyright = author
    )
}

/**
 * Converte a entidade de favoritos para ApodResponse quando o item for do tipo APOD.
 */
fun FavoriteEntity.toApodResponse(): ApodResponse {
    return ApodResponse(
        date = subtitleOrDate,
        title = title,
        explanation = explanationOrBody,
        url = extraUrl ?: imageUrl ?: "",
        hdurl = hdUrl,
        mediaType = mediaType,
        thumbnailUrl = thumbnailUrl,
        permalink = officialNasaUrl,
        copyright = authorOrCopyright,
        translatedTitle = title,
        translatedExplanation = explanationOrBody,
        isTranslated = true
    )
}
