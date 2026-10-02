package com.example.galaxia.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de dados para a resposta da API APOD da NASA com suporte a tradução.
 */
data class ApodResponse(
    val date: String,
    val explanation: String,
    val hdurl: String? = null,
    @SerializedName("media_type")
    val mediaType: String,
    @SerializedName("service_version")
    val serviceVersion: String? = "v1",
    val title: String,
    val url: String,
    val copyright: String? = null,
    val translatedTitle: String? = null,
    val translatedExplanation: String? = null,
    val isTranslated: Boolean = false
) : BaseModel {

    /**
     * Título pronto para exibição (traduzido se disponível, caso contrário o original em inglês).
     */
    val displayTitle: String
        get() = if (isTranslated && !translatedTitle.isNullOrBlank()) translatedTitle else title

    /**
     * Explicação pronta para exibição (traduzida se disponível, caso contrário a original em inglês).
     */
    val displayExplanation: String
        get() = if (isTranslated && !translatedExplanation.isNullOrBlank()) translatedExplanation else explanation
}
