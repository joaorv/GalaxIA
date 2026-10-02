package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.RetrofitClient
import retrofit2.HttpException

/**
 * Repositório responsável exclusivamente pela comunicação com a API NASA APOD.
 */
class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository {

    suspend fun getApodList(count: Int = 10): List<ApodResponse> {
        return try {
            apiService.getApodList(count = count)
        } catch (e: HttpException) {
            if (e.code() == 403 || e.code() == 429) {
                // Tenta com a DEMO_KEY caso a chave primária atinja limite ou falhe
                apiService.getApodList(apiKey = "DEMO_KEY", count = count)
            } else {
                throw e
            }
        }
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        return try {
            apiService.getApodByDate(date = date)
        } catch (e: HttpException) {
            if (e.code() == 403 || e.code() == 429) {
                apiService.getApodByDate(date = date, apiKey = "DEMO_KEY")
            } else {
                throw e
            }
        }
    }
}
