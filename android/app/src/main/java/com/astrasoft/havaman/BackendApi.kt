package com.astrasoft.havaman

import com.squareup.moshi.Json
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

private const val OWM_BASE_URL = "https://api.openweathermap.org/"
private const val SEARCH_LIMIT = 7

interface OpenWeatherService {
    @GET("geo/1.0/direct")
    suspend fun searchLocation(
        @Query("q") q: String,
        @Query("limit") limit: Int = SEARCH_LIMIT,
        @Query("appid") apiKey: String
    ): List<GeocodingDto>

    @GET("data/2.5/weather")
    suspend fun getCurrentWeather(
        @Query("lat") lat: Double,
        @Query("lon") lon: Double,
        @Query("appid") apiKey: String,
        @Query("units") units: String = "metric",
        @Query("lang") lang: String = "en"
    ): WeatherResponseDto
}

data class GeocodingDto(
    val name: String,
    val lat: Double,
    val lon: Double,
    val country: String?,
    val state: String?
)

data class WeatherResponseDto(
    val weather: List<WeatherDescriptionDto>,
    val main: WeatherMainDto,
    val wind: WindDto,
    val sys: WeatherSysDto,
    val name: String
)

data class WeatherDescriptionDto(val main: String, val description: String)

data class WeatherMainDto(val temp: Double, val humidity: Int)

data class WindDto(val speed: Double, val deg: Double)

data class WeatherSysDto(val country: String)

data class LocationSearchResult(
    val name: String,
    val latitude: Double,
    val longitude: Double,
    val country: String?
)

data class WeatherWisdom(
    val health_card: String,
    val travel_card: String,
    val clothing_card: String,
    val meta: WisdomMeta
)

data class WisdomMeta(val aqi: Int?, val uv: Double?, val temp: Double?)

private val moshi: Moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()

private val retrofit: Retrofit = Retrofit.Builder()
    .baseUrl(OWM_BASE_URL)
    .addConverterFactory(MoshiConverterFactory.create(moshi))
    .client(
        OkHttpClient.Builder()
            .addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BODY })
            .build()
    )
    .build()

private val weatherService: OpenWeatherService = retrofit.create(OpenWeatherService::class.java)

object BackendApi {
    suspend fun searchLocation(query: String, apiKey: String = ""): List<LocationSearchResult> = withContext(Dispatchers.IO) {
        val response = weatherService.searchLocation(query, apiKey = apiKey)
        response.map {
            LocationSearchResult(
                name = listOfNotNull(it.name, it.state).joinToString(", "),
                latitude = it.lat,
                longitude = it.lon,
                country = it.country
            )
        }
    }

    suspend fun fetchWeatherWisdom(lat: Double, lon: Double, apiKey: String = "", language: String = "en"): WeatherWisdom = withContext(Dispatchers.IO) {
        val response = weatherService.getCurrentWeather(lat, lon, apiKey = apiKey, lang = language)
        val weatherDescription = response.weather.firstOrNull()?.description?.replaceFirstChar { it.uppercase() } ?: "Clear skies"

        val temperature = response.main.temp
        val humidity = response.main.humidity
        val windSpeed = response.wind.speed

        val healthCard = buildString {
            append("Current conditions are $weatherDescription.")
            if (humidity >= 80) append(" Humidity is high; stay hydrated.")
            if (windSpeed >= 14) append(" Windy conditions are present.")
        }

        val travelCard = when {
            response.weather.any { it.main.contains("rain", ignoreCase = true) || it.main.contains("storm", ignoreCase = true) } ->
                "Rain or storm expected — choose covered transport and carry an umbrella."
            windSpeed >= 16 ->
                "Strong winds expected — be careful if traveling in open vehicles."
            else ->
                "Travel conditions look good for most routes."
        }

        val clothingCard = when {
            temperature <= 5 -> "Very cold: wear insulated layers and a warm coat."
            temperature <= 15 -> "Cool: wear a jacket and layered clothing."
            temperature <= 25 -> "Mild: light layers are comfortable."
            else -> "Hot: wear breathable fabrics and stay hydrated."
        }

        WeatherWisdom(
            health_card = if (healthCard.isBlank()) "No specific health alerts." else healthCard,
            travel_card = if (travelCard.isBlank()) "No travel advisories." else travelCard,
            clothing_card = clothingCard,
            meta = WisdomMeta(aqi = null, uv = null, temp = temperature)
        )
    }
}
