package com.example.galaxia.data.remote

import com.example.galaxia.data.model.ApodResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Interface Retrofit para consumo dos endpoints da API de Astronomia da NASA (APOD - Astronomy Picture of the Day).
 *
 * Fornece métodos assíncronos via Kotlin Coroutines para obter listas aleatórias ou fotos históricas por data.
 */
interface ApiService {

    companion object {
        /** Chave de API configurada para o perfil LuisAraujo-5 no projeto GalaxIA. */
        const val NASA_API_KEY = "iYN1B5NyA3l6hTrzCZCRzEVGeqgagQ6Xq5kUDZTj"
    }

    /**
     * Obtém uma lista aleatória de publicações da NASA APOD.
     *
     * @param apiKey Chave de autenticação da API da NASA.
     * @param count Quantidade de itens a serem retornados.
     */
    @GET("planetary/apod")
    suspend fun getApodList(
        @Query("api_key") apiKey: String = NASA_API_KEY,
        @Query("count") count: Int = 10
    ): List<ApodResponse>

    /**
     * Obtém a publicação de uma data específica da NASA APOD.
     *
     * @param date Data no formato YYYY-MM-DD.
     * @param apiKey Chave de autenticação da API da NASA.
     */
    @GET("planetary/apod")
    suspend fun getApodByDate(
        @Query("date") date: String,
        @Query("api_key") apiKey: String = NASA_API_KEY
    ): ApodResponse
}
