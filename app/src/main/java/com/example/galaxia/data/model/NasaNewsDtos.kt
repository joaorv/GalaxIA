package com.example.galaxia.data.model

import com.google.gson.annotations.SerializedName

// --- NASA Science News (WordPress REST API) ---

data class NasaSciencePostDto(
    val id: Long? = null,
    val date: String? = null,
    val link: String? = null,
    val title: WpRenderedText? = null,
    val excerpt: WpRenderedText? = null,
    val content: WpRenderedText? = null,
    @SerializedName("_embedded")
    val embedded: WpEmbedded? = null
)

data class WpRenderedText(
    val rendered: String? = null
)

data class WpEmbedded(
    @SerializedName("wp:featuredmedia")
    val featuredMedia: List<WpFeaturedMedia>? = null,
    @SerializedName("author")
    val authors: List<WpAuthor>? = null
)

data class WpFeaturedMedia(
    @SerializedName("source_url")
    val sourceUrl: String? = null,
    @SerializedName("media_details")
    val mediaDetails: WpMediaDetails? = null
)

data class WpMediaDetails(
    val sizes: Map<String, WpMediaSize>? = null
)

data class WpMediaSize(
    @SerializedName("source_url")
    val sourceUrl: String? = null
)

data class WpAuthor(
    val name: String? = null
)

// --- NASA DONKI (Space Weather) ---

data class DonkiNotificationDto(
    val messageType: String? = null,
    val messageID: String? = null,
    val messageURL: String? = null,
    val messageIssueTime: String? = null,
    val messageBody: String? = null
)

data class DonkiFlareDto(
    val flrID: String? = null,
    val beginTime: String? = null,
    val peakTime: String? = null,
    val classType: String? = null,
    val sourceLocation: String? = null,
    val note: String? = null,
    val link: String? = null
)

data class DonkiCmeDto(
    val activityID: String? = null,
    val startTime: String? = null,
    val note: String? = null,
    val link: String? = null
)

data class DonkiGstDto(
    val gstID: String? = null,
    val startTime: String? = null,
    val allKpIndex: List<DonkiKpIndexDto>? = null,
    val link: String? = null
)

data class DonkiKpIndexDto(
    val kpIndex: Double? = null,
    val source: String? = null
)

// --- NASA Image and Video Library ---

data class NasaImagesResponse(
    val collection: NasaImagesCollection? = null
)

data class NasaImagesCollection(
    val items: List<NasaImageItemDto>? = null,
    val metadata: NasaImagesMetadata? = null
)

data class NasaImageItemDto(
    val href: String? = null,
    val data: List<NasaImageDataDto>? = null,
    val links: List<NasaImageLinkDto>? = null
)

data class NasaImageDataDto(
    @SerializedName("nasa_id")
    val nasaId: String? = null,
    val title: String? = null,
    val description: String? = null,
    @SerializedName("description_508")
    val description508: String? = null,
    @SerializedName("date_created")
    val dateCreated: String? = null,
    @SerializedName("media_type")
    val mediaType: String? = null,
    val center: String? = null,
    val photographer: String? = null,
    @SerializedName("secondary_creator")
    val secondaryCreator: String? = null
)

data class NasaImageLinkDto(
    val href: String? = null,
    val rel: String? = null,
    val render: String? = null
)

data class NasaImagesMetadata(
    @SerializedName("total_hits")
    val totalHits: Int? = null
)

data class NasaAssetResponse(
    val collection: NasaAssetCollection? = null
)

data class NasaAssetCollection(
    val items: List<NasaAssetItemDto>? = null
)

data class NasaAssetItemDto(
    val href: String? = null
)

// --- NASA EONET v3 (Earth Observatory Natural Event Tracker — usado na Pesquisa Global) ---

data class EonetEventsResponse(
    val events: List<EonetEventDto>? = null
)

data class EonetEventDto(
    val id: String? = null,
    val title: String? = null,
    val description: String? = null,
    val link: String? = null,
    val categories: List<EonetCategoryDto>? = null,
    val sources: List<EonetSourceDto>? = null,
    val geometry: List<EonetGeometryDto>? = null
)

data class EonetCategoryDto(
    val id: String? = null,
    val title: String? = null
)

data class EonetSourceDto(
    val id: String? = null,
    val url: String? = null
)

data class EonetGeometryDto(
    val date: String? = null,
    val magnitudeValue: Double? = null,
    val magnitudeUnit: String? = null
)
