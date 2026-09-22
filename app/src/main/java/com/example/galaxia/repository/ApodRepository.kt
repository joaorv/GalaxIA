package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.RetrofitClient

class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository {

    suspend fun getApodList(count: Int = 10): List<ApodResponse> {
        return apiService.getApodList(count = count)
    }

    suspend fun getApodByDate(date: String): ApodResponse {
        return apiService.getApodByDate(date = date)
    }
}
