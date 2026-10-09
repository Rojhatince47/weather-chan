package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoForecastDto(
    @SerialName("current") val current: OpenMeteoCurrentDto = OpenMeteoCurrentDto(),
    @SerialName("hourly") val hourly: OpenMeteoHourlyDto = OpenMeteoHourlyDto(),
    @SerialName("daily") val daily: OpenMeteoDailyDto = OpenMeteoDailyDto()
)

@Serializable
data class OpenMeteoCurrentDto(
    @SerialName("temperature_2m") val temperature: Double = 0.0,
    @SerialName("relative_humidity_2m") val humidity: Int = 0,
    @SerialName("weather_code") val weatherCode: Int = 3,
    @SerialName("wind_speed_10m") val windSpeed: Double = 0.0,
    @SerialName("is_day") val isDay: Int? = null
)

@Serializable
data class OpenMeteoHourlyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("temperature_2m") val temperature: List<Double> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("precipitation_probability") val precipitationProbability: List<Int?> = emptyList()
)

@Serializable
data class OpenMeteoDailyDto(
    @SerialName("time") val time: List<String> = emptyList(),
    @SerialName("weather_code") val weatherCode: List<Int> = emptyList(),
    @SerialName("temperature_2m_max") val temperatureMax: List<Double> = emptyList(),
    @SerialName("temperature_2m_min") val temperatureMin: List<Double> = emptyList(),
    @SerialName("sunrise") val sunrise: List<String> = emptyList(),
    @SerialName("sunset") val sunset: List<String> = emptyList()
)
