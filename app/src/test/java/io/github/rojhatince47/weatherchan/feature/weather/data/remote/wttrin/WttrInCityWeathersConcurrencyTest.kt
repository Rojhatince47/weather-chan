package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WttrInCityWeathersConcurrencyTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `sehir hava durumlari es zamanli baslatilir`() = runTest {
        val api = GatedWttrInApi()
        val dataSource = dataSource(api)
        val cities = List(5) { testCity(it) }

        val job = launch { dataSource.cityWeathers(cities) }
        runCurrent()

        assertEquals(5, api.callCount)

        api.gate.complete(Unit)
        job.join()
    }

    @Test
    fun `es zamanli cekim tum sehirleri girdi sirasiyla dondurur`() = runTest {
        val dataSource = dataSource(GatedWttrInApi(openGate = true))
        val cities = List(5) { testCity(it) }

        val result = dataSource.cityWeathers(cities)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(5, data.size)
        assertEquals(cities.map { it.id }, data.map { it.city.id })
    }

    private fun dataSource(api: WttrInApi) = WttrInWeatherDataSource(
        api = api,
        mapper = WttrInWeatherMapper(),
        errorMapper = DefaultErrorMapper()
    )

    private fun testCity(index: Int) = City(
        name = "Şehir$index",
        region = "Bölge",
        country = "Ülke",
        coordinates = Coordinates(latitude = 40.0 + index, longitude = 30.0 + index)
    )

    private class GatedWttrInApi(openGate: Boolean = false) : WttrInApi {
        val gate = CompletableDeferred<Unit>().apply { if (openGate) complete(Unit) }
        var callCount = 0
            private set

        override suspend fun weather(location: String): WttrInDto {
            callCount++
            gate.await()
            return WttrInDto(
                currentCondition = listOf(WttrInCurrentDto(tempC = "19", weatherCode = "500"))
            )
        }
    }
}
