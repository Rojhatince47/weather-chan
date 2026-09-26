package io.github.rojhatince47.weatherchan.core.ui.state

import io.github.rojhatince47.weatherchan.core.ui.text.UiText

/** Yükleme işleminin durumu. İçerik state'inden türetilir, ayrıca saklanmaz. */
sealed interface LoadState {
    data object Idle : LoadState

    data object Loading : LoadState

    data object Success : LoadState

    data object Empty : LoadState

    data class Error(
        val message: UiText
    ) : LoadState
}

fun <T> UiState<T>.toLoadState(): LoadState = when (this) {
    is UiState.Loading -> LoadState.Loading
    is UiState.Success -> LoadState.Success
    is UiState.Empty -> LoadState.Empty
    is UiState.Error -> LoadState.Error(message)
}
