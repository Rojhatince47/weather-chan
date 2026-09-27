package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

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

class OpenMeteoCityWeathersConcurrencyTest {

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    @Test
    fun `sehir hava durumlari es zamanli baslatilir`() = runTest {
        val api = GatedOpenMeteoApi()
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
        val dataSource = dataSource(GatedOpenMeteoApi(openGate = true))
        val cities = List(5) { testCity(it) }

        val result = dataSource.cityWeathers(cities)

        assertTrue(result is AppResult.Success)
        val data = (result as AppResult.Success).data
        assertEquals(5, data.size)
        assertEquals(cities.map { it.id }, data.map { it.city.id })
    }

    private fun dataSource(api: OpenMeteoApi) = OpenMeteoWeatherDataSource(
        api = api,
        geocodingApi = StubGeocodingApi(),
        mapper = OpenMeteoWeatherMapper(),
        errorMapper = DefaultErrorMapper()
    )

    private fun testCity(index: Int) = City(
        name = "Şehir$index",
        region = "Bölge",
        country = "Ülke",
        coordinates = Coordinates(latitude = 40.0 + index, longitude = 30.0 + index)
    )

    private class StubGeocodingApi : OpenMeteoGeocodingApi {
        override suspend fun search(name: String, count: Int, language: String, format: String): OpenMeteoGeocodingDto =
            OpenMeteoGeocodingDto(results = emptyList())
    }

    private class GatedOpenMeteoApi(openGate: Boolean = false) : OpenMeteoApi {
        val gate = CompletableDeferred<Unit>().apply { if (openGate) complete(Unit) }
        var callCount = 0
            private set

        override suspend fun forecast(
            latitude: String,
            longitude: String,
            current: String,
            hourly: String,
            daily: String,
            timezone: String,
            forecastDays: Int
        ): OpenMeteoForecastDto {
            callCount++
            gate.await()
            return OpenMeteoForecastDto(
                current = OpenMeteoCurrentDto(temperature = 19.4, humidity = 70, weatherCode = 3),
                hourly = OpenMeteoHourlyDto(
                    time = listOf("2026-09-27T00:00"),
                    temperature = listOf(19.1),
                    weatherCode = listOf(3),
                    precipitationProbability = listOf(20)
                ),
                daily = OpenMeteoDailyDto(
                    time = listOf("2026-09-27"),
                    weatherCode = listOf(3),
                    temperatureMax = listOf(21.4),
                    temperatureMin = listOf(18.2)
                )
            )
        }
    }
}
