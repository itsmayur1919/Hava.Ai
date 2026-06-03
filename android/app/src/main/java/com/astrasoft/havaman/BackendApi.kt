package com.astrasoft.havaman

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

// Use emulator loopback by default. If testing on device, replace with backend host.
private const val BASE_URL = "http://10.0.2.2:8000/"

interface ApiService {
    @GET("api/location/search")
    suspend fun searchLocation(
        @Query("q") q: String,
        @Header("Authorization") authToken: String? = null
    ): List<LocationDto>

    @GET("api/weather/wisdom")
    suspend fun getWeatherWisdom(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Header("Authorization") authToken: String? = null
    ): WeatherWisdomDto
}

data class LocationDto(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?
)

data class WeatherWisdomDto(
    @Json(name = "health_card") val healthCard: String,
    @Json(name = "travel_card") val travelCard: String,
    @Json(name = "clothing_card") val clothingCard: String,
    val meta: MetaDto
)

data class MetaDto(val aqi: Int?, val uv: Double?, val temp: Double?)

object BackendApi {
    private var googleIdToken: String? = null

    private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        })
        .addInterceptor(Interceptor { chain ->
            val originalRequest = chain.request()
            val requestBuilder = originalRequest.newBuilder()
            
            // Add authentication header if token is available
            googleIdToken?.let {
                requestBuilder.addHeader("Authorization", "Bearer $it")
            }
            
            chain.proceed(requestBuilder.build())
        })
        .build()

    private val retrofit: Retrofit = Retrofit.Builder()
        .baseUrl(BASE_URL)
        .client(httpClient)
        .addConverterFactory(MoshiConverterFactory.create(moshi))
        .build()

    private val service: ApiService = retrofit.create(ApiService::class.java)

    /**
     * Set the Google ID token for authentication.
     * Call this after successful Google Sign-In.
     */
    fun setGoogleIdToken(token: String?) {
        googleIdToken = token
    }

    /**
     * Clear the authentication token on sign-out.
     */
    fun clearAuthentication() {
        googleIdToken = null
    }

    suspend fun searchLocation(query: String): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val res = service.searchLocation(query, googleIdToken?.let { "Bearer $it" })
        res.map { LocationSearchResult(it.name, it.latitude, it.longitude, it.country) }
    }

    suspend fun fetchWeatherWisdom(lat: Double, lon: Double): WeatherWisdom = withContext(Dispatchers.IO) {
        val dto = service.getWeatherWisdom(lat, lon, googleIdToken?.let { "Bearer $it" })
        WeatherWisdom(dto.healthCard, dto.travelCard, dto.clothingCard, WisdomMeta(dto.meta.aqi, dto.meta.uv, dto.meta.temp))
    }
}
