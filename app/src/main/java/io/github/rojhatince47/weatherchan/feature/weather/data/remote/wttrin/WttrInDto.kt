package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WttrInDto(
    @SerialName("current_condition") val currentCondition: List<WttrInCurrentDto> = emptyList(),
    @SerialName("nearest_area") val nearestArea: List<WttrInAreaDto> = emptyList(),
    @SerialName("weather") val weather: List<WttrInDayDto> = emptyList()
)

@Serializable
data class WttrInCurrentDto(
    @SerialName("observation_time") val observationTime: String = "",
    @SerialName("temp_C") val tempC: String = "0",
    @SerialName("humidity") val humidity: String = "0",
    @SerialName("windspeedKmph") val windSpeedKmph: String = "0",
    @SerialName("weatherCode") val weatherCode: String = "113",
    @SerialName("weatherDesc") val weatherDesc: List<WttrInTextDto> = emptyList()
)

@Serializable
data class WttrInAreaDto(
    @SerialName("areaName") val areaName: List<WttrInTextDto> = emptyList(),
    @SerialName("country") val country: List<WttrInTextDto> = emptyList(),
    @SerialName("region") val region: List<WttrInTextDto> = emptyList(),
    @SerialName("latitude") val latitude: String = "0.0",
    @SerialName("longitude") val longitude: String = "0.0"
)

@Serializable
data class WttrInDayDto(
    @SerialName("date") val date: String = "",
    @SerialName("maxtempC") val maxTempC: String = "0",
    @SerialName("mintempC") val minTempC: String = "0",
    @SerialName("astronomy") val astronomy: List<WttrInAstronomyDto> = emptyList(),
    @SerialName("hourly") val hourly: List<WttrInHourlyDto> = emptyList()
)

@Serializable
data class WttrInAstronomyDto(
    @SerialName("sunrise") val sunrise: String = "",
    @SerialName("sunset") val sunset: String = ""
)

@Serializable
data class WttrInHourlyDto(
    @SerialName("time") val time: String? = null,
    @SerialName("tempC") val tempC: String = "0",
    @SerialName("weatherCode") val weatherCode: String = "113",
    @SerialName("chanceofrain") val chanceOfRain: String = "0"
)

@Serializable
data class WttrInTextDto(
    @SerialName("value") val value: String = ""
)
