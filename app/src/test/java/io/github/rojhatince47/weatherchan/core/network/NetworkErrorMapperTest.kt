package io.github.rojhatince47.weatherchan.core.network

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import java.io.IOException
import kotlinx.serialization.SerializationException
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

class NetworkErrorMapperTest {
    private val mapper = NetworkErrorMapper()

    @Test
    fun `io exception maps to network error`() {
        assertEquals(AppError.Network, mapper.map(IOException("no route")))
    }

    @Test
    fun `http error maps to server error with code`() {
        val exception = HttpException(
            Response.error<Any>(503, "".toResponseBody())
        )

        assertEquals(AppError.Server(503), mapper.map(exception))
    }

    @Test
    fun `serialization error maps to parse error`() {
        assertEquals(AppError.Parse, mapper.map(SerializationException("bad json")))
    }

    @Test
    fun `unknown error maps to unknown`() {
        val cause = IllegalStateException("boom")

        assertTrue(mapper.map(cause) is AppError.Unknown)
    }
}
