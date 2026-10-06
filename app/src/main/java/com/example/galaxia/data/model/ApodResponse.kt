package com.example.galaxia.data.model

import androidx.core.text.HtmlCompat
import com.google.gson.annotations.SerializedName

/**
 * Modelo de dados para a resposta da API APOD da NASA com suporte a tradução.
 * Suporta a estrutura da API WordPress da NASA Science (apod-basic).
 */
data class ApodResponse(
    val date: String = "",
    @SerializedName("post_id")
    val postId: Long? = null,
    val title: String = "",
    val permalink: String? = null,
    @SerializedName("media_type")
    val mediaType: String = "image",
    val explanation: String = "",
    val credit: String? = null,
    val copyright: String? = null,
    val alt: String? = null,
    val url: String = "",
    val hdurl: String? = null,
    @SerializedName("basic_html")
    val basicHtml: String? = null,
    @SerializedName("basic_html_url")
    val basicHtmlUrl: String? = null,
    @SerializedName("service_version")
    val serviceVersion: String? = "v1",
    val translatedTitle: String? = null,
    val translatedExplanation: String? = null,
    val isTranslated: Boolean = false
) : BaseModel {

    /**
     * Título pronto para exibição (traduzido se disponível, caso contrário o original).
     */
    val displayTitle: String
        get() = if (isTranslated && !translatedTitle.isNullOrBlank()) translatedTitle else title

    /**
     * Explicação pronta para exibição (traduzida se disponível, caso contrário a versão limpa original).
     */
    val displayExplanation: String
        get() = if (isTranslated && !translatedExplanation.isNullOrBlank()) translatedExplanation else cleanExplanation

    /**
     * Retorna a URL direta da imagem para exibição nos cards.
     * Prioriza 'url' se for um link de imagem direta; caso contrário, utiliza 'hdurl'.
     */
    val displayImageUrl: String
        get() {
            val isUrlAnImage = url.isNotBlank() && (
                url.endsWith(".jpg", ignoreCase = true) ||
                url.endsWith(".png", ignoreCase = true) ||
                url.endsWith(".jpeg", ignoreCase = true) ||
                url.endsWith(".webp", ignoreCase = true) ||
                url.endsWith(".gif", ignoreCase = true) ||
                url.contains("/apod/image/", ignoreCase = true)
            )

            return when {
                isUrlAnImage -> url
                !hdurl.isNullOrBlank() -> hdurl
                url.isNotBlank() -> url
                else -> ""
            }
        }

    /**
     * Retorna o autor, copyright ou crédito do item APOD sem tags HTML.
     */
    val author: String?
        get() {
            val raw = copyright ?: credit ?: return null
            return HtmlCompat.fromHtml(raw, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace(Regex("(?i)^Image Credit:\\s*"), "")
                .trim()
        }

    /**
     * Retorna a explicação limpa sem tags HTML (como <strong>, <a>, &nbsp;, etc.).
     */
    val cleanExplanation: String
        get() {
            if (explanation.isBlank()) return ""
            return HtmlCompat.fromHtml(explanation, HtmlCompat.FROM_HTML_MODE_LEGACY)
                .toString()
                .replace(Regex("(?i)^Explanation:\\s*"), "")
                .trim()
        }
}
