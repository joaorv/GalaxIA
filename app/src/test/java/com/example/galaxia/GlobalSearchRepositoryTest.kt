package com.example.galaxia

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.model.DonkiCmeDto
import com.example.galaxia.data.model.DonkiFlareDto
import com.example.galaxia.data.model.DonkiGstDto
import com.example.galaxia.data.model.DonkiNotificationDto
import com.example.galaxia.data.model.EonetCategoryDto
import com.example.galaxia.data.model.EonetEventDto
import com.example.galaxia.data.model.EonetEventsResponse
import com.example.galaxia.data.model.EonetGeometryDto
import com.example.galaxia.data.model.EonetSourceDto
import com.example.galaxia.data.model.GlobalContentType
import com.example.galaxia.data.model.GlobalSearchSource
import com.example.galaxia.data.model.NasaAssetResponse
import com.example.galaxia.data.model.NasaImageDataDto
import com.example.galaxia.data.model.NasaImageItemDto
import com.example.galaxia.data.model.NasaImageLinkDto
import com.example.galaxia.data.model.NasaImagesCollection
import com.example.galaxia.data.model.NasaImagesMetadata
import com.example.galaxia.data.model.NasaImagesResponse
import com.example.galaxia.data.remote.DonkiApiService
import com.example.galaxia.data.remote.EonetApiService
import com.example.galaxia.data.remote.NasaImagesApiService
import com.example.galaxia.data.translation.TranslationService
import com.example.galaxia.repository.GlobalSearchRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class GlobalSearchRepositoryTest {

    private class FakeMediaService(private val fail: Boolean = false) : NasaImagesApiService {
        val calls = AtomicInteger()
        var lastMediaType: String? = null
        var lastPage: Int = 1

        override suspend fun searchImages(
            query: String,
            mediaType: String,
            page: Int,
            pageSize: Int,
            yearStart: Int?,
            yearEnd: Int?
        ): NasaImagesResponse {
            calls.incrementAndGet()
            lastMediaType = mediaType
            lastPage = page
            if (fail) throw IOException("Media API unavailable")

            val items = if (page == 1) {
                listOf(
                    NasaImageItemDto(
                        data = listOf(
                            NasaImageDataDto(
                                nasaId = "IMG_01",
                                title = "Solar Flare Photo",
                                description = "Sun flare photo",
                                dateCreated = "2026-10-01T00:00:00Z",
                                mediaType = "image"
                            )
                        ),
                        links = listOf(
                            NasaImageLinkDto("https://img/solar.jpg", rel = "preview", render = "image")
                        )
                    ),
                    NasaImageItemDto(
                        data = listOf(
                            NasaImageDataDto(
                                nasaId = "VID_01",
                                title = "Solar Flare Video",
                                description = "Sun flare video",
                                dateCreated = "2026-10-02T00:00:00Z",
                                mediaType = "video"
                            )
                        ),
                        links = listOf(
                            NasaImageLinkDto("https://img/solar_vid_thumb.jpg", rel = "preview", render = "image")
                        )
                    )
                )
            } else {
                listOf(
                    NasaImageItemDto(
                        data = listOf(
                            NasaImageDataDto(
                                nasaId = "IMG_02",
                                title = "Page 2 Item",
                                description = "More results",
                                dateCreated = "2026-09-15T00:00:00Z",
                                mediaType = "image"
                            )
                        ),
                        links = listOf(
                            NasaImageLinkDto("https://img/page2.jpg", rel = "preview", render = "image")
                        )
                    )
                )
            }

            return NasaImagesResponse(
                collection = NasaImagesCollection(
                    items = items,
                    metadata = NasaImagesMetadata(totalHits = 25)
                )
            )
        }

        override suspend fun getAssetManifest(nasaId: String) = NasaAssetResponse()
    }

    private class FakeDonkiService(private val fail: Boolean = false) : DonkiApiService {
        override suspend fun getNotifications(startDate: String, endDate: String, type: String, apiKey: String): List<DonkiNotificationDto> {
            if (fail) throw IOException("DONKI down")
            return listOf(
                DonkiNotificationDto(
                    messageType = "FLR",
                    messageID = "FLR-2026",
                    messageURL = "https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/Alert/1",
                    messageIssueTime = "2026-11-20T12:00Z",
                    messageBody = "## Summary: X1.5 solar flare observed."
                )
            )
        }

        override suspend fun getSolarFlares(startDate: String, endDate: String, apiKey: String) = emptyList<DonkiFlareDto>()
        override suspend fun getCmeEvents(startDate: String, endDate: String, apiKey: String) = emptyList<DonkiCmeDto>()
        override suspend fun getGeomagneticStorms(startDate: String, endDate: String, apiKey: String) = emptyList<DonkiGstDto>()
    }

    private class FakeEonetService(private val fail: Boolean = false) : EonetApiService {
        override suspend fun getEvents(status: String, limit: Int, days: Int): EonetEventsResponse {
            if (fail) throw IOException("EONET down")
            return EonetEventsResponse(
                events = listOf(
                    EonetEventDto(
                        id = "EONET_55",
                        title = "California Wildfire",
                        description = "Active wildfire detected.",
                        sources = listOf(EonetSourceDto("InciWeb", "https://inciweb.wildfire.gov/55")),
                        categories = listOf(EonetCategoryDto("wildfires", "Wildfires")),
                        geometry = listOf(EonetGeometryDto(date = "2026-11-22T00:00:00Z"))
                    )
                )
            )
        }
    }

    private class FakeTranslator : TranslationService {
        override suspend fun translateText(text: String, sourceLang: String, targetLang: String): String =
            if (sourceLang == "pt" && targetLang == "en") text else "PT($text)"
    }

    @Test
    fun search_combinesMediaPhotosVideosApodAndDonkiForSolarQuery() = runBlocking {
        val media = FakeMediaService()
        val repo = GlobalSearchRepository(
            nasaImagesApiService = media,
            donkiApiService = FakeDonkiService(),
            eonetApiService = FakeEonetService(),
            apodSearchProvider = { _, _ ->
                listOf(
                    ApodResponse(
                        date = "2026-10-05",
                        title = "Solar Corona APOD",
                        explanation = "Solar eclipse corona",
                        mediaType = "image",
                        url = "https://apod.nasa.gov/solar.jpg",
                        translatedTitle = "Coroa Solar APOD",
                        translatedExplanation = "Coroa do eclipse solar",
                        isTranslated = true
                    )
                )
            },
            translationService = FakeTranslator()
        )

        val page = repo.search("explosão solar")

        assertEquals("image,video", media.lastMediaType)
        assertTrue(page.items.any { it.source == GlobalSearchSource.DONKI && it.contentType == GlobalContentType.SPACE_WEATHER })
        assertTrue(page.items.any { it.source == GlobalSearchSource.APOD && it.contentType == GlobalContentType.PHOTO })
        val videoItem = page.items.first { it.contentType == GlobalContentType.VIDEO }
        assertNull(videoItem.imageUrl)
        assertEquals("https://img/solar_vid_thumb.jpg", videoItem.thumbnailUrl)
        assertTrue(page.items.zipWithNext().all { (a, b) -> a.date >= b.date })
        assertTrue(page.hasMore)
    }

    @Test
    fun search_findsNaturalEventsOnEonetForWildfireQuery() = runBlocking {
        val repo = GlobalSearchRepository(
            nasaImagesApiService = FakeMediaService(),
            donkiApiService = FakeDonkiService(),
            eonetApiService = FakeEonetService(),
            apodSearchProvider = { _, _ -> emptyList() },
            translationService = FakeTranslator()
        )

        val page = repo.search("incêndio florestal")
        assertTrue(page.items.any { it.source == GlobalSearchSource.EONET && it.contentType == GlobalContentType.NATURAL_EVENT })
    }

    @Test
    fun search_isolatesIndividualApiFailures() = runBlocking {
        val repo = GlobalSearchRepository(
            nasaImagesApiService = FakeMediaService(fail = false),
            donkiApiService = FakeDonkiService(fail = true),
            eonetApiService = FakeEonetService(fail = true),
            apodSearchProvider = { _, _ -> throw IOException("APOD down") },
            translationService = FakeTranslator()
        )

        val page = repo.search("explosão solar")
        assertTrue(page.items.isNotEmpty())
        assertTrue(page.items.all { it.source == GlobalSearchSource.NASA_MEDIA })
    }

    @Test
    fun search_usesCacheOnRepeatAndSupportsPagination() = runBlocking {
        val media = FakeMediaService()
        val repo = GlobalSearchRepository(
            nasaImagesApiService = media,
            donkiApiService = FakeDonkiService(),
            eonetApiService = FakeEonetService(),
            apodSearchProvider = { _, _ -> emptyList() },
            translationService = FakeTranslator()
        )

        repo.search("Saturno", page = 1)
        repo.search("Saturno", page = 1)
        assertEquals(1, media.calls.get())

        val page2 = repo.search("Saturno", page = 2, searchTerm = "saturn")
        assertEquals(2, media.calls.get())
        assertEquals(2, media.lastPage)
        assertEquals("IMG_02".let { "nasamedia_$it" }, page2.items.first().id)
    }
}
