package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Dynamic("İnternet bağlantısı yok")
    is AppError.Server -> UiText.Dynamic("Sunucu hatası ($code)")
    AppError.NotFound -> UiText.Resource(R.string.state_empty_list)
    AppError.Parse -> UiText.Dynamic("Veri okunamadı")
    is AppError.Unknown -> UiText.Resource(R.string.error_unexpected)
}
