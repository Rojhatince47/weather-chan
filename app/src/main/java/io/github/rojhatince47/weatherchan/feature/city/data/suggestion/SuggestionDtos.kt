package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class SuggestionFileDto(
    @SerialName("version") val version: Int = 0,
    @SerialName("worldFallbackOrder") val worldFallbackOrder: List<String> = emptyList(),
    @SerialName("cities") val cities: List<CityDto> = emptyList()
)

@Serializable
internal data class CityDto(
    @SerialName("name") val name: String = "",
    @SerialName("code") val code: String = "",
    @SerialName("country") val country: String = "",
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0
)