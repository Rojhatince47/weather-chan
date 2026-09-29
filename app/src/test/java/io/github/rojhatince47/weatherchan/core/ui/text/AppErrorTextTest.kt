package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.ui.text.UiText.Resource
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorTextTest {

    private fun textOf(error: AppError): UiText = error.toUiText()

    @Test
    fun `hiz siniri ayri bir mesaj alir`() {
        // 429 "İnternet bağlantısı yok" gibi gösterilmemeli: bağlantı vardır,
        // sağlayıcı isteği geçici olarak reddetmiştir.
        assertEquals(Resource(R.string.error_rate_limited), textOf(AppError.Server(429)))
    }

    @Test
    fun `sunucu hatasi 5xx icin genel mesaj kullanir`() {
        assertEquals(Resource(R.string.error_server_unavailable), textOf(AppError.Server(503)))
        assertEquals(Resource(R.string.error_server_unavailable), textOf(AppError.Server(500)))
    }

    @Test
    fun `diger sunucu kodlari koduyla gosterilir`() {
        assertEquals(Resource(R.string.error_server, 400), textOf(AppError.Server(400)))
    }

    @Test
    fun `ag hatasi yalnizca baglanti yok anlamina gelmez`() {
        // Zaman aşımı da bu sınıfa girer; "bağlantı yok" derse kullanıcı yanlış
        // teşhis yapar. Mesaj iki olasılığı da kapsamalı.
        assertEquals(Resource(R.string.error_network), textOf(AppError.Network))
    }
}
