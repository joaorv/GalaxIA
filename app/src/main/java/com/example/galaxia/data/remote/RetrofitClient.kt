package com.example.galaxia.data.remote

import com.example.galaxia.GalaxiaApplication
import com.example.galaxia.data.local.NasaApiKeyManager
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {
    private const val BASE_URL = "https://science.nasa.gov/wp-json/wp/v2/"

    /**
     * Interceptor que injeta dinamicamente a 'api_key' ativa (chave configurada pelo usuário
     * ou DEMO_KEY por padrão) em todas as requisições enviadas à API da NASA.
     */
    private val apiKeyInterceptor = Interceptor { chain ->
        val originalRequest = chain.request()
        val activeKey = NasaApiKeyManager.getInstance(GalaxiaApplication.instance).getActiveApiKey()

        val newUrl = originalRequest.url.newBuilder()
            .setQueryParameter("api_key", activeKey)
            .build()

        val newRequest = originalRequest.newBuilder()
            .url(newUrl)
            .build()

        chain.proceed(newRequest)
    }

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(apiKeyInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    private val publicOkHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(25, TimeUnit.SECONDS)
            .readTimeout(25, TimeUnit.SECONDS)
            .writeTimeout(25, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()
    }

    val apiService: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }

    val donkiApiService: DonkiApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://api.nasa.gov/")
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(DonkiApiService::class.java)
    }

    val nasaImagesApiService: NasaImagesApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://images-api.nasa.gov/")
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NasaImagesApiService::class.java)
    }

    val nasaScienceNewsApiService: NasaScienceNewsApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(NasaScienceNewsApiService::class.java)
    }

    val eonetApiService: EonetApiService by lazy {
        Retrofit.Builder()
            .baseUrl("https://eonet.gsfc.nasa.gov/api/v3/")
            .client(publicOkHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(EonetApiService::class.java)
    }
}
