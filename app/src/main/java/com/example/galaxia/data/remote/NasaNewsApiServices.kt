package com.example.galaxia.data.remote

import com.example.galaxia.BuildConfig
import com.example.galaxia.data.model.DonkiCmeDto
import com.example.galaxia.data.model.DonkiFlareDto
import com.example.galaxia.data.model.DonkiGstDto
import com.example.galaxia.data.model.DonkiNotificationDto
import com.example.galaxia.data.model.EonetEventsResponse
import com.example.galaxia.data.model.NasaAssetResponse
import com.example.galaxia.data.model.NasaImagesResponse
import com.example.galaxia.data.model.NasaSciencePostDto
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

/**
 * Serviço Retrofit para notícias e artigos oficiais da NASA Science (WordPress REST API).
 */
interface NasaScienceNewsApiService {

    @GET("posts")
    suspend fun getPosts(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("_embed") embed: Int = 1
    ): Response<List<NasaSciencePostDto>>
}

/**
 * Serviço Retrofit para a API NASA DONKI (Clima Espacial).
 */
interface DonkiApiService {

    @GET("DONKI/notifications")
    suspend fun getNotifications(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("type") type: String = "all",
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<DonkiNotificationDto>

    @GET("DONKI/FLR")
    suspend fun getSolarFlares(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<DonkiFlareDto>

    @GET("DONKI/CME")
    suspend fun getCmeEvents(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<DonkiCmeDto>

    @GET("DONKI/GST")
    suspend fun getGeomagneticStorms(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<DonkiGstDto>
}

/**
 * Serviço Retrofit para a NASA Image and Video Library.
 */
interface NasaImagesApiService {

    @GET("search")
    suspend fun searchImages(
        @Query("q") query: String,
        @Query("media_type") mediaType: String = "image",
        @Query("page") page: Int = 1,
        @Query("page_size") pageSize: Int = 12,
        @Query("year_start") yearStart: Int? = null,
        @Query("year_end") yearEnd: Int? = null
    ): NasaImagesResponse

    @GET("asset/{nasaId}")
    suspend fun getAssetManifest(
        @Path("nasaId") nasaId: String
    ): NasaAssetResponse
}

/**
 * Serviço Retrofit para a API NASA EONET v3 (usado na Pesquisa Global).
 */
interface EonetApiService {

    @GET("events")
    suspend fun getEvents(
        @Query("status") status: String = "all",
        @Query("limit") limit: Int = 40,
        @Query("days") days: Int = 60
    ): EonetEventsResponse
}
