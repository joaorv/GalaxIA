package com.example.galaxia.repository

import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.CachedCategoryData
import com.example.galaxia.data.local.FileSpaceNewsCache
import com.example.galaxia.data.local.SpaceNewsCache
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.NewsSortOrder
import com.example.galaxia.data.model.NewsSubTopic
import com.example.galaxia.data.model.SpaceNewsItem
import com.example.galaxia.data.model.subTopics
import com.example.galaxia.data.remote.DonkiApiService
import com.example.galaxia.data.remote.NasaImagesApiService
import com.example.galaxia.data.remote.NasaScienceNewsApiService
import com.example.galaxia.data.remote.RetrofitClient
import com.example.galaxia.data.remote.SpaceNewsFormatters
import com.example.galaxia.data.translation.AppTranslation
import com.example.galaxia.data.translation.TranslationService
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.ConcurrentHashMap

data class NewsPageResult(
    val items: List<SpaceNewsItem>,
    val page: Int,
    val hasMore: Boolean,
    val isFromOfflineCache: Boolean = false
)

/**
 * Repositório da seção Notícias e Curiosidades Espaciais.
 * Carrega apenas a categoria e sub-tópico ativos sob demanda, gerencia cache
 * com fallback offline e reutiliza o serviço de tradução compartilhado do aplicativo.
 */
class SpaceNewsRepository(
    private val nasaScienceNewsApiService: NasaScienceNewsApiService = RetrofitClient.nasaScienceNewsApiService,
    private val donkiApiService: DonkiApiService = RetrofitClient.donkiApiService,
    private val nasaImagesApiService: NasaImagesApiService = RetrofitClient.nasaImagesApiService,
    private val translationService: TranslationService = AppTranslation.service,
    private val cache: SpaceNewsCache = FileSpaceNewsCache(GalaxiaApplication.instance)
) : BaseRepository {

    private data class ImagePagingState(
        val windowIndex: Int,
        val nextApiPage: Int,
        val exhausted: Boolean = false
    )

    private val translationCache = ConcurrentHashMap<String, Triple<String, String, String>>()
    private val imagePagingStates = ConcurrentHashMap<String, ImagePagingState>()

    private fun restorePagingStateIfApplicable(
        category: NewsCategory,
        cacheKey: String,
        cached: CachedCategoryData
    ) {
        if (category == NewsCategory.MISSIONS_DISCOVERIES ||
            category == NewsCategory.UNIVERSE_CURIOSITIES
        ) {
            imagePagingStates[cacheKey] = ImagePagingState(
                windowIndex = cached.pagingWindowIndex,
                nextApiPage = cached.pagingNextApiPage,
                exhausted = cached.pagingExhausted
            )
        }
    }

    private fun buildCachedData(
        cacheKey: String,
        items: List<SpaceNewsItem>,
        pageLoaded: Int,
        hasMore: Boolean
    ): CachedCategoryData {
        val state = imagePagingStates[cacheKey]
        return CachedCategoryData(
            items = items,
            cachedAtMillis = System.currentTimeMillis(),
            pageLoaded = pageLoaded,
            hasMore = hasMore,
            pagingWindowIndex = state?.windowIndex ?: 0,
            pagingNextApiPage = state?.nextApiPage ?: 1,
            pagingExhausted = state?.exhausted ?: !hasMore
        )
    }

    fun buildCacheKey(
        category: NewsCategory,
        subTopic: NewsSubTopic?,
        sortOrder: NewsSortOrder = NewsSortOrder.NEWEST
    ): String {
        val effectiveSub = subTopic ?: category.subTopics.firstOrNull()
        return if (effectiveSub != null) {
            "${category.name}:${effectiveSub.id}:${sortOrder.name}"
        } else {
            category.name
        }
    }

    suspend fun fetchCategoryPage(
        category: NewsCategory,
        subTopic: NewsSubTopic? = null,
        sortOrder: NewsSortOrder = NewsSortOrder.NEWEST,
        page: Int = 1,
        forceRefresh: Boolean = false,
        allowStaleFallback: Boolean = true
    ): NewsPageResult {
        val cacheKey = buildCacheKey(category, subTopic, sortOrder)
        if (page == 1 && !forceRefresh) {
            cache.getValid(cacheKey)?.let { cached ->
                restorePagingStateIfApplicable(category, cacheKey, cached)
                return NewsPageResult(
                    items = cached.items,
                    page = cached.pageLoaded,
                    hasMore = cached.hasMore,
                    isFromOfflineCache = false
                )
            }
        }

        return try {
            val result = when (category) {
                NewsCategory.NASA_HIGHLIGHTS -> fetchNasaHighlights(page)
                NewsCategory.MISSIONS_DISCOVERIES,
                NewsCategory.UNIVERSE_CURIOSITIES -> {
                    val query = (subTopic ?: category.subTopics.first()).query
                    fetchNasaImages(
                        category = category,
                        query = query,
                        page = page,
                        sortOrder = sortOrder,
                        cacheKey = cacheKey
                    )
                }
                NewsCategory.SPACE_WEATHER -> fetchSpaceWeather()
            }

            if (page == 1 && result.items.isNotEmpty()) {
                cache.put(
                    cacheKey,
                    buildCachedData(
                        cacheKey = cacheKey,
                        items = result.items,
                        pageLoaded = result.page,
                        hasMore = result.hasMore
                    )
                )
            }
            result
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            if (page == 1 && allowStaleFallback) {
                cache.getStale(cacheKey)?.let { stale ->
                    restorePagingStateIfApplicable(category, cacheKey, stale)
                    return NewsPageResult(
                        items = stale.items,
                        page = stale.pageLoaded,
                        hasMore = stale.hasMore,
                        isFromOfflineCache = true
                    )
                }
            }
            throw e
        }
    }

    suspend fun updateCachedItems(
        cacheKey: String,
        items: List<SpaceNewsItem>,
        pageLoaded: Int,
        hasMore: Boolean
    ) {
        if (items.isEmpty()) return
        cache.put(
            cacheKey,
            buildCachedData(
                cacheKey = cacheKey,
                items = items,
                pageLoaded = pageLoaded,
                hasMore = hasMore
            )
        )
    }

    suspend fun translateItem(item: SpaceNewsItem): SpaceNewsItem {
        if (item.isTranslated &&
            (!item.translatedFullDescription.isNullOrBlank() || item.fullDescription.isBlank())
        ) {
            return item
        }

        translationCache[item.id]?.let { (titlePt, summaryPt, descPt) ->
            return item.copy(
                translatedTitle = titlePt,
                translatedSummary = summaryPt,
                translatedFullDescription = descPt,
                isTranslated = true
            )
        }

        return try {
            val titlePt = if (item.category == NewsCategory.SPACE_WEATHER) {
                // Títulos do DONKI já são mapeados em português
                item.title
            } else {
                item.translatedTitle?.takeIf { it.isNotBlank() }
                    ?: translationService.translateText(item.title)
            }

            val fullDescPt = if (item.fullDescription.isNotBlank()) {
                translationService.translateText(item.fullDescription)
            } else {
                ""
            }

            val summaryPt = when {
                item.summary.isBlank() -> ""
                item.summary == item.fullDescription -> fullDescPt
                else -> translationService.translateText(item.summary)
            }

            translationCache[item.id] = Triple(titlePt, summaryPt, fullDescPt)

            item.copy(
                translatedTitle = titlePt,
                translatedSummary = summaryPt,
                translatedFullDescription = fullDescPt,
                isTranslated = true
            )
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            item
        }
    }

    suspend fun translateTitleOnly(item: SpaceNewsItem): SpaceNewsItem {
        if (item.isTranslated && !item.translatedTitle.isNullOrBlank()) return item
        if (item.category == NewsCategory.SPACE_WEATHER) {
            return item.copy(translatedTitle = item.title, isTranslated = true)
        }
        return try {
            val titlePt = translationService.translateText(item.title)
            item.copy(translatedTitle = titlePt, isTranslated = true)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            item
        }
    }

    private suspend fun fetchNasaHighlights(page: Int): NewsPageResult {
        val response = nasaScienceNewsApiService.getPosts(
            perPage = HIGHLIGHTS_PAGE_SIZE,
            page = page,
            embed = 1
        )
        if (!response.isSuccessful) {
            throw HttpException(response)
        }
        val posts = response.body().orEmpty()
        val items = SpaceNewsFormatters.groupDuplicateTitleItems(
            posts.mapNotNull { SpaceNewsFormatters.mapNasaSciencePost(it) }
                .distinctBy { it.id }
                .sortedByDescending { it.date }
        ).sortedByDescending { it.date }

        val totalPages = response.headers()["X-WP-TotalPages"]?.toIntOrNull() ?: page
        val hasMore = items.isNotEmpty() && page < totalPages

        return NewsPageResult(
            items = items,
            page = page,
            hasMore = hasMore
        )
    }

    private suspend fun fetchSpaceWeather(): NewsPageResult {
        val today = LocalDate.now()
        val endDate = today.format(DateTimeFormatter.ISO_LOCAL_DATE)
        val startDate = today.minusDays(DONKI_LOOKBACK_DAYS).format(DateTimeFormatter.ISO_LOCAL_DATE)

        val notifications = donkiApiService.getNotifications(
            startDate = startDate,
            endDate = endDate
        )
        val fromNotifications = notifications
            .mapNotNull { SpaceNewsFormatters.mapDonkiNotification(it) }
            .distinctBy { it.id }
            .sortedByDescending { it.date }
            .take(DONKI_MAX_ITEMS)

        if (fromNotifications.isNotEmpty()) {
            return NewsPageResult(items = fromNotifications, page = 1, hasMore = false)
        }

        // Fallback sequencial para endpoints específicos caso /notifications retorne vazio
        val fallbackItems = mutableListOf<SpaceNewsItem>()
        try {
            donkiApiService.getSolarFlares(startDate, endDate)
                .mapNotNullTo(fallbackItems) { SpaceNewsFormatters.mapDonkiFlare(it) }
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {}

        if (fallbackItems.size < 6) {
            try {
                donkiApiService.getGeomagneticStorms(startDate, endDate)
                    .mapNotNullTo(fallbackItems) { SpaceNewsFormatters.mapDonkiGst(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        if (fallbackItems.size < 6) {
            try {
                donkiApiService.getCmeEvents(startDate, endDate)
                    .mapNotNullTo(fallbackItems) { SpaceNewsFormatters.mapDonkiCme(it) }
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {}
        }

        val sorted = fallbackItems
            .distinctBy { it.id }
            .sortedByDescending { it.date }
            .take(DONKI_MAX_ITEMS)

        return NewsPageResult(items = sorted, page = 1, hasMore = false)
    }

    private fun buildYearWindows(
        currentYear: Int,
        sortOrder: NewsSortOrder
    ): List<Pair<Int?, Int?>> {
        val newestFirst = buildList {
            add(currentYear to null)
            add((currentYear - 1) to (currentYear - 1))
            add((currentYear - 2) to (currentYear - 2))
            add((currentYear - 3) to (currentYear - 3))
            add((currentYear - 4) to (currentYear - 4))
            add((currentYear - 6) to (currentYear - 5))
            add((currentYear - 9) to (currentYear - 7))
            add((currentYear - 14) to (currentYear - 10))
            add((currentYear - 21) to (currentYear - 15))
            if (1990 <= currentYear - 22) {
                add(1990 to (currentYear - 22))
            }
            add(null to 1989)
        }
        return when (sortOrder) {
            NewsSortOrder.NEWEST -> newestFirst
            NewsSortOrder.OLDEST -> newestFirst.reversed()
        }
    }

    private suspend fun fetchNasaImages(
        category: NewsCategory,
        query: String,
        page: Int,
        sortOrder: NewsSortOrder,
        cacheKey: String
    ): NewsPageResult {
        val windows = buildYearWindows(LocalDate.now().year, sortOrder)
        val initialState = if (page <= 1) {
            ImagePagingState(windowIndex = 0, nextApiPage = 1)
        } else {
            imagePagingStates[cacheKey] ?: ImagePagingState(windowIndex = 0, nextApiPage = page)
        }

        if (initialState.exhausted || initialState.windowIndex >= windows.size) {
            return NewsPageResult(items = emptyList(), page = page, hasMore = false)
        }

        var windowIndex = initialState.windowIndex
        var apiPage = initialState.nextApiPage
        var requestsMade = 0
        val collectedItems = mutableListOf<SpaceNewsItem>()

        while (windowIndex < windows.size) {
            val window = windows[windowIndex]
            val response = nasaImagesApiService.searchImages(
                query = query,
                mediaType = "image",
                page = apiPage,
                pageSize = IMAGE_PAGE_SIZE,
                yearStart = window.first,
                yearEnd = window.second
            )
            requestsMade++

            val collection = response.collection
            val mapped = collection?.items.orEmpty()
                .mapNotNull { SpaceNewsFormatters.mapNasaImageItem(it, category) }
            collectedItems.addAll(mapped)

            val totalHits = collection?.metadata?.totalHits ?: 0
            val windowHasMore = mapped.isNotEmpty() &&
                (apiPage * IMAGE_PAGE_SIZE < totalHits) &&
                (apiPage < MAX_IMAGE_API_PAGES)

            if (windowHasMore) {
                apiPage += 1
            } else {
                windowIndex += 1
                apiPage = 1
            }

            val distinctCount = collectedItems.distinctBy { it.id }.size
            if (distinctCount >= MIN_PAGE_RESULTS) break
            if (distinctCount > 0 && requestsMade >= MAX_WINDOW_HOPS_PER_PAGE) break
        }

        val exhausted = windowIndex >= windows.size
        imagePagingStates[cacheKey] = ImagePagingState(
            windowIndex = windowIndex,
            nextApiPage = apiPage,
            exhausted = exhausted
        )

        val uniqueItems = collectedItems.distinctBy { it.id }
        val sorted = when (sortOrder) {
            NewsSortOrder.NEWEST -> uniqueItems.sortedByDescending { it.date }
            NewsSortOrder.OLDEST -> uniqueItems.sortedBy { it.date.ifBlank { "9999-99-99" } }
        }
        val grouped = SpaceNewsFormatters.groupDuplicateTitleItems(sorted)
        val finalOrdered = when (sortOrder) {
            NewsSortOrder.NEWEST -> grouped.sortedByDescending { it.date }
            NewsSortOrder.OLDEST -> grouped.sortedBy { it.date.ifBlank { "9999-99-99" } }
        }

        return NewsPageResult(
            items = finalOrdered,
            page = page,
            hasMore = !exhausted
        )
    }

    companion object {
        const val HIGHLIGHTS_PAGE_SIZE = 10
        const val IMAGE_PAGE_SIZE = 20
        private const val MIN_PAGE_RESULTS = 8
        private const val MAX_WINDOW_HOPS_PER_PAGE = 4
        private const val MAX_IMAGE_API_PAGES = 100
        private const val DONKI_LOOKBACK_DAYS = 30L
        private const val DONKI_MAX_ITEMS = 15
    }
}
