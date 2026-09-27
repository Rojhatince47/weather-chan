package io.github.rojhatince47.weatherchan.core.ui.text

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import org.junit.Assert.assertEquals
import org.junit.Test

class AppErrorTextTest {

    @Test
    fun `network error maps to a string resource`() {
        assertEquals(
            UiText.Resource(R.string.error_network),
            AppError.Network.toUiText()
        )
    }

    @Test
    fun `server error carries its status code as an argument`() {
        assertEquals(
            UiText.Resource(R.string.error_server, 503),
            AppError.Server(503).toUiText()
        )
    }

    @Test
    fun `not found has its own resource and is not the empty list state`() {
        val text = AppError.NotFound.toUiText()
        assertEquals(UiText.Resource(R.string.error_not_found), text)
        // Boş liste durumu ile "bulunamadı" hatası kavramsal olarak farklıdır;
        // birbirinin yerine kullanılmamalıdır.
        assertEquals(false, text == UiText.Resource(R.string.state_empty_list))
    }

    @Test
    fun `parse error maps to a string resource`() {
        assertEquals(
            UiText.Resource(R.string.error_parse),
            AppError.Parse.toUiText()
        )
    }

    @Test
    fun `unknown error maps to a string resource`() {
        assertEquals(
            UiText.Resource(R.string.error_unexpected),
            AppError.Unknown(RuntimeException("x")).toUiText()
        )
    }

    @Test
    fun `every app error maps to a resource, never to a hardcoded literal`() {
        val errors = listOf(
            AppError.Network,
            AppError.Server(500),
            AppError.NotFound,
            AppError.Parse,
            AppError.Unknown(RuntimeException("x"))
        )
        errors.forEach { error ->
            assertEquals(
                "$error gomulu metin uretiyor",
                true,
                error.toUiText() is UiText.Resource
            )
        }
    }
}
