package com.example.galaxia.data.remote

import com.example.galaxia.data.model.ApodResponse

/**
 * Dados de contingência (Fallback) para exibição fluida caso a API da NASA atinja o limite de requisições.
 */
object ApodMockData {

    val sampleApods = listOf(
        ApodResponse(
            date = "2026-10-01",
            title = "A Nebulosa da Laguna em Luz Visível",
            explanation = "A majestosa Nebulosa da Laguna é lar de muitas estrelas jovens e quentes e de poeira cósmica em expansão. Localizada a cerca de 5.000 anos-luz de distância na direção da constelação do Sagitário, esta região de formação estelar se estende por mais de 100 anos-luz.",
            url = "https://images.unsplash.com/photo-1451187580459-43490279c0fa?q=80&w=1200",
            hdurl = "https://images.unsplash.com/photo-1451187580459-43490279c0fa",
            mediaType = "image",
            copyright = "NASA / ESA / Hubble",
            translatedTitle = "A Nebulosa da Laguna em Luz Visível",
            translatedExplanation = "A majestosa Nebulosa da Laguna é lar de muitas estrelas jovens e quentes e de poeira cósmica em expansão. Localizada a cerca de 5.000 anos-luz de distância na direção da constelação do Sagitário, esta região de formação estelar se estende por mais de 100 anos-luz.",
            isTranslated = true
        ),
        ApodResponse(
            date = "2026-09-30",
            title = "Pilares da Criação na Nebulosa da Águia",
            explanation = "Esta icônica imagem capturada pelo Telescópio Espacial James Webb revela detalhes impressionantes de densas colunas de poeira e gás onde novas estrelas estão se formando ativamente na Nebulosa da Águia (M16).",
            url = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa?q=80&w=1200",
            hdurl = "https://images.unsplash.com/photo-1446776811953-b23d57bd21aa",
            mediaType = "image",
            copyright = "NASA / ESA / CSA / STScI",
            translatedTitle = "Pilares da Criação na Nebulosa da Águia",
            translatedExplanation = "Esta icônica imagem capturada pelo Telescópio Espacial James Webb revela detalhes impressionantes de densas colunas de poeira e gás onde novas estrelas estão se formando ativamente na Nebulosa da Águia (M16).",
            isTranslated = true
        ),
        ApodResponse(
            date = "2026-09-29",
            title = "Galáxia de Andrômeda em Ultra Alta Definição",
            explanation = "Andrômeda (M31) é a galáxia espiral grande mais próxima da nossa Via Láctea, situada a aproximadamente 2,5 milhões de anos-luz de distância. Esta composição cobre mais de 200.000 anos-luz de extensão.",
            url = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86?q=80&w=1200",
            hdurl = "https://images.unsplash.com/photo-1506703719100-a0f3a48c0f86",
            mediaType = "image",
            copyright = "NASA / JPL-Caltech",
            translatedTitle = "Galáxia de Andrômeda em Ultra Alta Definição",
            translatedExplanation = "Andrômeda (M31) é a galáxia espiral grande mais próxima da nossa Via Láctea, situada a aproximadamente 2,5 milhões de anos-luz de distância. Esta composição cobre mais de 200.000 anos-luz de extensão.",
            isTranslated = true
        )
    )

    fun getMockByDate(date: String): ApodResponse {
        return sampleApods.find { it.date == date } ?: sampleApods.first().copy(
            date = date,
            title = "Foto Astronômica do Dia ($date)",
            translatedTitle = "Foto Astronômica do Dia ($date)"
        )
    }
}
