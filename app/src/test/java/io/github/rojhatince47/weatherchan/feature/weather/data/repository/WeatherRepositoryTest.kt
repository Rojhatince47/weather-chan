package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.network.NetworkErrorMapper
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoGeocodingApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherMapper
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherMapper
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class WeatherRepositoryTest {

    private val primaryServer = MockWebServer()
    private val backupServer = MockWebServer()
    private lateinit var repository: WeatherRepository

    @Before
    fun setUp() {
        primaryServer.start()
        backupServer.start()
        val json = Json { ignoreUnknownKeys = true }
        val primaryRetrofit = retrofit(primaryServer)
        val backupRetrofit = retrofit(backupServer)
        val errorMapper = NetworkErrorMapper()
        repository = WeatherRepositoryImpl(
            primary = OpenMeteoWeatherDataSource(
                api = primaryRetrofit.create(OpenMeteoApi::class.java),
                geocodingApi = primaryRetrofit.create(OpenMeteoGeocodingApi::class.java),
                mapper = OpenMeteoWeatherMapper(),
                errorMapper = errorMapper
            ),
            backup = WttrInWeatherDataSource(
                api = backupRetrofit.create(WttrInApi::class.java),
                mapper = WttrInWeatherMapper(),
                errorMapper = errorMapper
            ),
            sampleDataSource = SampleWeatherDataSource()
        )
    }

    @After
    fun tearDown() {
        primaryServer.shutdown()
        backupServer.shutdown()
    }

    @Test
    fun `primary success returns primary data without calling backup`() = runTest {
        repeat(5) {
            primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))
        }

        val result = repository.cityWeathers()

        assertTrue(result is AppResult.Success)
        assertEquals(5, (result as AppResult.Success).data.size)
        assertEquals(0, backupServer.requestCount)
    }

    @Test
    fun `primary failure falls back to backup`() = runTest {
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        repeat(5) {
            backupServer.enqueue(MockResponse().setBody(WTTR_JSON))
        }

        val result = repository.cityWeathers()

        assertTrue(result is AppResult.Success)
        assertEquals(
            19,
            (result as AppResult.Success).data.first().currentWeather.temperatureCelsius
        )
    }

    @Test
    fun `both sources failing returns failure`() = runTest {
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        repeat(5) {
            backupServer.enqueue(MockResponse().setResponseCode(500))
        }

        val result = repository.cityWeathers()

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `forecast falls back to backup`() = runTest {
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        backupServer.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = repository.forecast(cityId = 1L)

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.hourlyForecast.size)
    }

    private fun retrofit(server: MockWebServer): Retrofit {
        val json = Json { ignoreUnknownKeys = true }
        return Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
    }

    private companion object {
        const val FORECAST_JSON = """
            {
              "current": {
                "temperature_2m": 19.4,
                "relative_humidity_2m": 70,
                "weather_code": 3,
                "wind_speed_10m": 15.2
              },
              "hourly": {
                "time": ["2026-09-27T00:00"],
                "temperature_2m": [19.1],
                "weather_code": [3],
                "precipitation_probability": [20]
              },
              "daily": {
                "time": ["2026-09-27"],
                "weather_code": [3],
                "temperature_2m_max": [21.4],
                "temperature_2m_min": [18.2]
              }
            }
        """

        const val WTTR_JSON = """
            {
              "current_condition": [
                {
                  "temp_C": "19",
                  "humidity": "70",
                  "windspeedKmph": "26",
                  "weatherCode": "176",
                  "weatherDesc": [{ "value": "Patchy rain nearby" }]
                }
              ],
              "nearest_area": [
                {
                  "areaName": [{ "value": "Findikli" }],
                  "country": [{ "value": "Turkey" }],
                  "region": [{ "value": "Istanbul" }],
                  "latitude": "41.033",
                  "longitude": "29.000"
                }
              ],
              "weather": [
                {
                  "date": "2026-09-27",
                  "maxtempC": "21",
                  "mintempC": "18",
                  "hourly": [
                    {
                      "time": "0",
                      "tempC": "19",
                      "weatherCode": "176",
                      "chanceofrain": "21"
                    }
                  ]
                }
              ]
            }
        """
    }
}
