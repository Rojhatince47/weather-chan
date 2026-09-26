package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import retrofit2.http.GET
import retrofit2.http.Query

interface OpenMeteoApi {

    @GET("v1/forecast")
    suspend fun forecast(
        @Query("latitude") latitude: String,
        @Query("longitude") longitude: String,
        @Query("current") current: String = "temperature_2m,relative_humidity_2m,weather_code,wind_speed_10m",
        @Query("hourly") hourly: String = "temperature_2m,weather_code,precipitation_probability",
        @Query("daily") daily: String = "weather_code,temperature_2m_max,temperature_2m_min",
        @Query("timezone") timezone: String = "auto",
        @Query("forecast_days") forecastDays: Int = 7
    ): OpenMeteoForecastDto
}
