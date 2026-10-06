package io.github.rojhatince47.weatherchan.feature.city.data.remote.overpass

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class OverpassResponseDto(
    @SerialName("elements") val elements: List<OverpassElementDto> = emptyList()
)

@Serializable
internal data class OverpassElementDto(
    @SerialName("type") val type: String? = null,
    @SerialName("lat") val lat: Double = 0.0,
    @SerialName("lon") val lon: Double = 0.0,
    @SerialName("tags") val tags: Map<String, String> = emptyMap()
)
