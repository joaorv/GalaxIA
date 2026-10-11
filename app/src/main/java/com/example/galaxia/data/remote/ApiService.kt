package com.example.galaxia.data.remote

import com.example.galaxia.BuildConfig
import com.example.galaxia.data.model.ApodResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    /**
     * Lista paginada de APODs, sempre da mais recente para a mais antiga.
     * A API (WordPress) ignora 'count' e 'orderby=rand'; só 'per_page' e 'page' são respeitados.
     */
    @GET("apod-basic")
    suspend fun getApodList(
        @Query("per_page") perPage: Int = 10,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<ApodResponse>

    /**
     * Mesma listagem, mas com acesso aos cabeçalhos (X-WP-Total traz o total de APODs publicados).
     */
    @GET("apod-basic")
    suspend fun getApodListResponse(
        @Query("per_page") perPage: Int = 1,
        @Query("page") page: Int = 1,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): Response<List<ApodResponse>>

    @GET("apod-basic/{dateCode}")
    suspend fun getApodByDate(
        @Path("dateCode") dateCode: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): ApodResponse
}
