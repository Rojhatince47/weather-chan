package io.github.rojhatince47.weatherchan.core.ui.state

import io.github.rojhatince47.weatherchan.core.ui.text.UiText

/** PDF'teki dört ekran durumu: yükleniyor / veri var / boş / hata. */
sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>

    data class Success<T>(
        val data: T
    ) : UiState<T>

    data object Empty : UiState<Nothing>

    data class Error(
        val message: UiText
    ) : UiState<Nothing>
}
