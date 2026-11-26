package com.example.galaxia

import com.example.galaxia.data.local.CachedCategoryData
import com.example.galaxia.data.local.SpaceNewsCache
import com.example.galaxia.data.model.DonkiCmeDto
import com.example.galaxia.data.model.DonkiFlareDto
import com.example.galaxia.data.model.DonkiGstDto
import com.example.galaxia.data.model.DonkiNotificationDto
import com.example.galaxia.data.model.NasaAssetResponse
import com.example.galaxia.data.model.NasaImageDataDto
import com.example.galaxia.data.model.NasaImageItemDto
import com.example.galaxia.data.model.NasaImageLinkDto
import com.example.galaxia.data.model.NasaImagesCollection
import com.example.galaxia.data.model.NasaImagesMetadata
import com.example.galaxia.data.model.NasaImagesResponse
import com.example.galaxia.data.model.NasaSciencePostDto
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.NewsSortOrder
import com.example.galaxia.data.model.SpaceNewsItem
import com.example.galaxia.data.model.WpRenderedText
import com.example.galaxia.data.model.subTopics
import com.example.galaxia.data.remote.DonkiApiService
import com.example.galaxia.data.remote.NasaImagesApiService
import com.example.galaxia.data.remote.NasaScienceNewsApiService
import com.example.galaxia.data.translation.TranslationService
import com.example.galaxia.repository.SpaceNewsRepository
import kotlinx.coroutines.runBlocking
import okhttp3.Headers.Companion.headersOf
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class SpaceNewsRepositoryTest {

    private class MemoryNewsCache : SpaceNewsCache {
        val validMap = mutableMapOf<String, CachedCategoryData>()
        val staleMap = mutableMapOf<String, CachedCategoryData>()

        override suspend fun getValid(cacheKey: String, nowMillis: Long): CachedCategoryData? =
            validMap[cacheKey]

        override suspend fun getStale(cacheKey: String): CachedCategoryData? =
            staleMap[cacheKey] ?: validMap[cacheKey]

        override suspend fun put(cacheKey: String, data: CachedCategoryData) {
            validMap[cacheKey] = data
            staleMap[cacheKey] = data
        }
    }

    private class FakeScienceNewsService(private val fail: Boolean = false) : NasaScienceNewsApiService {
        val calls = AtomicInteger()

        override suspend fun getPosts(perPage: Int, page: Int, embed: Int): Response<List<NasaSciencePostDto>> {
            calls.incrementAndGet()
            if (fail) throw IOException("Network unavailable")
            val posts = listOf(
                NasaSciencePostDto(
                    id = 100L,
                    date = "2026-11-25T10:00:00",
                    link = "https://science.nasa.gov/post/100",
                    title = WpRenderedText("New Exoplanet Found"),
                    excerpt = WpRenderedText("Summary"),
                    content = WpRenderedText("Full details")
                )
            )
            return Response.success(posts, headersOf("X-WP-TotalPages", "3"))
        }
    }

    private class FakeDonkiService : DonkiApiService {
        override suspend fun getNotifications(startDate: String, endDate: String, type: String, apiKey: String) =
            emptyList<DonkiNotificationDto>()

        override suspend fun getSolarFlares(startDate: String, endDate: String, apiKey: String) =
            emptyList<DonkiFlareDto>()

        override suspend fun getCmeEvents(startDate: String, endDate: String, apiKey: String) =
            emptyList<DonkiCmeDto>()

        override suspend fun getGeomagneticStorms(startDate: String, endDate: String, apiKey: String) =
            emptyList<DonkiGstDto>()
    }

    private class FakeImagesService(
        private val returnEmptyWhenYearEndSet: Boolean = false
    ) : NasaImagesApiService {
        val calls = AtomicInteger()
        val recordedParams = mutableListOf<Triple<String, Int?, Int?>>()

        override suspend fun searchImages(
            query: String,
            mediaType: String,
            page: Int,
            pageSize: Int,
            yearStart: Int?,
            yearEnd: Int?
        ): NasaImagesResponse {
            calls.incrementAndGet()
            recordedParams.add(Triple(query, yearStart, yearEnd))

            if (returnEmptyWhenYearEndSet && yearEnd != null) {
                return NasaImagesResponse(
                    collection = NasaImagesCollection(
                        items = emptyList(),
                        metadata = NasaImagesMetadata(totalHits = 0)
                    )
                )
            }

            val items = listOf(
                imageDto("ID_OLD", "Older Image", "2008-03-15T00:00:00Z"),
                imageDto("ID_NEW", "Newest Image", "2026-10-01T00:00:00Z"),
                imageDto("ID_MID", "Mid Image", "2023-06-10T00:00:00Z"),
                imageDto("ID_RECENT", "Recent Image", "2025-01-20T00:00:00Z")
            )
            return NasaImagesResponse(
                collection = NasaImagesCollection(
                    items = items,
                    metadata = NasaImagesMetadata(totalHits = items.size)
                )
            )
        }

        override suspend fun getAssetManifest(nasaId: String) = NasaAssetResponse()

        private fun imageDto(id: String, title: String, date: String) = NasaImageItemDto(
            data = listOf(
                NasaImageDataDto(
                    nasaId = id,
                    title = title,
                    description = "Desc $id",
                    dateCreated = date,
                    mediaType = "image"
                )
            ),
            links = listOf(
                NasaImageLinkDto(
                    href = "https://images-assets.nasa.gov/image/$id/preview.jpg",
                    rel = "preview",
                    render = "image"
                )
            )
        )
    }

    private class FakeTranslationService : TranslationService {
        override suspend fun translateText(text: String, sourceLang: String, targetLang: String): String =
            "PT[$text]"
    }

    @Test
    fun fetchCategoryPage_newestFiltersByYearStartAndSortsDescending() = runBlocking {
        val images = FakeImagesService()
        val cache = MemoryNewsCache()
        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = images,
            translationService = FakeTranslationService(),
            cache = cache
        )

        val saturn = NewsCategory.UNIVERSE_CURIOSITIES.subTopics.first { it.id == "saturn" }
        val result = repo.fetchCategoryPage(
            category = NewsCategory.UNIVERSE_CURIOSITIES,
            subTopic = saturn,
            sortOrder = NewsSortOrder.NEWEST
        )

        assertEquals("saturn", images.recordedParams.first().first)
        assertNotNull(images.recordedParams.first().second)
        assertNull(images.recordedParams.first().third)
        assertEquals(listOf("2026-10-01", "2025-01-20", "2023-06-10", "2008-03-15"), result.items.map { it.date })
    }

    @Test
    fun fetchCategoryPage_oldestUsesYearEndFallsBackWhenEmptyAndSortsAscending() = runBlocking {
        val images = FakeImagesService(returnEmptyWhenYearEndSet = true)
        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = images,
            translationService = FakeTranslationService(),
            cache = MemoryNewsCache()
        )

        val webb = NewsCategory.MISSIONS_DISCOVERIES.subTopics.first { it.id == "jwst" }
        val result = repo.fetchCategoryPage(
            category = NewsCategory.MISSIONS_DISCOVERIES,
            subTopic = webb,
            sortOrder = NewsSortOrder.OLDEST
        )

        assertTrue(images.calls.get() > 1)
        assertNotNull(images.recordedParams.first().third)
        assertNull(images.recordedParams.last().third)
        assertEquals(listOf("2008-03-15", "2023-06-10", "2025-01-20", "2026-10-01"), result.items.map { it.date })
    }

    @Test
    fun fetchCategoryPage_separatesNewestAndOldestInCache() = runBlocking {
        val images = FakeImagesService()
        val cache = MemoryNewsCache()
        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = images,
            translationService = FakeTranslationService(),
            cache = cache
        )

        val nebula = NewsCategory.UNIVERSE_CURIOSITIES.subTopics.first { it.id == "nebula" }
        repo.fetchCategoryPage(NewsCategory.UNIVERSE_CURIOSITIES, nebula, NewsSortOrder.NEWEST)
        val callsAfterNewest = images.calls.get()
        assertTrue(callsAfterNewest >= 1)

        repo.fetchCategoryPage(NewsCategory.UNIVERSE_CURIOSITIES, nebula, NewsSortOrder.NEWEST)
        assertEquals(callsAfterNewest, images.calls.get())

        val oldest = repo.fetchCategoryPage(NewsCategory.UNIVERSE_CURIOSITIES, nebula, NewsSortOrder.OLDEST)
        assertTrue(images.calls.get() > callsAfterNewest)
        assertEquals("2008-03-15", oldest.items.first().date)
        assertFalse(oldest.isFromOfflineCache)
    }

    @Test
    fun fetchCategoryPage_returnsStaleCacheOnNetworkError() = runBlocking {
        val cache = MemoryNewsCache()
        val cachedItem = SpaceNewsItem(
            id = "nasasci_cached",
            category = NewsCategory.NASA_HIGHLIGHTS,
            sourceName = "NASA Science",
            title = "Cached Highlight",
            summary = "Cached summary",
            fullDescription = "Cached description",
            date = "2026-11-20",
            sourceUrl = "https://science.nasa.gov/cached"
        )
        cache.staleMap[NewsCategory.NASA_HIGHLIGHTS.name] = CachedCategoryData(
            items = listOf(cachedItem),
            cachedAtMillis = 1L,
            pageLoaded = 1,
            hasMore = false
        )

        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(fail = true),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = FakeImagesService(),
            translationService = FakeTranslationService(),
            cache = cache
        )

        val result = repo.fetchCategoryPage(NewsCategory.NASA_HIGHLIGHTS)

        assertTrue(result.isFromOfflineCache)
        assertEquals(listOf(cachedItem), result.items)
    }

    @Test
    fun translateItem_populatesPortugueseFields() = runBlocking {
        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = FakeImagesService(),
            translationService = FakeTranslationService(),
            cache = MemoryNewsCache()
        )

        val item = SpaceNewsItem(
            id = "nasaimg_1",
            category = NewsCategory.MISSIONS_DISCOVERIES,
            sourceName = "NASA Image Library",
            title = "Perseverance Rover",
            summary = "Mars exploration",
            fullDescription = "Mars exploration full text",
            date = "2026-11-10",
            imageUrl = "https://images-assets.nasa.gov/image/1/preview.jpg",
            sourceUrl = "https://images.nasa.gov/details/1"
        )

        val translated = repo.translateItem(item)

        assertTrue(translated.isTranslated)
        assertEquals("PT[Perseverance Rover]", translated.displayTitle)
        assertEquals("PT[Mars exploration]", translated.displaySummary)
        assertEquals("PT[Mars exploration full text]", translated.displayFullDescription)
    }

    @Test
    fun fetchCategoryPage_newestTransitionsAcrossAllWindowsChronologically() = runBlocking {
        val currentYear = java.time.LocalDate.now().year
        val allCatalog = listOf(
            // 8 itens no ano atual para preencher a página 1 apenas com a primeira janela
            dto("R1", "Recent 1", "$currentYear-08-01T00:00:00Z"),
            dto("R2", "Recent 2", "$currentYear-07-01T00:00:00Z"),
            dto("R3", "Recent 3", "$currentYear-06-01T00:00:00Z"),
            dto("R4", "Recent 4", "$currentYear-05-01T00:00:00Z"),
            dto("R5", "Recent 5", "$currentYear-04-01T00:00:00Z"),
            dto("R6", "Recent 6", "$currentYear-03-01T00:00:00Z"),
            dto("R7", "Recent 7", "$currentYear-02-01T00:00:00Z"),
            dto("R8", "Recent 8", "$currentYear-01-01T00:00:00Z"),
            // Itens antigos após janelas intermediárias vazias (inclui <= 2021 e pré-1990)
            dto("A1", "Archive 2021", "2021-08-15T00:00:00Z"),
            dto("A2", "Archive 2020", "2020-07-10T00:00:00Z"),
            dto("A3", "Archive 1985", "1985-04-12T00:00:00Z")
        )

        val pagedImages = object : NasaImagesApiService {
            val recordedWindows = mutableListOf<Triple<Int, Int?, Int?>>()

            override suspend fun searchImages(
                query: String,
                mediaType: String,
                page: Int,
                pageSize: Int,
                yearStart: Int?,
                yearEnd: Int?
            ): NasaImagesResponse {
                recordedWindows.add(Triple(page, yearStart, yearEnd))
                val matching = allCatalog.filter { item ->
                    val year = item.data!!.first().dateCreated!!.substring(0, 4).toInt()
                    (yearStart == null || year >= yearStart) &&
                        (yearEnd == null || year <= yearEnd)
                }
                val slice = if (page == 1) matching.take(pageSize) else emptyList()
                return NasaImagesResponse(
                    collection = NasaImagesCollection(
                        items = slice,
                        metadata = NasaImagesMetadata(totalHits = matching.size)
                    )
                )
            }

            override suspend fun getAssetManifest(nasaId: String) = NasaAssetResponse()
        }

        val sharedCache = MemoryNewsCache()
        val repo = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = pagedImages,
            translationService = FakeTranslationService(),
            cache = sharedCache
        )
        val mars = NewsCategory.MISSIONS_DISCOVERIES.subTopics.first { it.id == "mars" }

        val page1 = repo.fetchCategoryPage(
            category = NewsCategory.MISSIONS_DISCOVERIES,
            subTopic = mars,
            sortOrder = NewsSortOrder.NEWEST,
            page = 1
        )
        assertEquals(8, page1.items.size)
        assertEquals("nasaimg_missions_discoveries_R1", page1.items.first().id)
        assertTrue(page1.hasMore)

        // Simula nova instância (ex.: reabrir o app) restaurando cursor da cache
        val repoAfterReopen = SpaceNewsRepository(
            nasaScienceNewsApiService = FakeScienceNewsService(),
            donkiApiService = FakeDonkiService(),
            nasaImagesApiService = pagedImages,
            translationService = FakeTranslationService(),
            cache = sharedCache
        )
        val cachedPage1 = repoAfterReopen.fetchCategoryPage(
            category = NewsCategory.MISSIONS_DISCOVERIES,
            subTopic = mars,
            sortOrder = NewsSortOrder.NEWEST,
            page = 1
        )
        assertEquals(8, cachedPage1.items.size)
        assertTrue(cachedPage1.hasMore)

        val page2 = repoAfterReopen.fetchCategoryPage(
            category = NewsCategory.MISSIONS_DISCOVERIES,
            subTopic = mars,
            sortOrder = NewsSortOrder.NEWEST,
            page = 2,
            forceRefresh = true
        )
        assertEquals(
            listOf("nasaimg_missions_discoveries_A1", "nasaimg_missions_discoveries_A2"),
            page2.items.map { it.id }
        )
        assertTrue(page2.hasMore)

        val page3 = repoAfterReopen.fetchCategoryPage(
            category = NewsCategory.MISSIONS_DISCOVERIES,
            subTopic = mars,
            sortOrder = NewsSortOrder.NEWEST,
            page = 3,
            forceRefresh = true
        )
        assertEquals(listOf("nasaimg_missions_discoveries_A3"), page3.items.map { it.id })
        assertFalse(page3.hasMore)
    }

    private fun dto(id: String, title: String, date: String) = NasaImageItemDto(
        data = listOf(
            NasaImageDataDto(
                nasaId = id,
                title = title,
                description = "Desc $id",
                dateCreated = date,
                mediaType = "image"
            )
        ),
        links = listOf(
            NasaImageLinkDto(
                href = "https://images-assets.nasa.gov/image/$id/preview.jpg",
                rel = "preview",
                render = "image"
            )
        )
    )
}
