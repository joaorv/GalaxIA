package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.RetrofitClient
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Repositório responsável exclusivamente pela comunicação com a API NASA APOD (science.nasa.gov).
 */
class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository {

    suspend fun getApodList(count: Int = 10): List<ApodResponse> {
        return apiService.getApodList(count = count)
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        val dateCode = formatDateToCode(date)
        return apiService.getApodByDate(dateCode = dateCode)
    }

    private fun formatDateToCode(dateString: String): String {
        return try {
            val localDate = LocalDate.parse(dateString, DateTimeFormatter.ISO_LOCAL_DATE)
            localDate.format(DateTimeFormatter.ofPattern("yyMMdd"))
        } catch (_: Exception) {
            dateString.replace("-", "").takeLast(6)
        }
    }
}
