package com.example.galaxia.data.remote

import com.example.galaxia.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("planetary/apod")
    suspend fun getApodList(
        @Query("api_key") apiKey: String = "DEMO_KEY",
        @Query("count") count: Int = 10
    ): List<ApodResponse>

    @GET("planetary/apod")
    suspend fun getApodByDate(
        @Query("date") date: String,
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): ApodResponse
}
