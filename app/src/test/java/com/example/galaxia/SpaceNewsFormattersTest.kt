package com.example.galaxia

import com.example.galaxia.data.local.FavoriteEntity
import com.example.galaxia.data.local.allImageUrls
import com.example.galaxia.data.model.DonkiNotificationDto
import com.example.galaxia.data.model.GlobalContentType
import com.example.galaxia.data.model.GlobalSearchItem
import com.example.galaxia.data.model.GlobalSearchSource
import com.example.galaxia.data.model.NasaImageDataDto
import com.example.galaxia.data.model.NasaImageItemDto
import com.example.galaxia.data.model.NasaImageLinkDto
import com.example.galaxia.data.model.NasaSciencePostDto
import com.example.galaxia.data.model.NewsCategory
import com.example.galaxia.data.model.SpaceNewsItem
import com.example.galaxia.data.model.WpAuthor
import com.example.galaxia.data.model.WpEmbedded
import com.example.galaxia.data.model.WpFeaturedMedia
import com.example.galaxia.data.model.WpMediaDetails
import com.example.galaxia.data.model.WpMediaSize
import com.example.galaxia.data.model.WpRenderedText
import com.example.galaxia.data.model.toFavoriteEntity
import com.example.galaxia.data.remote.SpaceNewsFormatters
import com.google.gson.Gson
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SpaceNewsFormattersTest {

    @Test
    fun mapNasaSciencePost_cleansHtmlDecodesEntitiesAndAddsReadingTimeChip() {
        val dto = NasaSciencePostDto(
            id = 4201L,
            date = "2026-11-24T12:00:00",
            link = "https://science.nasa.gov/missions/webb/new-galaxy-discovery/",
            title = WpRenderedText("Webb&#8217;s New View &amp; Discovery"),
            excerpt = WpRenderedText("<p>Astronomers spotted an ancient galaxy.</p>"),
            content = WpRenderedText("<p>Full article body with <strong>scientific</strong> details.</p>"),
            embedded = WpEmbedded(
                featuredMedia = listOf(
                    WpFeaturedMedia(
                        sourceUrl = "https://assets.science.nasa.gov/full.jpg",
                        mediaDetails = WpMediaDetails(
                            sizes = mapOf("medium_large" to WpMediaSize("https://assets.science.nasa.gov/med.jpg"))
                        )
                    )
                ),
                authors = listOf(WpAuthor("NASA Science Team"))
            )
        )

        val item = SpaceNewsFormatters.mapNasaSciencePost(dto)
        assertNotNull(item)
        item!!

        assertEquals("nasasci_4201", item.id)
        assertEquals(NewsCategory.NASA_HIGHLIGHTS, item.category)
        assertEquals("Webb\u2019s New View & Discovery", item.title)
        assertEquals("2026-11-24", item.date)
        assertEquals("https://assets.science.nasa.gov/med.jpg", item.imageUrl)
        assertEquals("https://science.nasa.gov/missions/webb/new-galaxy-discovery/", item.sourceUrl)
        assertEquals("NASA Science Team", item.authorOrCredit)
        assertTrue(item.metadataChips.any { it.contains("min de leitura") })
    }

    @Test
    fun mapDonkiNotification_extractsMetricsCasualSubtitleAndStripsDisclaimer() {
        val dto = DonkiNotificationDto(
            messageType = "CME",
            messageID = "20261120-AL-001",
            messageURL = "https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/Alert/12345",
            messageIssueTime = "2026-11-20T14:30Z",
            messageBody = """
                ## Message Type: CME
                ## Summary:
                A partial halo CME (X1.2 flare associated) was detected by SOHO LASCO C2 at 14:12Z with speed 720 km/s.
                See details at https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/Alert/12345
                ## Disclaimer
                NOAA is the official source for alerts.
            """.trimIndent()
        )

        val item = SpaceNewsFormatters.mapDonkiNotification(dto)
        assertNotNull(item)
        item!!

        assertEquals("donki_20261120-AL-001", item.id)
        assertEquals(NewsCategory.SPACE_WEATHER, item.category)
        assertEquals("Ejeção de Massa Coronal (CME)", item.title)
        assertEquals("Grande nuvem de plasma solar lançada pelo espaço", item.subtitleTag)
        assertEquals("2026-11-20", item.date)
        assertNull(item.imageUrl)
        assertEquals("https://kauai.ccmc.gsfc.nasa.gov/DONKI/view/Alert/12345", item.sourceUrl)
        assertTrue(item.metadataChips.contains("720 km/s"))
        assertTrue(item.metadataChips.contains("Classe X1.2"))
        assertFalse(item.fullDescription.contains("NOAA is the official source"))
        assertFalse(item.fullDescription.contains("https://"))
    }

    @Test
    fun mapNasaImageItem_acceptsImageAndRejectsVideos() {
        val validImageDto = NasaImageItemDto(
            data = listOf(
                NasaImageDataDto(
                    nasaId = "PIA12345",
                    title = "Pillars of Creation",
                    description = "<p>Captured by James Webb Space Telescope.</p>",
                    dateCreated = "2026-10-19T00:00:00Z",
                    mediaType = "image",
                    center = "GSFC"
                )
            ),
            links = listOf(
                NasaImageLinkDto(
                    href = "https://images-assets.nasa.gov/image/PIA12345/PIA12345~thumb.jpg",
                    rel = "preview",
                    render = "image"
                )
            )
        )

        val mapped = SpaceNewsFormatters.mapNasaImageItem(validImageDto, NewsCategory.UNIVERSE_CURIOSITIES)
        assertNotNull(mapped)
        mapped!!
        assertEquals("https://images-assets.nasa.gov/image/PIA12345/PIA12345~thumb.jpg", mapped.imageUrl)
        assertEquals("https://images.nasa.gov/details/PIA12345", mapped.sourceUrl)
        assertEquals("Captured by James Webb Space Telescope.", mapped.fullDescription)

        val videoDto = NasaImageItemDto(
            data = listOf(
                NasaImageDataDto(
                    nasaId = "VID999",
                    title = "Launch Video",
                    mediaType = "video"
                )
            ),
            links = listOf(
                NasaImageLinkDto(
                    href = "https://images-assets.nasa.gov/video/VID999/VID999~orig.mp4",
                    rel = "preview",
                    render = "video"
                )
            )
        )

        assertNull(SpaceNewsFormatters.mapNasaImageItem(videoDto, NewsCategory.MISSIONS_DISCOVERIES))
    }

    @Test
    fun groupDuplicateTitleItems_mergesSameTitlePhotosIntoCarouselAndRespectsLimit() {
        val items = listOf(
            SpaceNewsItem(
                id = "1",
                category = NewsCategory.UNIVERSE_CURIOSITIES,
                sourceName = "NASA Image Library",
                title = "Carina Nebula",
                summary = "",
                fullDescription = "",
                date = "2026-10-01",
                imageUrl = "https://img/1.jpg",
                sourceUrl = "https://images.nasa.gov/details/1"
            ),
            SpaceNewsItem(
                id = "2",
                category = NewsCategory.UNIVERSE_CURIOSITIES,
                sourceName = "NASA Image Library",
                title = "  carina   NEBULA ",
                summary = "Rich summary",
                fullDescription = "Rich description",
                date = "2026-09-15",
                imageUrl = "https://img/2.jpg",
                sourceUrl = "https://images.nasa.gov/details/2"
            ),
            SpaceNewsItem(
                id = "3",
                category = NewsCategory.UNIVERSE_CURIOSITIES,
                sourceName = "NASA Image Library",
                title = "Carina Nebula",
                summary = "Third",
                fullDescription = "Third",
                date = "2026-08-01",
                imageUrl = "https://img/3.jpg",
                sourceUrl = "https://images.nasa.gov/details/3"
            ),
            SpaceNewsItem(
                id = "4",
                category = NewsCategory.UNIVERSE_CURIOSITIES,
                sourceName = "NASA Image Library",
                title = "Orion Nebula",
                summary = "Orion",
                fullDescription = "Orion",
                date = "2026-07-01",
                imageUrl = "https://img/4.jpg",
                sourceUrl = "https://images.nasa.gov/details/4"
            ),
            SpaceNewsItem(
                id = "donki_1",
                category = NewsCategory.SPACE_WEATHER,
                sourceName = "NASA DONKI",
                title = "Tempestade Geomagnética",
                summary = "Kp 5",
                fullDescription = "Kp 5",
                date = "2026-10-01",
                imageUrl = null,
                sourceUrl = "https://donki/1"
            ),
            SpaceNewsItem(
                id = "donki_2",
                category = NewsCategory.SPACE_WEATHER,
                sourceName = "NASA DONKI",
                title = "Tempestade Geomagnética",
                summary = "Kp 6",
                fullDescription = "Kp 6",
                date = "2026-09-20",
                imageUrl = null,
                sourceUrl = "https://donki/2"
            )
        )

        val grouped = SpaceNewsFormatters.groupDuplicateTitleItems(items, maxPhotosPerCard = 2)

        assertEquals(4, grouped.size)
        val carina = grouped[0]
        assertEquals("Carina Nebula", carina.title)
        assertEquals(listOf("https://img/1.jpg", "https://img/2.jpg"), carina.allImageUrls)
        assertEquals("Rich description", carina.fullDescription)
        assertEquals("Orion Nebula", grouped[1].title)
        // Alertas sem imagem (DONKI) permanecem individuais
        assertEquals("donki_1", grouped[2].id)
        assertEquals("donki_2", grouped[3].id)
    }

    @Test
    fun gsonDeserialization_withoutGalleryOrMetadataFields_doesNotCrash() {
        val legacyJson = """
            {
              "id": "nasaimg_1",
              "category": "MISSIONS_DISCOVERIES",
              "sourceName": "NASA Image Library",
              "title": "James Webb First Deep Field",
              "summary": "Deep space view",
              "fullDescription": "Deep space full view",
              "date": "2026-10-01",
              "imageUrl": "https://img/webb.jpg",
              "sourceUrl": "https://images.nasa.gov/details/1"
            }
        """.trimIndent()

        val item = Gson().fromJson(legacyJson, SpaceNewsItem::class.java)
        assertEquals(listOf("https://img/webb.jpg"), item.allImageUrls)
        assertEquals(emptyList<String>(), item.safeMetadataChips)

        val grouped = SpaceNewsFormatters.groupDuplicateTitleItems(listOf(item))
        assertEquals(1, grouped.size)
        assertEquals(listOf("https://img/webb.jpg"), grouped.first().allImageUrls)
    }

    @Test
    fun toFavoriteEntity_preservesGalleryAndLegacyFavoriteJsonDoesNotCrash() {
        val newsItem = SpaceNewsItem(
            id = "nasaimg_10",
            category = NewsCategory.UNIVERSE_CURIOSITIES,
            sourceName = "NASA Image Library",
            title = "Saturn Rings",
            summary = "Summary",
            fullDescription = "Full text",
            date = "2026-11-01",
            imageUrl = "https://img/saturn1.jpg",
            galleryImageUrls = listOf("https://img/saturn2.jpg", "https://img/saturn3.jpg"),
            sourceUrl = "https://images.nasa.gov/details/10"
        )

        val entity = newsItem.toFavoriteEntity()
        assertEquals(
            listOf("https://img/saturn1.jpg", "https://img/saturn2.jpg", "https://img/saturn3.jpg"),
            entity.allImageUrls
        )

        val legacyFavoriteJson = """
            {
              "id": "apod_2026-10-24",
              "itemType": "APOD",
              "title": "Orion",
              "subtitleOrDate": "2026-10-24",
              "explanationOrBody": "Text",
              "imageUrl": "https://img/orion.jpg"
            }
        """.trimIndent()

        val legacyEntity = Gson().fromJson(legacyFavoriteJson, FavoriteEntity::class.java)
        assertEquals(listOf("https://img/orion.jpg"), legacyEntity.allImageUrls)
    }

    @Test
    fun nasaVideoHelpers_andGlobalSearchVideoToFavoriteEntity_workCorrectly() {
        assertEquals(
            "GSFC_2026_VID",
            SpaceNewsFormatters.extractNasaMediaDetailsId("https://images.nasa.gov/details/GSFC_2026_VID")
        )
        assertNull(SpaceNewsFormatters.extractNasaMediaDetailsId("https://science.nasa.gov/apod/"))

        val selected = SpaceNewsFormatters.selectBestNasaVideoUrl(
            listOf(
                "http://images-assets.nasa.gov/video/My Video/A~orig.mov",
                "http://images-assets.nasa.gov/video/My Video/A~large.mp4",
                "http://images-assets.nasa.gov/video/My Video/A~medium.mp4",
                "http://images-assets.nasa.gov/video/My Video/A~thumb.jpg"
            )
        )
        assertEquals("https://images-assets.nasa.gov/video/My%20Video/A~medium.mp4", selected)

        val videoSearchItem = GlobalSearchItem(
            id = "nasamedia_GSFC_2026_VID",
            source = GlobalSearchSource.NASA_MEDIA,
            contentType = GlobalContentType.VIDEO,
            title = "Mars Helicopter Flight",
            description = "Video description",
            date = "2026-10-01",
            imageUrl = null,
            thumbnailUrl = "https://img/thumb.jpg",
            mediaType = "video",
            sourceUrl = "https://images.nasa.gov/details/GSFC_2026_VID"
        )

        val entity = videoSearchItem.toFavoriteEntity()
        assertEquals("video", entity.mediaType)
        assertEquals("https://images.nasa.gov/details/GSFC_2026_VID", entity.extraUrl)
        assertEquals("https://img/thumb.jpg", entity.thumbnailUrl)
    }
}
