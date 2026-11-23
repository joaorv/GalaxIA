package com.example.galaxia.data.local

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.model.BaseModel
import com.example.galaxia.data.model.SpaceHighlight

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
    val extraUrl: String? = null,
    val hdUrl: String? = null,
    val mediaType: String = "image",
    val authorOrCopyright: String? = null,
    val savedAtTimestamp: Long = System.currentTimeMillis()
) : BaseModel

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
        extraUrl = permalink ?: hdurl ?: url,
        hdUrl = hdurl,
        mediaType = mediaType,
        authorOrCopyright = author
    )
}

/**
 * Converte uma notícia (DONKI) ou curiosidade (NeoWs) para a entidade de favoritos.
 * Esses itens não têm imagem; a fonte fica em [FavoriteEntity.authorOrCopyright].
 */
fun SpaceHighlight.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        id = id,
        itemType = type.name,
        title = title,
        subtitleOrDate = date,
        explanationOrBody = body,
        imageUrl = null,
        extraUrl = sourceUrl,
        mediaType = MEDIA_TYPE_TEXT,
        authorOrCopyright = source
    )
}

/** Tipo de mídia dos itens só de texto (notícias e curiosidades). */
const val MEDIA_TYPE_TEXT = "text"

/** true para notícias e curiosidades, que são exibidas sem área de imagem. */
val FavoriteEntity.isTextOnly: Boolean
    get() = itemType != FavoriteType.APOD.name

/**
 * Converte a entidade de favoritos para ApodResponse quando o item for do tipo APOD.
 */
fun FavoriteEntity.toApodResponse(): ApodResponse {
    return ApodResponse(
        date = subtitleOrDate,
        title = title,
        explanation = explanationOrBody,
        url = imageUrl ?: "",
        hdurl = hdUrl ?: extraUrl,
        mediaType = mediaType,
        copyright = authorOrCopyright,
        translatedTitle = title,
        translatedExplanation = explanationOrBody,
        isTranslated = true
    )
}
