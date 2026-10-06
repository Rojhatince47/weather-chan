package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoGeocodingApi {

    @GET("v1/search")
    suspend fun search(
        @Query("name") name: String,
        @Query("count") count: Int = 10,
        @Query("language") language: String,
        @Query("format") format: String = "json"
    ): OpenMeteoGeocodingDto
}
