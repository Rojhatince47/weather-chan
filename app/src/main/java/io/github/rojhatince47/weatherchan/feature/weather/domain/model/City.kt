package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class City(
    val id: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val coordinates: Coordinates
)
