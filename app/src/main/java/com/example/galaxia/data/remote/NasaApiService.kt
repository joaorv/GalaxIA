package com.example.galaxia.data.remote

import com.example.galaxia.data.model.DonkiNotification
import com.example.galaxia.data.model.NeoFeedResponse
import retrofit2.http.GET
import retrofit2.http.Query

/**
 * Endpoints de api.nasa.gov usados para notícias e curiosidades do feed.
 * A 'api_key' é adicionada automaticamente pelo interceptor do [RetrofitClient].
 */
interface NasaApiService {

    /** Asteroides com aproximação da Terra entre as datas informadas (yyyy-MM-dd, máx. 7 dias). */
    @GET("neo/rest/v1/feed")
    suspend fun getNeoFeed(
        @Query("start_date") startDate: String,
        @Query("end_date") endDate: String
    ): NeoFeedResponse

    /** Notificações de clima espacial emitidas entre as datas informadas (yyyy-MM-dd). */
    @GET("DONKI/notifications")
    suspend fun getDonkiNotifications(
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String,
        @Query("type") type: String = "all"
    ): List<DonkiNotification>
}
