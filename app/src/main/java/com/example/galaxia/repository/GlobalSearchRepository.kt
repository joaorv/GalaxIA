package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.model.EonetEventDto
import com.example.galaxia.data.model.GlobalContentType
import com.example.galaxia.data.model.GlobalSearchItem
import com.example.galaxia.data.model.GlobalSearchPage
import com.example.galaxia.data.model.GlobalSearchSource
import com.example.galaxia.data.model.NasaImageItemDto
import com.example.galaxia.data.model.SearchFilter
import com.example.galaxia.data.remote.DonkiApiService
import com.example.galaxia.data.remote.EonetApiService
import com.example.galaxia.data.remote.NasaImagesApiService
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.remote.SpaceNewsFormatters
import com.example.galaxia.data.translation.AppTranslation
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.text.Normalizer
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

/**
 * Repositório da Pesquisa Global do GalaxIA.
 * Consulta em paralelo a NASA Image & Video Library (fotos e vídeos), APOD, DONKI e EONET,
 * com tradução inteligente PT <-> EN, cache em memória, isolamento de falhas por API e paginação.
 */
class GlobalSearchRepository(
    private val nasaImagesApiService: NasaImagesApiService = RetrofitClient.nasaImagesApiService,
    private val donkiApiService: DonkiApiService = RetrofitClient.donkiApiService,
    private val eonetApiService: EonetApiService = RetrofitClient.eonetApiService,
    private val apodSearchProvider: suspend (query: String, englishTerm: String) -> List<ApodResponse> = { q, term ->
        ApodRepository().searchApods(
            query = q,
            page = 1,
            searchTerm = term,
            filter = SearchFilter.ALL,
            perPage = APOD_MAX_ITEMS
        ).items
    },
    private val translationService: TranslationService = AppTranslation.service
) : BaseRepository {

    private data class TimedItems(val items: List<GlobalSearchItem>, val cachedAtMillis: Long)
    private data class CachedSearchEntry(val page: GlobalSearchPage, val cachedAtMillis: Long)

    private val queryCacheMutex = Mutex()
    private val queryCache = object : LinkedHashMap<String, CachedSearchEntry>(32, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, CachedSearchEntry>?): Boolean =
            size > MAX_QUERY_CACHE_ENTRIES
    }

    private val donkiCacheMutex = Mutex()
    private var donkiCache: TimedItems? = null

    private val eonetCacheMutex = Mutex()
    private var eonetCache: TimedItems? = null

    private val translationCache = ConcurrentHashMap<String, Pair<String, String>>()

    suspend fun search(
        query: String,
        page: Int = 1,
        searchTerm: String? = null,
        forceRefresh: Boolean = false
    ): GlobalSearchPage {
        val trimmedQuery = query.trim()
        val cacheKey = "${trimmedQuery.lowercase()}:$page"
        val now = System.currentTimeMillis()

        if (!forceRefresh) {
            queryCacheMutex.withLock {
                val cached = queryCache[cacheKey]
                if (cached != null && now - cached.cachedAtMillis <= QUERY_CACHE_TTL_MS) {
                    return cached.page
                }
            }
        }

        val englishTerm = searchTerm ?: resolveEnglishTerm(trimmedQuery)

        if (page > 1) {
            val mediaPage = fetchMediaPage(englishTerm, trimmedQuery, page)
            val result = GlobalSearchPage(
                items = mediaPage.items,
                page = page,
                hasMore = mediaPage.hasMore,
                totalResults = mediaPage.totalHits,
                searchTerm = englishTerm
            )
            queryCacheMutex.withLock {
                queryCache[cacheKey] = CachedSearchEntry(result, System.currentTimeMillis())
            }
            return result
        }

        val normalizedPt = normalizeForMatching(trimmedQuery)
        val normalizedEn = normalizeForMatching(englishTerm)
        val querySpaceWeather = isSpaceWeatherQuery(normalizedPt, normalizedEn)
        val queryNaturalEvent = isNaturalEventQuery(normalizedPt, normalizedEn)

        val result = coroutineScope {
            val mediaDeferred = async {
                runCatchingApi { fetchMediaPage(englishTerm, trimmedQuery, page = 1) }
            }
            val apodDeferred = async {
                runCatchingApi { fetchApodMatches(trimmedQuery, englishTerm) }
            }
            val donkiDeferred = if (querySpaceWeather) {
                async { runCatchingApi { fetchDonkiMatches(normalizedPt, normalizedEn, forceRefresh) } }
            } else {
                null
            }
            val eonetDeferred = if (queryNaturalEvent) {
                async { runCatchingApi { fetchEonetMatches(normalizedPt, normalizedEn, forceRefresh) } }
            } else {
                null
            }

            val mediaResult = mediaDeferred.await()
            val apodResult = apodDeferred.await()
            val donkiResult = donkiDeferred?.await()
            val eonetResult = eonetDeferred?.await()

            val resultsList = listOfNotNull(mediaResult, apodResult, donkiResult, eonetResult)
            val allFailed = resultsList.all { it.isFailure }
            if (allFailed) {
                val firstFailure = resultsList.firstNotNullOfOrNull { it.exceptionOrNull() }
                if (firstFailure != null) throw firstFailure
            }

            val mediaPage = mediaResult.getOrNull() ?: MediaPage(emptyList(), 0, false)
            val apodItems = apodResult.getOrNull().orEmpty()
            val donkiItems = donkiResult?.getOrNull().orEmpty()
            val eonetItems = eonetResult?.getOrNull().orEmpty()

            val combined = mergeAndDeduplicate(
                donkiItems = donkiItems,
                eonetItems = eonetItems,
                apodItems = apodItems,
                mediaItems = mediaPage.items
            )

            val extraCount = donkiItems.size + eonetItems.size + apodItems.size
            val total = maxOf(combined.size, mediaPage.totalHits + extraCount)

            GlobalSearchPage(
                items = combined,
                page = 1,
                hasMore = mediaPage.hasMore,
                totalResults = total,
                searchTerm = englishTerm
            )
        }

        queryCacheMutex.withLock {
            queryCache[cacheKey] = CachedSearchEntry(result, System.currentTimeMillis())
        }
        return result
    }

    suspend fun translateItem(item: GlobalSearchItem): GlobalSearchItem {
        if (item.isTranslated &&
            (!item.translatedDescription.isNullOrBlank() || item.description.isBlank())
        ) {
            return item
        }

        translationCache[item.id]?.let { (titlePt, descPt) ->
            return item.copy(
                translatedTitle = titlePt,
                translatedDescription = descPt,
                isTranslated = true
            )
        }

        return try {
            val titlePt = if (item.source == GlobalSearchSource.DONKI ||
                !item.translatedTitle.isNullOrBlank()
            ) {
                item.translatedTitle ?: item.title
            } else {
                translationService.translateText(item.title, "en", "pt")
            }

            val isPortugueseFallback = item.source == GlobalSearchSource.EONET &&
                item.description.startsWith("Evento natural de ")
            val descPt = when {
                item.description.isBlank() -> ""
                isPortugueseFallback -> item.description
                else -> translationService.translateText(item.description, "en", "pt")
            }

            translationCache[item.id] = Pair(titlePt, descPt)
            item.copy(
                translatedTitle = titlePt,
                translatedDescription = descPt,
                isTranslated = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            item
        }
    }

    suspend fun translateTitleOnly(item: GlobalSearchItem): GlobalSearchItem {
        if (item.isTranslated && !item.translatedTitle.isNullOrBlank()) return item
        if (item.source == GlobalSearchSource.DONKI) {
            return item.copy(translatedTitle = item.title, isTranslated = item.description.isBlank())
        }
        return try {
            val titlePt = translationService.translateText(item.title, "en", "pt")
            item.copy(translatedTitle = titlePt, isTranslated = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            item
        }
    }

    internal suspend fun resolveEnglishTerm(query: String): String {
        val normalized = normalizeForMatching(query)
        GLOSSARY_PT_TO_EN[normalized]?.let { return it }

        return try {
            val translated = translationService.translateText(query, "pt", "en").trim()
            translated.ifBlank { query }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            query
        }
    }

    private data class MediaPage(
        val items: List<GlobalSearchItem>,
        val totalHits: Int,
        val hasMore: Boolean
    )

    private suspend fun fetchMediaPage(
        englishTerm: String,
        originalQuery: String,
        page: Int
    ): MediaPage {
        var response = nasaImagesApiService.searchImages(
            query = englishTerm,
            mediaType = "image,video",
            page = page,
            pageSize = MEDIA_PAGE_SIZE
        )
        var items = response.collection?.items.orEmpty().mapNotNull(::mapNasaMediaItem)

        if (page == 1 && items.isEmpty() && !englishTerm.equals(originalQuery, ignoreCase = true)) {
            response = nasaImagesApiService.searchImages(
                query = originalQuery,
                mediaType = "image,video",
                page = 1,
                pageSize = MEDIA_PAGE_SIZE
            )
            items = response.collection?.items.orEmpty().mapNotNull(::mapNasaMediaItem)
        }

        val totalHits = response.collection?.metadata?.totalHits ?: items.size
        val hasMore = items.isNotEmpty() && (page * MEDIA_PAGE_SIZE < totalHits)
        return MediaPage(items = items.sortedByDescending { it.date }, totalHits = totalHits, hasMore = hasMore)
    }

    private fun mapNasaMediaItem(dto: NasaImageItemDto): GlobalSearchItem? {
        val data = dto.data.orEmpty().firstOrNull() ?: return null
        val nasaId = data.nasaId?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val title = data.title?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val rawMediaType = data.mediaType?.trim()?.lowercase() ?: "image"

        val contentType = when (rawMediaType) {
            "image" -> GlobalContentType.PHOTO
            "video" -> GlobalContentType.VIDEO
            else -> GlobalContentType.SCIENTIFIC_RECORD
        }

        val previewLink = dto.links.orEmpty()
            .firstOrNull { link ->
                !link.href.isNullOrBlank() &&
                    !isDirectVideoUrl(link.href) &&
                    (link.rel.equals("preview", ignoreCase = true) ||
                        link.render.equals("image", ignoreCase = true))
            }
            ?.href
            ?.trim()

        val rawDesc = data.description?.takeIf { it.isNotBlank() }
            ?: data.description508?.takeIf { it.isNotBlank() }
            ?: ""
        val cleanDesc = SpaceNewsFormatters.cleanHtml(rawDesc)
        val credit = data.photographer?.takeIf { it.isNotBlank() }
            ?: data.secondaryCreator?.takeIf { it.isNotBlank() }
            ?: data.center?.takeIf { it.isNotBlank() }?.let { "NASA • $it" }

        return GlobalSearchItem(
            id = "nasamedia_$nasaId",
            source = GlobalSearchSource.NASA_MEDIA,
            contentType = contentType,
            title = title,
            description = cleanDesc,
            date = SpaceNewsFormatters.normalizeIsoDate(data.dateCreated),
            imageUrl = if (contentType == GlobalContentType.PHOTO) previewLink else null,
            thumbnailUrl = previewLink,
            mediaType = if (contentType == GlobalContentType.PHOTO) "image" else "video",
            sourceUrl = "https://images.nasa.gov/details/$nasaId",
            authorOrCredit = credit,
            isTranslated = false
        )
    }

    private suspend fun fetchApodMatches(query: String, englishTerm: String): List<GlobalSearchItem> {
        return apodSearchProvider(query, englishTerm)
            .take(APOD_MAX_ITEMS)
            .map { apod ->
                val isVideo = apod.mediaType == "video" ||
                    apod.mediaType == "iframe" ||
                    apod.mediaType == "embed"
                GlobalSearchItem(
                    id = "apod_${apod.date}",
                    source = GlobalSearchSource.APOD,
                    contentType = if (isVideo) GlobalContentType.VIDEO else GlobalContentType.PHOTO,
                    title = apod.displayTitle,
                    description = apod.displayExplanation,
                    date = apod.date,
                    imageUrl = if (isVideo) null else apod.displayImageUrl.takeIf { it.isNotBlank() },
                    thumbnailUrl = apod.displayThumbnailUrl,
                    mediaType = apod.mediaType,
                    sourceUrl = apod.officialNasaUrl,
                    authorOrCredit = apod.author ?: "NASA APOD",
                    translatedTitle = apod.translatedTitle,
                    translatedDescription = apod.translatedExplanation,
                    isTranslated = apod.isTranslated,
                    apod = apod
                )
            }
    }

    private suspend fun fetchDonkiMatches(
        normalizedPt: String,
        normalizedEn: String,
        forceRefresh: Boolean = false
    ): List<GlobalSearchItem> {
        val pool = getCachedDonkiItems(forceRefresh)
        if (pool.isEmpty()) return emptyList()

        val tokens = extractSearchTokens(normalizedPt, normalizedEn)
        val scored = pool.mapNotNull { item ->
            val haystack = normalizeForMatching("${item.title} ${item.description} ${item.authorOrCredit.orEmpty()}")
            val score = tokens.count { haystack.contains(it) }
            if (score > 0) Pair(score, item) else null
        }.sortedByDescending { it.first }

        return if (scored.isNotEmpty()) {
            scored.take(MAX_SUPPLEMENTAL_ITEMS).map { it.second }
        } else {
            // Consulta genérica de clima espacial (ex.: "clima espacial", "explosões solares")
            pool.take(MAX_SUPPLEMENTAL_ITEMS)
        }
    }

    private suspend fun getCachedDonkiItems(forceRefresh: Boolean = false): List<GlobalSearchItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            donkiCacheMutex.withLock {
                val cached = donkiCache
                if (cached != null && now - cached.cachedAtMillis <= SUPPLEMENTAL_CACHE_TTL_MS) {
                    return cached.items
                }
            }
        }

        val today = LocalDate.now()
        val endDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val startDate = today.minusDays(DONKI_LOOKBACK_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val mapped = mutableListOf<GlobalSearchItem>()
        donkiApiService.getNotifications(startDate = startDate, endDate = endDate)
            .mapNotNull { SpaceNewsFormatters.mapDonkiNotification(it) }
            .mapTo(mapped) { newsItem ->
                GlobalSearchItem(
                    id = newsItem.id,
                    source = GlobalSearchSource.DONKI,
                    contentType = GlobalContentType.SPACE_WEATHER,
                    title = newsItem.title,
                    description = newsItem.fullDescription,
                    date = newsItem.date,
                    imageUrl = null,
                    thumbnailUrl = null,
                    mediaType = "article",
                    sourceUrl = newsItem.sourceUrl,
                    authorOrCredit = newsItem.authorOrCredit,
                    translatedTitle = newsItem.title,
                    isTranslated = newsItem.fullDescription.isBlank()
                )
            }

        if (mapped.isEmpty()) {
            runCatchingApi {
                donkiApiService.getSolarFlares(startDate, endDate)
                    .mapNotNull { SpaceNewsFormatters.mapDonkiFlare(it) }
            }.getOrNull()?.mapTo(mapped) { item ->
                GlobalSearchItem(
                    id = item.id,
                    source = GlobalSearchSource.DONKI,
                    contentType = GlobalContentType.SPACE_WEATHER,
                    title = item.title,
                    description = item.fullDescription,
                    date = item.date,
                    imageUrl = null,
                    thumbnailUrl = null,
                    mediaType = "article",
                    sourceUrl = item.sourceUrl,
                    authorOrCredit = item.authorOrCredit,
                    translatedTitle = item.title,
                    isTranslated = item.isTranslated
                )
            }
            runCatchingApi {
                donkiApiService.getGeomagneticStorms(startDate, endDate)
                    .mapNotNull { SpaceNewsFormatters.mapDonkiGst(it) }
            }.getOrNull()?.mapTo(mapped) { item ->
                GlobalSearchItem(
                    id = item.id,
                    source = GlobalSearchSource.DONKI,
                    contentType = GlobalContentType.SPACE_WEATHER,
                    title = item.title,
                    description = item.fullDescription,
                    date = item.date,
                    imageUrl = null,
                    thumbnailUrl = null,
                    mediaType = "article",
                    sourceUrl = item.sourceUrl,
                    authorOrCredit = item.authorOrCredit,
                    translatedTitle = item.title,
                    isTranslated = true
                )
            }
            runCatchingApi {
                donkiApiService.getCmeEvents(startDate, endDate)
                    .mapNotNull { SpaceNewsFormatters.mapDonkiCme(it) }
            }.getOrNull()?.mapTo(mapped) { item ->
                GlobalSearchItem(
                    id = item.id,
                    source = GlobalSearchSource.DONKI,
                    contentType = GlobalContentType.SPACE_WEATHER,
                    title = item.title,
                    description = item.fullDescription,
                    date = item.date,
                    imageUrl = null,
                    thumbnailUrl = null,
                    mediaType = "article",
                    sourceUrl = item.sourceUrl,
                    authorOrCredit = item.authorOrCredit,
                    translatedTitle = item.title,
                    isTranslated = item.isTranslated
                )
            }
        }

        val finalized = mapped.distinctBy { it.id }.sortedByDescending { it.date }
        donkiCacheMutex.withLock {
            donkiCache = TimedItems(finalized, System.currentTimeMillis())
        }
        return finalized
    }

    private suspend fun fetchEonetMatches(
        normalizedPt: String,
        normalizedEn: String,
        forceRefresh: Boolean = false
    ): List<GlobalSearchItem> {
        val pool = getCachedEonetItems(forceRefresh)
        if (pool.isEmpty()) return emptyList()

        val tokens = extractSearchTokens(normalizedPt, normalizedEn)
        val scored = pool.mapNotNull { item ->
            val haystack = normalizeForMatching("${item.title} ${item.description} ${item.authorOrCredit.orEmpty()}")
            val score = tokens.count { haystack.contains(it) }
            if (score > 0) Pair(score, item) else null
        }.sortedByDescending { it.first }

        return if (scored.isNotEmpty()) {
            scored.take(MAX_SUPPLEMENTAL_ITEMS).map { it.second }
        } else if (normalizedPt.contains("evento natural") || normalizedEn.contains("natural event")) {
            pool.take(MAX_SUPPLEMENTAL_ITEMS)
        } else {
            emptyList()
        }
    }

    private suspend fun getCachedEonetItems(forceRefresh: Boolean = false): List<GlobalSearchItem> {
        val now = System.currentTimeMillis()
        if (!forceRefresh) {
            eonetCacheMutex.withLock {
                val cached = eonetCache
                if (cached != null && now - cached.cachedAtMillis <= SUPPLEMENTAL_CACHE_TTL_MS) {
                    return cached.items
                }
            }
        }

        val response = eonetApiService.getEvents()
        val mapped = response.events.orEmpty()
            .mapNotNull(::mapEonetSearchItem)
            .distinctBy { it.id }
            .sortedByDescending { it.date }

        eonetCacheMutex.withLock {
            eonetCache = TimedItems(mapped, System.currentTimeMillis())
        }
        return mapped
    }

    private fun mapEonetSearchItem(dto: EonetEventDto): GlobalSearchItem? {
        val id = dto.id?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val title = dto.title?.trim().takeUnless { it.isNullOrBlank() } ?: return null
        val sourceUrl = dto.sources.orEmpty()
            .firstOrNull { !it.url.isNullOrBlank() }
            ?.url?.trim()
            ?: dto.link?.trim()
            ?: return null

        val firstCategory = dto.categories.orEmpty().firstOrNull()
        val categoryLabelPt = eonetCategoryLabelPt(firstCategory?.id, firstCategory?.title)
        val latestGeom = dto.geometry.orEmpty().lastOrNull()
        val date = SpaceNewsFormatters.normalizeIsoDate(latestGeom?.date)

        val apiDesc = SpaceNewsFormatters.cleanHtml(dto.description.orEmpty())
        val description = apiDesc.ifBlank {
            buildList {
                if (categoryLabelPt.isNotBlank()) {
                    add("Evento natural de $categoryLabelPt monitorado por satélites.")
                }
                val mag = latestGeom?.magnitudeValue
                val unit = latestGeom?.magnitudeUnit?.trim()
                if (mag != null) {
                    add("Magnitude observada: ${if (!unit.isNullOrBlank()) "$mag $unit" else "$mag"}.")
                }
            }.joinToString(" ")
        }

        return GlobalSearchItem(
            id = "eonet_$id",
            source = GlobalSearchSource.EONET,
            contentType = GlobalContentType.NATURAL_EVENT,
            title = title,
            description = description,
            date = date,
            imageUrl = null,
            thumbnailUrl = null,
            mediaType = "article",
            sourceUrl = sourceUrl,
            authorOrCredit = if (categoryLabelPt.isNotBlank()) "NASA EONET • $categoryLabelPt" else "NASA EONET",
            isTranslated = false
        )
    }

    private fun mergeAndDeduplicate(
        donkiItems: List<GlobalSearchItem>,
        eonetItems: List<GlobalSearchItem>,
        apodItems: List<GlobalSearchItem>,
        mediaItems: List<GlobalSearchItem>
    ): List<GlobalSearchItem> {
        val merged = mutableListOf<GlobalSearchItem>()
        val seenIds = HashSet<String>()
        val seenSourceTitle = HashSet<String>()

        fun append(items: List<GlobalSearchItem>) {
            for (item in items) {
                val titleKey = "${item.source.name}:${normalizeForMatching(item.title)}"
                if (item.id in seenIds || titleKey in seenSourceTitle) continue
                seenIds.add(item.id)
                seenSourceTitle.add(titleKey)
                merged.add(item)
            }
        }

        // Prioriza relevância equilibrada: eventos específicos (quando aplicável), APODs e acervo principal
        append(donkiItems.take(3))
        append(eonetItems.take(3))
        append(apodItems.take(3))
        append(mediaItems)
        append(donkiItems.drop(3))
        append(eonetItems.drop(3))
        append(apodItems.drop(3))

        return merged.sortedByDescending { it.date }
    }

    private suspend inline fun <T> runCatchingApi(block: () -> T): Result<T> {
        return try {
            Result.success(block())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isSpaceWeatherQuery(pt: String, en: String): Boolean =
        SPACE_WEATHER_KEYWORDS.any { pt.contains(it) || en.contains(it) }

    private fun isNaturalEventQuery(pt: String, en: String): Boolean =
        NATURAL_EVENT_KEYWORDS.any { pt.contains(it) || en.contains(it) }

    private fun extractSearchTokens(pt: String, en: String): List<String> {
        val raw = (pt.split(" ") + en.split(" "))
            .map { it.trim() }
            .filter { it.length >= 3 && it !in STOPWORDS }
            .distinct()
        return raw.ifEmpty { listOf(en, pt).filter { it.isNotBlank() } }
    }

    private fun normalizeForMatching(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase().trim(), Normalizer.Form.NFD)
        return decomposed.replace(ACCENTS_REGEX, "").replace(MULTI_SPACE_REGEX, " ")
    }

    private fun isDirectVideoUrl(url: String): Boolean {
        val lower = url.lowercase().substringBefore('?')
        return lower.endsWith(".mp4") || lower.endsWith(".mov") ||
            lower.endsWith(".webm") || lower.endsWith(".m3u8") ||
            lower.contains("video.media.nasa.gov")
    }

    private fun eonetCategoryLabelPt(id: String?, fallback: String?): String = when (id?.lowercase()) {
        "wildfires" -> "Incêndios Florestais"
        "severestorms" -> "Tempestades Severas"
        "volcanoes" -> "Erupções Vulcânicas"
        "sealakeice" -> "Gelo Marinho"
        "earthquakes" -> "Terremotos"
        "floods" -> "Inundações"
        "landslides" -> "Deslizamentos"
        "drought" -> "Secas"
        "dusthaze" -> "Poeira e Neblina"
        else -> fallback?.trim().orEmpty()
    }

    companion object {
        const val MEDIA_PAGE_SIZE = 20
        private const val APOD_MAX_ITEMS = 4
        private const val MAX_SUPPLEMENTAL_ITEMS = 5
        private const val DONKI_LOOKBACK_DAYS = 45L
        private const val MAX_QUERY_CACHE_ENTRIES = 25
        private const val QUERY_CACHE_TTL_MS = 10 * 60 * 1000L
        private const val SUPPLEMENTAL_CACHE_TTL_MS = 15 * 60 * 1000L

        private val ACCENTS_REGEX = Regex("""\p{InCombiningDiacriticalMarks}+""")
        private val MULTI_SPACE_REGEX = Regex("""\s+""")
        private val STOPWORDS = setOf("the", "and", "for", "with", "from", "das", "dos", "para", "com", "sobre", "fora")

        private val SPACE_WEATHER_KEYWORDS = listOf(
            "sol", "solar", "sun", "flare", "cme", "coronal", "geomagn", "aurora",
            "clima espacial", "space weather", "particula", "particle", "radiac", "radiation",
            "explos", "tempestade solar", "vento solar"
        )

        private val NATURAL_EVENT_KEYWORDS = listOf(
            "incendio", "queimada", "wildfire", "fire", "vulca", "volcan",
            "tempestade", "storm", "furac", "hurricane", "ciclone", "cyclone",
            "terremoto", "earthquake", "inunda", "flood", "gelo", "ice",
            "seca", "drought", "evento natural", "natural event", "desastre"
        )

        private val GLOSSARY_PT_TO_EN = mapOf(
            "buraco negro" to "black hole",
            "buracos negros" to "black hole",
            "via lactea" to "milky way",
            "aurora boreal" to "aurora",
            "explosao solar" to "solar flare",
            "explosoes solares" to "solar flare",
            "erupcao solar" to "solar flare",
            "erupcoes solares" to "solar flare",
            "tempestade solar" to "geomagnetic storm",
            "tempestade geomagnetica" to "geomagnetic storm",
            "ejecao de massa coronal" to "coronal mass ejection",
            "clima espacial" to "space weather",
            "nebulosa de orion" to "orion nebula",
            "incendio florestal" to "wildfire",
            "incendios florestais" to "wildfire",
            "queimada" to "wildfire",
            "vulcao" to "volcano",
            "erupcao vulcanica" to "volcano",
            "tempestade" to "storm",
            "furacao" to "hurricane",
            "vida fora da terra" to "astrobiology exoplanet",
            "missoes da nasa" to "nasa mission",
            "marte" to "mars",
            "saturno" to "saturn",
            "jupiter" to "jupiter",
            "lua" to "moon",
            "asteroides" to "asteroid",
            "asteroide" to "asteroid",
            "estrelas" to "stars",
            "estrela" to "star",
            "galaxia" to "galaxy",
            "galaxias" to "galaxy",
            "nebulosa" to "nebula",
            "nebulosas" to "nebula"
        )
    }
}
