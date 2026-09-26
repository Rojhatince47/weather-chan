package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoGeocodingApi {

    @GET("https://geocoding-api.open-meteo.com/v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 5,
        @Query("language") language: String = "tr",
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingDto
}
