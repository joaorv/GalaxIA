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
    @SerializedName("thumbnail_url")
    val thumbnailUrl: String? = null,
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
     * Retorna a URL da imagem de menor resolução (não HD) para pré-carregamento rápido.
     */
    val lowResImageUrl: String
        get() = displayImageUrl

    /**
     * Retorna a URL da imagem em alta definição (HD), se disponível e diferente da imagem não HD.
     */
    val highResImageUrl: String?
        get() = hdurl?.takeIf { it.isNotBlank() && it != displayImageUrl }

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

    /**
     * Retorna o permalink original da publicação na NASA.
     * Utilizado EXCLUSIVAMENTE para o botão de ação "Abrir no site da NASA" / Visitar NASA.
     */
    val officialNasaUrl: String
        get() {
            val candidate = permalink?.takeIf { it.isNotBlank() } ?: url
            return if (candidate.contains("science.nasa.gov") || candidate.contains("apod.nasa.gov")) {
                candidate
            } else {
                "https://science.nasa.gov/picture-of-the-day/"
            }
        }

    /**
     * Retorna a URL real de mídia para exibição e reprodução.
     * Inspeciona 'url' e inspeciona o campo 'basic_html' da nova API WordPress da NASA
     * para extrair iFrames, links de YouTube, Vimeo ou arquivos de vídeo diretos.
     */
    val effectiveMediaUrl: String
        get() {
            // 1. Se 'url' já for YouTube, Vimeo ou arquivo de vídeo direto, utiliza 'url'
            val ytId = extractYouTubeVideoId(url)
            if (ytId != null) return url
            val vimeoId = extractVimeoVideoId(url)
            if (vimeoId != null) return url
            if (isDirectVideoFileUrl(url)) return url

            // 2. Se 'basic_html' estiver presente no JSON da NASA, extrai o iFrame/vídeo incorporado
            if (!basicHtml.isNullOrBlank()) {
                val extracted = extractMediaUrlFromHtml(basicHtml)
                if (!extracted.isNullOrBlank()) {
                    return extracted
                }
            }

            return url
        }

    /**
     * Retorna a URL da miniatura para exibição de vídeos.
     * Tenta utilizar 'thumbnail_url' da API, ou deriva a miniatura de vídeos do YouTube se omitido.
     */
    val displayThumbnailUrl: String?
        get() {
            if (!thumbnailUrl.isNullOrBlank()) return thumbnailUrl
            val targetUrl = effectiveMediaUrl
            val ytId = extractYouTubeVideoId(targetUrl) ?: extractYouTubeVideoId(url)
            if (ytId != null) {
                return "https://img.youtube.com/vi/$ytId/hqdefault.jpg"
            }
            return displayImageUrl.takeIf { it.isNotBlank() }
        }

    /**
     * Identifica se a mídia é um vídeo diretamente reproduzível (MP4, WebM, M3U8, MOV).
     */
    val isDirectVideoUrl: Boolean
        get() = isDirectVideoFileUrl(effectiveMediaUrl) || isDirectVideoFileUrl(url)

    /**
     * Formata a URL do vídeo para incorporação em iFrame/Player (YouTube/Vimeo/Embed).
     */
    val videoEmbedUrl: String
        get() {
            val targetUrl = effectiveMediaUrl
            val ytId = extractYouTubeVideoId(targetUrl) ?: extractYouTubeVideoId(url)
            if (ytId != null) {
                return "https://www.youtube.com/embed/$ytId?autoplay=1&rel=0&enablejsapi=1"
            }
            val vimeoId = extractVimeoVideoId(targetUrl) ?: extractVimeoVideoId(url)
            if (vimeoId != null) {
                return "https://player.vimeo.com/video/$vimeoId?autoplay=1"
            }
            return targetUrl
        }

    companion object {
        fun extractYouTubeVideoId(url: String): String? {
            if (url.isBlank()) return null
            val pattern = Regex(
                "(?:youtube(?:-nocookie)?\\.com/(?:[^/]+/.+/|(?:v|e(?:mbed)?|shorts)/|.*[?&]v=)|youtu\\.be/)([^\"&?/#\\s]{11})",
                RegexOption.IGNORE_CASE
            )
            return pattern.find(url)?.groupValues?.getOrNull(1)
        }

        fun extractVimeoVideoId(url: String): String? {
            if (url.isBlank()) return null
            val pattern = Regex("(?:vimeo\\.com/(?:video/)?)([0-9]+)", RegexOption.IGNORE_CASE)
            return pattern.find(url)?.groupValues?.getOrNull(1)
        }

        /**
         * Extrai a URL real de mídia de um trecho HTML (como o 'basic_html' fornecido pela NASA).
         */
        fun extractMediaUrlFromHtml(html: String): String? {
            if (html.isBlank()) return null

            // 1. Procura por <iframe src="...">
            val iframeRegex = Regex("""<iframe[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val iframeMatch = iframeRegex.find(html)?.groupValues?.getOrNull(1)
            if (!iframeMatch.isNullOrBlank()) {
                return fixRelativeUrl(iframeMatch)
            }

            // 2. Procura por <video src="..."> ou <source src="...">
            val videoRegex = Regex("""<(?:video|source)[^>]+src=["']([^"']+)["']""", RegexOption.IGNORE_CASE)
            val videoMatch = videoRegex.find(html)?.groupValues?.getOrNull(1)
            if (!videoMatch.isNullOrBlank()) {
                return fixRelativeUrl(videoMatch)
            }

            // 3. Procura por links de YouTube soltos no HTML
            val ytRegex = Regex("""https?://(?:www\.)?(?:youtube\.com/(?:embed/|v/|watch\?v=)|youtu\.be/)[a-zA-Z0-9_-]{11}""", RegexOption.IGNORE_CASE)
            val ytMatch = ytRegex.find(html)?.value
            if (!ytMatch.isNullOrBlank()) {
                return ytMatch
            }

            // 4. Procura por links de Vimeo soltos no HTML
            val vimeoRegex = Regex("""https?://(?:player\.)?vimeo\.com/(?:video/)?[0-9]+""", RegexOption.IGNORE_CASE)
            val vimeoMatch = vimeoRegex.find(html)?.value
            if (!vimeoMatch.isNullOrBlank()) {
                return vimeoMatch
            }

            // 5. Procura por arquivos diretos de vídeo (.mp4, .webm, .m3u8, .mov)
            val directRegex = Regex("""https?://[^\s"'<>]+\.(?:mp4|webm|m3u8|mov)(?:\?[^\s"'<>]*)?""", RegexOption.IGNORE_CASE)
            val directMatch = directRegex.find(html)?.value
            if (!directMatch.isNullOrBlank()) {
                return directMatch
            }

            return null
        }

        fun isDirectVideoFileUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val lower = url.lowercase()
            return lower.endsWith(".mp4") || lower.endsWith(".webm") ||
                   lower.endsWith(".m3u8") || lower.endsWith(".m4v") ||
                   lower.endsWith(".mov") || lower.contains("video.media.nasa.gov") ||
                   lower.contains(".mp4?") || lower.contains(".webm?")
        }

        private fun fixRelativeUrl(url: String): String {
            return when {
                url.startsWith("//") -> "https:$url"
                url.startsWith("/") -> "https://science.nasa.gov$url"
                else -> url
            }
        }
    }
}
