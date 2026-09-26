package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText

data class CityWeatherUiModel(
    val cityId: Long,
    val name: String,
    val region: String?,
    val country: String?,
    val temperatureText: String,
    val conditionEmoji: String,
    val conditionLabel: UiText,
    val isFavorite: Boolean
)
