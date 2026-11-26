package com.example.galaxia.data.remote

import com.example.galaxia.data.model.DonkiCmeDto
import com.example.galaxia.data.model.DonkiFlareDto
import com.example.galaxia.data.model.DonkiGstDto
import com.example.galaxia.data.model.DonkiNotificationDto
import com.example.galaxia.data.model.NasaImageItemDto
import com.example.galaxia.data.model.NasaSciencePostDto
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.SpaceNewsItem

/**
 * Funções puras (sem dependências Android) para normalizar respostas das APIs
 * NASA Science, DONKI e NASA Image Library em [SpaceNewsItem], preservando
 * rigorosamente os dados científicos retornados sem inventar informações.
 */
object SpaceNewsFormatters {

    private const val SUMMARY_MAX_CHARS = 280
    private val ISO_DATE_PREFIX_REGEX = Regex("""^\d{4}-\d{2}-\d{2}""")
    private val HTML_SCRIPT_STYLE_REGEX = Regex("""<(script|style)[^>]*>[\s\S]*?</\1>""", RegexOption.IGNORE_CASE)
    private val HTML_BLOCK_BREAK_REGEX = Regex("""</?(p|div|br|li|h[1-6]|blockquote)[^>]*>""", RegexOption.IGNORE_CASE)
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
    private val NUMERIC_ENTITY_REGEX = Regex("""&#(x?[0-9a-fA-F]+);""")
    private val URL_REGEX = Regex("""https?://\S+""")
    private val WHITESPACE_REGEX = Regex("""\s+""")
    private val FLARE_CLASS_REGEX = Regex("""\b([XMC]\d+(?:\.\d+)?)\b""")
    private val SPEED_REGEX = Regex("""\b(\d{3,4})\s*km/s\b""", RegexOption.IGNORE_CASE)
    private val KP_REGEX = Regex("""\bKp\s*[:=]?\s*(\d(?:\.\d)?)\b""", RegexOption.IGNORE_CASE)
    private val VIDEO_URL_EXTENSIONS = listOf(".mp4", ".mov", ".webm", ".m3u8", ".m4v")

    fun mapNasaSciencePost(dto: NasaSciencePostDto): SpaceNewsItem? {
        val id = dto.id ?: return null
        val link = dto.link?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val title = cleanHtml(dto.title?.rendered.orEmpty())
        if (title.isBlank()) return null

        val fullText = cleanHtml(dto.content?.rendered.orEmpty())
        val excerptText = cleanHtml(dto.excerpt?.rendered.orEmpty())
        val effectiveFull = fullText.ifBlank { excerptText }
        val effectiveSummary = truncateSummary(excerptText.ifBlank { effectiveFull })

        val featured = dto.embedded?.featuredMedia?.firstOrNull()
        val sizes = featured?.mediaDetails?.sizes
        val imageUrl = sizes?.get("medium_large")?.sourceUrl
            ?: sizes?.get("large")?.sourceUrl
            ?: sizes?.get("medium")?.sourceUrl
            ?: featured?.sourceUrl

        val author = dto.embedded?.authors?.firstOrNull()?.name?.trim()?.takeIf { it.isNotBlank() }
        val wordCount = effectiveFull.split(WHITESPACE_REGEX).count { it.isNotBlank() }
        val readMinutes = if (wordCount > 0) (wordCount / 180).coerceAtLeast(1) else null

        val chips = buildList {
            readMinutes?.let { add("~$it min de leitura") }
        }

        return SpaceNewsItem(
            id = "nasasci_$id",
            category = NewsCategory.NASA_HIGHLIGHTS,
            sourceName = "NASA Science",
            title = title,
            summary = effectiveSummary,
            fullDescription = effectiveFull,
            date = normalizeIsoDate(dto.date),
            imageUrl = imageUrl?.takeIf { it.isNotBlank() && !isVideoUrl(it) },
            sourceUrl = link,
            authorOrCredit = author ?: "NASA Science",
            subtitleTag = "Artigo Oficial NASA",
            metadataChips = chips,
            isTranslated = false
        )
    }

    fun mapDonkiNotification(dto: DonkiNotificationDto): SpaceNewsItem? {
        val rawId = dto.messageID?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val sourceUrl = dto.messageURL?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val typeCode = dto.messageType?.trim().orEmpty()
        val date = normalizeIsoDate(dto.messageIssueTime)
        val title = donkiTypeTitle(typeCode)

        val cleanedBody = cleanDonkiBody(dto.messageBody.orEmpty())
        val summary = truncateSummary(cleanedBody)
        val metricChips = extractDonkiMetrics(typeCode, dto.messageBody.orEmpty())

        return SpaceNewsItem(
            id = "donki_$rawId",
            category = NewsCategory.SPACE_WEATHER,
            sourceName = "NASA DONKI",
            title = title,
            summary = summary,
            fullDescription = cleanedBody,
            date = date,
            imageUrl = null,
            sourceUrl = sourceUrl,
            authorOrCredit = if (typeCode.isNotBlank()) "NASA CCMC • $typeCode" else "NASA CCMC",
            subtitleTag = donkiCasualSubtitle(typeCode),
            metadataChips = metricChips,
            accentKey = typeCode.lowercase().ifBlank { "alert" },
            isTranslated = cleanedBody.isBlank()
        )
    }

    fun mapDonkiFlare(dto: DonkiFlareDto): SpaceNewsItem? {
        val id = dto.flrID?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val sourceUrl = dto.link?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val classPart = dto.classType?.trim()?.takeIf { it.isNotBlank() }?.let { "Classe $it" }
        val regionPart = dto.sourceLocation?.trim()?.takeIf { it.isNotBlank() }?.let { "Região $it" }
        val note = cleanWhitespace(dto.note.orEmpty())
        val details = listOfNotNull(classPart, regionPart, note.takeIf { it.isNotBlank() })
            .joinToString(" • ")

        return SpaceNewsItem(
            id = "donki_flr_$id",
            category = NewsCategory.SPACE_WEATHER,
            sourceName = "NASA DONKI",
            title = listOfNotNull("Erupção Solar", classPart).joinToString(" — "),
            summary = truncateSummary(details),
            fullDescription = details,
            date = normalizeIsoDate(dto.peakTime ?: dto.beginTime),
            imageUrl = null,
            sourceUrl = sourceUrl,
            authorOrCredit = "NASA CCMC • FLR",
            subtitleTag = donkiCasualSubtitle("FLR"),
            metadataChips = listOfNotNull(classPart, regionPart),
            accentKey = "flr",
            isTranslated = note.isBlank()
        )
    }

    fun mapDonkiCme(dto: DonkiCmeDto): SpaceNewsItem? {
        val id = dto.activityID?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val sourceUrl = dto.link?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val note = cleanWhitespace(dto.note.orEmpty())

        return SpaceNewsItem(
            id = "donki_cme_$id",
            category = NewsCategory.SPACE_WEATHER,
            sourceName = "NASA DONKI",
            title = "Ejeção de Massa Coronal (CME)",
            summary = truncateSummary(note),
            fullDescription = note,
            date = normalizeIsoDate(dto.startTime),
            imageUrl = null,
            sourceUrl = sourceUrl,
            authorOrCredit = "NASA CCMC • CME",
            subtitleTag = donkiCasualSubtitle("CME"),
            metadataChips = extractDonkiMetrics("CME", note),
            accentKey = "cme",
            isTranslated = note.isBlank()
        )
    }

    fun mapDonkiGst(dto: DonkiGstDto): SpaceNewsItem? {
        val id = dto.gstID?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val sourceUrl = dto.link?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val maxKp = dto.allKpIndex.orEmpty().mapNotNull { it.kpIndex }.maxOrNull()
        val kpText = maxKp?.let { "Índice Kp máximo registrado: $it." }.orEmpty()

        return SpaceNewsItem(
            id = "donki_gst_$id",
            category = NewsCategory.SPACE_WEATHER,
            sourceName = "NASA DONKI",
            title = "Tempestade Geomagnética",
            summary = kpText,
            fullDescription = kpText,
            date = normalizeIsoDate(dto.startTime),
            imageUrl = null,
            sourceUrl = sourceUrl,
            authorOrCredit = "NASA CCMC • GST",
            subtitleTag = donkiCasualSubtitle("GST"),
            metadataChips = listOfNotNull(maxKp?.let { "Índice Kp $it" }),
            accentKey = "gst",
            isTranslated = true
        )
    }

    fun mapNasaImageItem(dto: NasaImageItemDto, category: NewsCategory): SpaceNewsItem? {
        val data = dto.data.orEmpty().firstOrNull() ?: return null
        if (!data.mediaType.equals("image", ignoreCase = true)) return null

        val nasaId = data.nasaId?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val title = data.title?.trim().takeUnless { it.isNullOrBlank() } ?: return null

        val previewUrl = dto.links
            .orEmpty()
            .firstOrNull { link ->
                link.rel.equals("preview", ignoreCase = true) &&
                    (link.render == null || link.render.equals("image", ignoreCase = true)) &&
                    !link.href.isNullOrBlank() &&
                    !isVideoUrl(link.href)
            }
            ?.href
            ?.trim()
            ?: return null

        val rawDescription = data.description?.takeIf { it.isNotBlank() }
            ?: data.description508?.takeIf { it.isNotBlank() }
            ?: ""
        val cleanDesc = cleanHtml(rawDescription)
        val credit = data.photographer?.takeIf { it.isNotBlank() }
            ?: data.secondaryCreator?.takeIf { it.isNotBlank() }
            ?: data.center?.takeIf { it.isNotBlank() }?.let { "NASA • $it" }

        val dateIso = normalizeIsoDate(data.dateCreated)
        val chips = buildList {
            data.center?.trim()?.takeIf { it.isNotBlank() }?.let { add("Centro $it") }
            if (dateIso.length >= 4) add(dateIso.take(4))
        }

        return SpaceNewsItem(
            id = "nasaimg_${category.name.lowercase()}_$nasaId",
            category = category,
            sourceName = "NASA Image Library",
            title = title,
            summary = truncateSummary(cleanDesc),
            fullDescription = cleanDesc,
            date = dateIso,
            imageUrl = previewUrl,
            sourceUrl = "https://images.nasa.gov/details/$nasaId",
            authorOrCredit = credit,
            metadataChips = chips,
            isTranslated = false
        )
    }

    /**
     * Agrupa itens com imagem que possuem o mesmo título normalizado em um único card,
     * preservando a ordem de aparição e reunindo as diferentes fotos em [SpaceNewsItem.galleryImageUrls].
     * Alertas sem imagem (como boletins do DONKI) permanecem individuais.
     */
    fun groupDuplicateTitleItems(
        items: List<SpaceNewsItem>,
        maxPhotosPerCard: Int = 10
    ): List<SpaceNewsItem> {
        if (items.size <= 1) return items
        val grouped = mutableListOf<SpaceNewsItem>()
        val titleToIndex = HashMap<String, Int>()

        for (item in items) {
            val itemImages = item.allImageUrls
            val titleKey = cleanWhitespace(item.title).lowercase()

            if (itemImages.isEmpty() || titleKey.isBlank()) {
                grouped.add(item)
                continue
            }

            val existingIndex = titleToIndex[titleKey]
            if (existingIndex == null) {
                titleToIndex[titleKey] = grouped.size
                grouped.add(item)
            } else {
                val existing = grouped[existingIndex]
                val mergedImages = (existing.allImageUrls + itemImages)
                    .distinct()
                    .take(maxPhotosPerCard)

                val primaryDesc = existing.fullDescription.ifBlank { item.fullDescription }
                val primarySummary = existing.summary.ifBlank { item.summary }

                grouped[existingIndex] = existing.copy(
                    imageUrl = mergedImages.firstOrNull() ?: existing.imageUrl,
                    galleryImageUrls = mergedImages.drop(1),
                    fullDescription = primaryDesc,
                    summary = primarySummary
                )
            }
        }
        return grouped
    }

    fun cleanDonkiBody(rawBody: String): String {
        if (rawBody.isBlank()) return ""

        val withoutDisclaimer = rawBody.substringBefore("## Disclaimer").substringBefore("## Notes on")
        val summarySection = if (withoutDisclaimer.contains("## Summary:")) {
            withoutDisclaimer.substringAfter("## Summary:")
        } else {
            withoutDisclaimer
        }

        val lines = summarySection.lines()
            .map { it.trim() }
            .filter { line ->
                line.isNotBlank() &&
                    !line.startsWith("##") &&
                    !line.startsWith("Message Type:", ignoreCase = true) &&
                    !line.startsWith("Message Issue Date:", ignoreCase = true) &&
                    !line.startsWith("Message ID:", ignoreCase = true)
            }
            .map { line -> line.replace(URL_REGEX, "").trim() }
            .filter { it.isNotBlank() }

        return cleanWhitespace(lines.joinToString(" "))
    }

    internal fun extractDonkiMetrics(typeCode: String, rawBody: String): List<String> {
        val metrics = mutableListOf<String>()
        if (typeCode.isNotBlank()) {
            metrics.add("Tipo $typeCode")
        }
        FLARE_CLASS_REGEX.find(rawBody)?.groupValues?.getOrNull(1)?.let {
            metrics.add("Classe $it")
        }
        SPEED_REGEX.find(rawBody)?.groupValues?.getOrNull(1)?.let {
            metrics.add("$it km/s")
        }
        KP_REGEX.find(rawBody)?.groupValues?.getOrNull(1)?.let {
            metrics.add("Kp $it")
        }
        return metrics.distinct()
    }

    fun normalizeIsoDate(rawDate: String?): String {
        if (rawDate.isNullOrBlank()) return ""
        return ISO_DATE_PREFIX_REGEX.find(rawDate.trim())?.value ?: ""
    }

    fun cleanHtml(raw: String): String {
        if (raw.isBlank()) return ""
        val withoutScripts = raw.replace(HTML_SCRIPT_STYLE_REGEX, " ")
        val withBreaks = withoutScripts.replace(HTML_BLOCK_BREAK_REGEX, " ")
        val noTags = withBreaks.replace(HTML_TAG_REGEX, " ")
        val decodedNamed = noTags
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&apos;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&mdash;", "—")
            .replace("&ndash;", "–")
            .replace("&hellip;", "...")
            .replace("&rsquo;", "'")
            .replace("&lsquo;", "'")
            .replace("&rdquo;", "\"")
            .replace("&ldquo;", "\"")

        val decodedNumeric = NUMERIC_ENTITY_REGEX.replace(decodedNamed) { match ->
            val rawCode = match.groupValues[1]
            val codePoint = if (rawCode.startsWith("x", ignoreCase = true)) {
                rawCode.drop(1).toIntOrNull(16)
            } else {
                rawCode.toIntOrNull(10)
            }
            if (codePoint != null && codePoint in 32..0x10FFFF) {
                String(Character.toChars(codePoint))
            } else {
                " "
            }
        }
        return cleanWhitespace(decodedNumeric)
    }

    private fun cleanWhitespace(text: String): String =
        text.replace(WHITESPACE_REGEX, " ").trim()

    private fun truncateSummary(text: String): String {
        if (text.length <= SUMMARY_MAX_CHARS) return text
        val sliced = text.take(SUMMARY_MAX_CHARS)
        val lastSpace = sliced.lastIndexOf(' ')
        val cut = if (lastSpace > SUMMARY_MAX_CHARS / 2) sliced.take(lastSpace) else sliced
        return "$cut..."
    }

    private fun isVideoUrl(url: String): Boolean {
        val lower = url.lowercase().substringBefore('?')
        return VIDEO_URL_EXTENSIONS.any { lower.endsWith(it) } ||
            lower.contains("video.media.nasa.gov") ||
            lower.contains("/video/")
    }

    fun extractNasaMediaDetailsId(url: String?): String? {
        if (url.isNullOrBlank()) return null
        val match = Regex("""https?://images\.nasa\.gov/details/([^/?#]+)""", RegexOption.IGNORE_CASE)
            .find(url.trim())
        return match?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    fun selectBestNasaVideoUrl(assetHrefs: List<String>): String? {
        val mp4Candidates = assetHrefs
            .map { it.trim() }
            .filter { href ->
                href.isNotBlank() &&
                    href.substringBefore('?').lowercase().endsWith(".mp4")
            }
        if (mp4Candidates.isEmpty()) return null

        val preferred = mp4Candidates.firstOrNull { it.contains("~medium.mp4", ignoreCase = true) }
            ?: mp4Candidates.firstOrNull { it.contains("~small.mp4", ignoreCase = true) }
            ?: mp4Candidates.firstOrNull { it.contains("~mobile.mp4", ignoreCase = true) }
            ?: mp4Candidates.firstOrNull { it.contains("~large.mp4", ignoreCase = true) }
            ?: mp4Candidates.first()

        val upgraded = if (preferred.startsWith("http://", ignoreCase = true)) {
            "https://" + preferred.removePrefix("http://").removePrefix("HTTP://")
        } else {
            preferred
        }
        return upgraded.replace(" ", "%20")
    }

    private fun donkiTypeTitle(messageType: String): String = when (messageType.uppercase()) {
        "FLR" -> "Erupção Solar Detectada"
        "CME" -> "Ejeção de Massa Coronal (CME)"
        "GST" -> "Tempestade Geomagnética"
        "SEP" -> "Evento de Partículas Energéticas Solares"
        "IPS" -> "Choque Interplanetário"
        "HSS" -> "Corrente de Vento Solar de Alta Velocidade"
        "RBE" -> "Aumento no Cinturão de Radiação"
        "MPC" -> "Cruzamento da Magnetopausa"
        "REPORT" -> "Boletim de Clima Espacial"
        else -> "Alerta de Clima Espacial"
    }

    private fun donkiCasualSubtitle(messageType: String): String = when (messageType.uppercase()) {
        "FLR" -> "Explosão de radiação na superfície do Sol"
        "CME" -> "Grande nuvem de plasma solar lançada pelo espaço"
        "GST" -> "Pode intensificar auroras polares e afetar satélites"
        "SEP" -> "Fluxo intenso de partículas solares de alta energia"
        "IPS" -> "Onda de choque detectada no vento solar"
        "HSS" -> "Corrente rápida de partículas solares rumo à Terra"
        "RBE" -> "Elevação de partículas no cinturão magnético terrestre"
        "MPC" -> "Compressão temporária do campo magnético da Terra"
        "REPORT" -> "Resumo semanal de atividade solar observado pela NASA"
        else -> "Monitoramento da atividade solar e campo magnético"
    }
}
