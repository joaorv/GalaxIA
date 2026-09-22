package com.example.galaxia.data.model

import com.google.gson.annotations.SerializedName

/**
 * Modelo de dados para a resposta da API APOD da NASA.
 */
data class ApodResponse(
    val date: String,
    val explanation: String,
    val hdurl: String? = null,
    @SerializedName("media_type")
    val mediaType: String,
    @SerializedName("service_version")
    val serviceVersion: String? = "v1",
    val title: String,
    val url: String,
    val copyright: String? = null
) : BaseModel
