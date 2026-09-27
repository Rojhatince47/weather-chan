package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError

fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Resource(R.string.error_network)
    is AppError.Server -> UiText.Resource(R.string.error_server, code)
    AppError.NotFound -> UiText.Resource(R.string.error_not_found)
    AppError.Parse -> UiText.Resource(R.string.error_parse)
    is AppError.Unknown -> UiText.Resource(R.string.error_unexpected)
}
