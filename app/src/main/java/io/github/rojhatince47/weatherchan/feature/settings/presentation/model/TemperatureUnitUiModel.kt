package io.github.rojhatince47.weatherchan.feature.settings.presentation.model

import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit

data class TemperatureUnitUiModel(
    val unit: TemperatureUnit,
    val label: UiText
)
