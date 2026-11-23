package com.example.galaxia.data.model

import com.example.galaxia.data.local.FavoriteType

/**
 * Item de conteúdo extra do feed, sem imagem: uma notícia de clima espacial (DONKI)
 * ou uma curiosidade sobre asteroides próximos (NeoWs). Os textos já chegam em português.
 */
data class SpaceHighlight(
    val id: String,
    val type: FavoriteType,
    val title: String,
    /** Data ISO (yyyy-MM-dd) do evento ou da publicação. */
    val date: String,
    val body: String,
    /** Nome da fonte exibido no card, ex.: "NASA DONKI". */
    val source: String,
    val sourceUrl: String?,
    /** Destaque curto opcional, ex.: "Potencialmente perigoso". */
    val badge: String? = null
) : BaseModel

/**
 * Item exibido na lista do feed: uma foto APOD ou um destaque (notícia/curiosidade).
 */
sealed interface FeedItem {
    val key: String

    data class Apod(val apod: ApodResponse) : FeedItem {
        override val key: String get() = "apod_${apod.date}"
    }

    data class Highlight(val highlight: SpaceHighlight) : FeedItem {
        override val key: String get() = highlight.id
    }
}

/**
 * Intercala os destaques entre as fotos: um destaque a cada [apodsPerHighlight] fotos.
 * Destaques que sobrarem vão para o fim da lista.
 */
fun mergeFeedItems(
    apods: List<ApodResponse>,
    highlights: List<SpaceHighlight>,
    apodsPerHighlight: Int = 2
): List<FeedItem> {
    if (highlights.isEmpty()) return apods.map { FeedItem.Apod(it) }

    val result = ArrayList<FeedItem>(apods.size + highlights.size)
    val pending = highlights.iterator()
    apods.forEachIndexed { index, apod ->
        result += FeedItem.Apod(apod)
        if ((index + 1) % apodsPerHighlight == 0 && pending.hasNext()) {
            result += FeedItem.Highlight(pending.next())
        }
    }
    pending.forEach { result += FeedItem.Highlight(it) }
    return result
}
