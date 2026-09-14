package com.example.galaxia.repository

import com.example.galaxia.data.model.ApodResponse
import com.example.galaxia.data.remote.ApiService
import com.example.galaxia.data.remote.RetrofitClient

class ApodRepository(
    private val apiService: ApiService = RetrofitClient.apiService
) : BaseRepository {

    suspend fun getApod(): ApodResponse {
        return apiService.getApod()
    }
}
