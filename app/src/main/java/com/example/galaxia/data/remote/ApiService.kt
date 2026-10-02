package com.example.galaxia.data.remote

import com.example.galaxia.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    companion object {
        const val NASA_API_KEY = "iYN1B5NyA3l6hTrzCZCRzEVGeqgagQ6Xq5kUDZTj"
    }

    @GET("planetary/apod")
    suspend fun getApodList(
        @Query("api_key") apiKey: String = NASA_API_KEY,
        @Query("count") count: Int = 10
    ): List<ApodResponse>

    @GET("planetary/apod")
    suspend fun getApodByDate(
        @Query("date") date: String,
        @Query("api_key") apiKey: String = NASA_API_KEY
    ): ApodResponse
}
