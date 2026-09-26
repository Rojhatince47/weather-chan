package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import io.github.rojhatince47.weatherchan.core.ui.text.UiText

sealed interface MainEvent {
    data class ShowSnackbar(
        val message: UiText
    ) : MainEvent
}
