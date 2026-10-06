package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class OpenMeteoWeatherDataSourceTest {

    private val server = MockWebServer()
    private lateinit var dataSource: OpenMeteoWeatherDataSource
    private lateinit var retrofit: Retrofit

    @Before
    fun setUp() {
        server.start()
        val json = Json { ignoreUnknownKeys = true }
        retrofit = Retrofit.Builder()
            .baseUrl(server.url("/"))
            .client(OkHttpClient())
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()
        dataSource = OpenMeteoWeatherDataSource(
            api = retrofit.create(OpenMeteoApi::class.java),
            geocodingApi = retrofit.create(OpenMeteoGeocodingApi::class.java),
            mapper = OpenMeteoWeatherMapper(),
            errorMapper = DefaultErrorMapper(),
            languageProvider = { "tr" }
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun `city weathers map temperature and condition`() = runTest {
        server.enqueue(MockResponse().setBody(FORECAST_JSON))

        val result = dataSource.cityWeathers(listOf(testCity()))

        assertTrue(result is AppResult.Success)
        val cityWeather = (result as AppResult.Success).data.first()
        assertEquals(19, cityWeather.currentWeather.temperatureCelsius)
        assertEquals(WeatherCondition.CLOUDY, cityWeather.currentWeather.condition)
    }

    @Test
    fun `forecast contains hourly and daily items`() = runTest {
        server.enqueue(MockResponse().setBody(FORECAST_JSON))

        val result = dataSource.forecast(testCity())

        assertTrue(result is AppResult.Success)
        val forecast = (result as AppResult.Success).data
        assertEquals(2, forecast.hourlyForecast.size)
        assertEquals(2, forecast.dailyForecast.size)
        assertEquals(0, forecast.hourlyForecast.first().hourOfDay)
    }

    @Test
    fun `search maps region and country`() = runTest {
        server.enqueue(MockResponse().setBody(GEOCODING_JSON))

        val result = dataSource.searchCities("İstanbul")

        assertTrue(result is AppResult.Success)
        val city = (result as AppResult.Success).data.first()
        assertEquals("İstanbul", city.name)
        assertEquals("İstanbul", city.region)
        assertEquals("Türkiye Cumhuriyeti", city.country)
    }

    @Test
    fun `search request goes through the configured base url with ten results`() = runTest {
        server.enqueue(MockResponse().setBody(GEOCODING_JSON))

        val result = dataSource.searchCities("İstanbul")

        assertTrue(result is AppResult.Success)
        val request = server.takeRequest(2, TimeUnit.SECONDS)
        assertNotNull("geocoding istegi sahte sunucuya gitmeli", request)
        val path = request!!.path!!
        assertEquals("/v1/search", path.substringBefore("?"))
        assertTrue(path.contains("count=10"))
    }

    @Test
    fun `dil saglayicisi geocoding sorgusuna tasinir`() = runTest {
        server.enqueue(MockResponse().setBody(GEOCODING_JSON))
        dataSource.searchCities("İstanbul")
        assertEquals(
            "tr",
            server.takeRequest(2, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("language")
        )

        server.enqueue(MockResponse().setBody(GEOCODING_JSON))
        val english = OpenMeteoWeatherDataSource(
            api = retrofit.create(OpenMeteoApi::class.java),
            geocodingApi = retrofit.create(OpenMeteoGeocodingApi::class.java),
            mapper = OpenMeteoWeatherMapper(),
            errorMapper = DefaultErrorMapper(),
            languageProvider = { "en" }
        )
        english.searchCities("İstanbul")
        assertEquals(
            "en",
            server.takeRequest(2, TimeUnit.SECONDS)!!.requestUrl!!.queryParameter("language")
        )
    }

    private fun testCity(name: String = "İstanbul", latitude: Double = 41.01, longitude: Double = 28.98): City = City(
        name = name,
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = latitude, longitude = longitude)
    )

    private val istanbul = testCity()
    private val ankara = testCity(name = "Ankara", latitude = 39.92, longitude = 32.85)

    /**
     * Yanıtı **koordinata göre** eşler.
     *
     * Kuyruk (`enqueue`) sıra tabanlıdır ama `batchAll` istekleri `async` ile
     * başlatır; gönderim sırası garanti değildir. İki şehir aynı koordinatı
     * taşıdığında istekler ayırt edilemez ve test rastgele başarısız olurdu.
     * Koordinata göre eşleme bu yarışı tamamen ortadan kaldırır.
     */
    private fun respondByLatitude(vararg rules: Pair<Double, MockResponse>) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                val rule = rules.firstOrNull { (latitude, _) -> path.contains("latitude=$latitude") }
                return rule?.second ?: MockResponse().setResponseCode(500)
            }
        }
    }

    // --- kısmi başarısızlık ---------------------------------------------------

    @Test
    fun `tek sehir basarisiz olsa bile listeyi bosaltmaz`() = runTest {
        respondByLatitude(
            ankara.coordinates.latitude to MockResponse().setBody(FORECAST_JSON)
        ) // İstanbul için kural yok -> 500

        val result = dataSource.cityWeathers(listOf(istanbul, ankara))

        assertTrue(
            "Bir şehir hata verse de diğeri başarılıysa liste boşalmamalı",
            result is AppResult.Success
        )
        assertEquals(1, (result as AppResult.Success).data.size)
        assertEquals("Ankara", result.data.single().city.name)
    }

    @Test
    fun `tum sehirler basarisizsa hata doner`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(503))

        val result = dataSource.cityWeathers(listOf(istanbul, ankara))

        assertTrue(
            "Hiçbir şehir gelemiyorsa hata yutulmamalı, kullanıcıya bildirilmeli",
            result is AppResult.Failure
        )
    }

    @Test
    fun `bos liste hataya dusmez`() = runTest {
        val result = dataSource.cityWeathers(emptyList())

        assertTrue(result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
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
                "time": ["2026-09-27T00:00", "2026-09-27T01:00"],
                "temperature_2m": [19.1, 18.7],
                "weather_code": [3, 61],
                "precipitation_probability": [20, 45]
              },
              "daily": {
                "time": ["2026-09-27", "2026-09-28"],
                "weather_code": [3, 95],
                "temperature_2m_max": [21.4, 20.1],
                "temperature_2m_min": [18.2, 17.5]
              }
            }
        """

        const val GEOCODING_JSON = """
            {
              "results": [
                {
                  "id": 745044,
                  "name": "İstanbul",
                  "latitude": 41.01384,
                  "longitude": 28.94966,
                  "country": "Türkiye Cumhuriyeti",
                  "admin1": "İstanbul"
                }
              ]
            }
        """
    }
}
