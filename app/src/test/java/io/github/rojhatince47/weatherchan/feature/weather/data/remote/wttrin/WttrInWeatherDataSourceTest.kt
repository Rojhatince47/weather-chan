package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
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

class WttrInWeatherDataSourceTest {

    private val server = MockWebServer()
    private lateinit var dataSource: WttrInWeatherDataSource

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        val retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        dataSource = WttrInWeatherDataSource(
            api = retrofit.create(WttrInApi::class.java),
            mapper = WttrInWeatherMapper(),
            errorMapper = DefaultErrorMapper()
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `city weathers map temperature and condition`() = runTest {
        server.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = dataSource.cityWeathers(listOf(testCity()))

        assertTrue(result is AppResult.Success)
        val cityWeather = (result as AppResult.Success).data.first()
        assertEquals(19, cityWeather.currentWeather.temperatureCelsius)
        assertEquals(WeatherCondition.RAIN, cityWeather.currentWeather.condition)
    }

    @Test
    fun `forecast takes hourly slices and daily items`() = runTest {
        server.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = dataSource.forecast(testCity())

        assertTrue(result is AppResult.Success)
        val forecast = (result as AppResult.Success).data
        assertEquals(2, forecast.hourlyForecast.size)
        assertEquals(0, forecast.hourlyForecast.first().hourOfDay)
        assertEquals(1, forecast.dailyForecast.size)
        assertEquals(18, forecast.dailyForecast.first().minTemperatureCelsius)
        assertEquals(21, forecast.dailyForecast.first().maxTemperatureCelsius)
    }

    @Test
    fun `search resolves single city`() = runTest {
        server.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = dataSource.searchCities("İstanbul")

        assertTrue(result is AppResult.Success)
        val city = (result as AppResult.Success).data.first()
        assertEquals("Findikli", city.name)
        assertEquals("Turkey", city.country)
    }

    private fun testCity(): City = City(
        name = "İstanbul",
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 41.01, longitude = 28.98)
    )

    private companion object {
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
                    },
                    {
                      "time": "300",
                      "tempC": "20",
                      "weatherCode": "122",
                      "chanceofrain": "10"
                    }
                  ]
                }
              ]
            }
        """
    }
}
