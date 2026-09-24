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
    val extraUrl: String? = null,
    val mediaType: String = "image",
    val authorOrCopyright: String? = null,
    val savedAtTimestamp: Long = System.currentTimeMillis()
) : BaseModel

/**
 * Converte um ApodResponse para a entidade unificada de favoritos.
 */
fun ApodResponse.toFavoriteEntity(): FavoriteEntity {
    return FavoriteEntity(
        id = "apod_$date",
        itemType = FavoriteType.APOD.name,
        title = title,
        subtitleOrDate = date,
        explanationOrBody = explanation,
        imageUrl = url,
        extraUrl = hdurl,
        mediaType = mediaType,
        authorOrCopyright = copyright
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
        url = imageUrl ?: "",
        hdurl = extraUrl,
        mediaType = mediaType,
        copyright = authorOrCopyright
    )
}
