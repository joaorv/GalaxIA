package com.example.galaxia.data.remote

import com.example.galaxia.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Definição da API remota do GalaxIA (camada Data - remote).
 */
interface ApiService {

    @GET("planetary/apod")
    suspend fun getApod(
        @Query("api_key") apiKey: String = "DEMO_KEY"
    ): ApodResponse
}
