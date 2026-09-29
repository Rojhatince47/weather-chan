package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError

/**
 * Teknik hatayı kullanıcıya gösterilecek metne çevirir.
 *
 * `AppError.Network` **yalnızca "bağlantı yok" anlamına gelmez**: zaman aşımı
 * ve bağlantı kesilmesi de bu sınıfa girer. Bu ayrım önemlidir, çünkü sağlayıcı
 * hız sınırı uyguladığında istekler zaman aşımına uğrar ve kullanıcı yanlış
 * teşhisle ("internetim yok") ağ sorunu sandığında aslında bir geçici sunucu
 * sınırı vardır.
 */
fun AppError.toUiText(): UiText = when (this) {
    AppError.Network -> UiText.Resource(R.string.error_network)
    is AppError.Server -> when (code) {
        429 -> UiText.Resource(R.string.error_rate_limited)
        in 500..599 -> UiText.Resource(R.string.error_server_unavailable)
        else -> UiText.Resource(R.string.error_server, code)
    }
    AppError.NotFound -> UiText.Resource(R.string.error_not_found)
    AppError.Parse -> UiText.Resource(R.string.error_parse)
    is AppError.Unknown -> UiText.Resource(R.string.error_unexpected)
}
