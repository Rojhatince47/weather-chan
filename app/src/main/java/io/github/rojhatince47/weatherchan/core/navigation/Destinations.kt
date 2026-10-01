package io.github.rojhatince47.weatherchan.core.navigation

import kotlinx.serialization.Serializable

@Serializable
data object ListDestination

@Serializable
data object AddCityDestination

@Serializable
data object SettingsDestination

@Serializable
data class ForecastDestination(
    val cityId: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val latitude: Double,
    val longitude: Double
)
