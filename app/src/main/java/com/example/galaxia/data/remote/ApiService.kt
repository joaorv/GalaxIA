package com.example.galaxia.data.remote

/**
 * Definição da API remota do GalaxIA (camada Data - remote).
 *
 * Quando o Retrofit for adicionado ao build.gradle.kts, esta interface
 * passará a declarar os endpoints consumidos da API pública da NASA
 * (ex.: @GET("planetary/apod") para a Foto Astronômica do Dia), com
 * seus parâmetros de data e chave de API.
 */
interface ApiService
