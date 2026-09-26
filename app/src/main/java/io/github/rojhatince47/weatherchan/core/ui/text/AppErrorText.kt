package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.core.common.error.AppError

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Dynamic("İnternet bağlantısı yok")
    is AppError.Server -> UiText.Dynamic("Sunucu hatası ($code)")
    AppError.NotFound -> UiText.Dynamic("Sonuç bulunamadı")
    AppError.Parse -> UiText.Dynamic("Veri okunamadı")
    is AppError.Unknown -> UiText.Dynamic("Beklenmeyen bir hata oluştu")
}
