package com.example.galaxia.data.model

import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.FavoriteType

/**
 * Categorias da seção de Notícias e Curiosidades Espaciais, ordenadas para o público casual.
 */
enum class NewsCategory(
    val label: String,
    val favoriteType: FavoriteType
) {
    NASA_HIGHLIGHTS("Destaques NASA", FavoriteType.NEWS),
    MISSIONS_DISCOVERIES("Descobertas e Missões", FavoriteType.NEWS),
    UNIVERSE_CURIOSITIES("Curiosidades", FavoriteType.CURIOSITY),
    SPACE_WEATHER("Clima Espacial", FavoriteType.NEWS)
}

/**
 * Ordenação para as listas de Descobertas e Missões e Curiosidades.
 */
enum class NewsSortOrder(val label: String) {
    NEWEST("Mais recentes"),
    OLDEST("Mais antigas")
}

/**
 * Sub-tópico clicável para explorar temas específicos em Descobertas e Curiosidades
 * com consultas precisas na NASA Image Library.
 */
data class NewsSubTopic(
    val id: String,
    val label: String,
    val query: String
)

val NewsCategory.subTopics: List<NewsSubTopic>
    get() = when (this) {
        NewsCategory.MISSIONS_DISCOVERIES -> listOf(
            NewsSubTopic("jwst", "James Webb", "James Webb"),
            NewsSubTopic("mars", "Marte", "Perseverance"),
            NewsSubTopic("artemis", "Lua & Artemis", "Artemis"),
            NewsSubTopic("hubble", "Hubble", "Hubble"),
            NewsSubTopic("iss", "Estação Espacial", "International Space Station")
        )
        NewsCategory.UNIVERSE_CURIOSITIES -> listOf(
            NewsSubTopic("nebula", "Nebulosas", "nebula"),
            NewsSubTopic("galaxy", "Galáxias", "galaxy"),
            NewsSubTopic("black_hole", "Buracos Negros", "black hole"),
            NewsSubTopic("saturn", "Saturno", "saturn"),
            NewsSubTopic("jupiter", "Júpiter", "jupiter"),
            NewsSubTopic("supernova", "Supernovas", "supernova")
        )
        else -> emptyList()
    }

/**
 * Modelo unificado de exibição para notícias, alertas espaciais e curiosidades
 * provenientes das APIs oficiais da NASA.
 */
data class SpaceNewsItem(
    val id: String,
    val category: NewsCategory,
    val sourceName: String,
    val title: String,
    val summary: String,
    val fullDescription: String,
    val date: String,
    val imageUrl: String? = null,
    val galleryImageUrls: List<String> = emptyList(),
    val sourceUrl: String,
    val authorOrCredit: String? = null,
    val subtitleTag: String? = null,
    val metadataChips: List<String> = emptyList(),
    val accentKey: String? = null,
    val translatedTitle: String? = null,
    val translatedSummary: String? = null,
    val translatedFullDescription: String? = null,
    val isTranslated: Boolean = false
) : BaseModel {

    @Suppress("USELESS_ELVIS")
    val safeGalleryImageUrls: List<String>
        get() = galleryImageUrls ?: emptyList()

    @Suppress("USELESS_ELVIS")
    val safeMetadataChips: List<String>
        get() = metadataChips ?: emptyList()

    val allImageUrls: List<String>
        get() = buildList {
            imageUrl?.takeIf { it.isNotBlank() }?.let { add(it) }
            safeGalleryImageUrls.forEach { url ->
                if (url.isNotBlank() && url !in this) add(url)
            }
        }

    val displayTitle: String
        get() = if (isTranslated && !translatedTitle.isNullOrBlank()) translatedTitle else title

    val displaySummary: String
        get() = if (isTranslated && !translatedSummary.isNullOrBlank()) translatedSummary else summary

    val displayFullDescription: String
        get() = if (isTranslated && !translatedFullDescription.isNullOrBlank()) {
            translatedFullDescription
        } else {
            fullDescription
        }
}

/**
 * Converte um [SpaceNewsItem] para [FavoriteEntity] preservando título, descrição completa,
 * URL oficial da fonte e eventual imagem ilustrativa.
 */
fun SpaceNewsItem.toFavoriteEntity(): FavoriteEntity {
    val body = displayFullDescription.ifBlank { displaySummary }
    return FavoriteEntity(
        id = id,
        itemType = category.favoriteType.name,
        title = displayTitle,
        subtitleOrDate = date,
        explanationOrBody = body,
        imageUrl = imageUrl,
        galleryImageUrls = safeGalleryImageUrls,
        mediaType = if (!imageUrl.isNullOrBlank()) "image" else "article",
        permalink = sourceUrl,
        authorOrCopyright = authorOrCredit?.takeIf { it.isNotBlank() } ?: sourceName
    )
}
