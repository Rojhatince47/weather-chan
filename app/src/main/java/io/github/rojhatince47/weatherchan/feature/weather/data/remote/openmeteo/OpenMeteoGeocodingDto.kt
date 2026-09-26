package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OpenMeteoGeocodingDto(
    @SerialName("results") val results: List<OpenMeteoGeocodingResultDto> = emptyList()
)

@Serializable
data class OpenMeteoGeocodingResultDto(
    @SerialName("id") val id: Long = 0L,
    @SerialName("name") val name: String = "",
    @SerialName("latitude") val latitude: Double = 0.0,
    @SerialName("longitude") val longitude: Double = 0.0,
    @SerialName("country") val country: String? = null,
    @SerialName("admin1") val region: String? = null
)
