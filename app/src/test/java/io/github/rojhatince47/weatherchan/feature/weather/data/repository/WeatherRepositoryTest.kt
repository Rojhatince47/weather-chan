package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherRepositoryTest {

    @Test
    fun `returns full city list on success`() = runTest {
        val repository: WeatherRepository = WeatherRepositoryImpl(
            SampleWeatherDataSource(),
            DefaultErrorMapper()
        )

        val result = repository.cityWeathers()

        assertTrue(result is AppResult.Success)
        assertEquals(5, (result as AppResult.Success).data.size)
    }

    @Test
    fun `maps unknown city to not found failure`() = runTest {
        val repository: WeatherRepository = WeatherRepositoryImpl(
            SampleWeatherDataSource(),
            DefaultErrorMapper()
        )

        val result = repository.forecast(cityId = 999L)

        assertTrue(result is AppResult.Failure)
        assertEquals(AppError.NotFound, (result as AppResult.Failure).error)
    }
}
