package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import io.github.rojhatince47.weatherchan.core.ui.text.UiText

sealed interface DetailEvent {
    data class ShowSnackbar(
        val message: UiText
    ) : DetailEvent
}
