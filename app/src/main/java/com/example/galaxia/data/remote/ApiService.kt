package com.example.galaxia.data.remote

import com.example.galaxia.BuildConfig
import com.example.galaxia.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @GET("apod-basic")
    suspend fun getApodList(
        @Query("count") count: Int = 10,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): List<ApodResponse>
    @GET("apod-basic/{dateCode}")
    suspend fun getApodByDate(
        @Path("dateCode") dateCode: String,
        @Query("api_key") apiKey: String = BuildConfig.NASA_API_KEY
    ): ApodResponse
}
