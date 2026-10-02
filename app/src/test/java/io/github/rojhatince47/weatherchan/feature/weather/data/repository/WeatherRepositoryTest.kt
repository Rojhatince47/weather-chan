package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.network.NetworkErrorMapper
import io.github.rojhatince47.weatherchan.feature.weather.data.local.WeatherCachePreferences
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoGeocodingApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherMapper
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInApi
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherMapper
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import java.io.File
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class WeatherRepositoryTest {

    private val primaryServer = MockWebServer()
    private val backupServer = MockWebServer()
    private lateinit var repository: WeatherRepository

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private lateinit var cacheFile: File
    private val fixedNow = Instant.parse("2026-10-02T10:00:00Z")

    /** Eski sabit 5 şehirlik listede OLMAYAN bir şehir (Bursa). */
    private fun dynamicCity() = City(
        name = "Bursa",
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 40.19, longitude = 29.06)
    )

    private fun testCities(count: Int): List<City> = List(count) { index ->
        City(
            name = "Şehir $index",
            region = null,
            country = "Türkiye",
            coordinates = Coordinates(latitude = 40.0 + index / 10.0, longitude = 29.0)
        )
    }

    @Before
    fun setUp() {
        primaryServer.start()
        backupServer.start()
        cacheFile = File(temporaryFolder.root, "weather_cache_test.preferences_pb")
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
            cache = WeatherCachePreferences(createCacheDataStore()),
            clock = Clock.fixed(fixedNow, ZoneOffset.UTC)
        )
    }

    private fun createCacheDataStore(): DataStore<Preferences> = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(UnconfinedTestDispatcher() + Job())
    ) { cacheFile }

    @After
    fun tearDown() {
        primaryServer.shutdown()
        backupServer.shutdown()
    }

    @Test
    fun `primary success returns primary data without calling backup`() = runTest {
        val cities = testCities(5)
        repeat(5) {
            primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))
        }

        val result = repository.cityWeathers(cities)

        assertTrue(result is AppResult.Success)
        assertEquals(5, (result as AppResult.Success).data.size)
        assertEquals(0, backupServer.requestCount)
    }

    @Test
    fun `primary failure falls back to backup`() = runTest {
        val cities = testCities(5)
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        repeat(5) {
            backupServer.enqueue(MockResponse().setBody(WTTR_JSON))
        }

        val result = repository.cityWeathers(cities)

        assertTrue(result is AppResult.Success)
        assertEquals(
            19,
            (result as AppResult.Success).data.first().currentWeather.temperatureCelsius
        )
    }

    @Test
    fun `search returns geocoding results via primary`() = runTest {
        primaryServer.enqueue(MockResponse().setBody(GEOCODING_JSON))

        val result = repository.search("İstanbul")

        assertTrue(result is AppResult.Success)
        val cities = (result as AppResult.Success).data
        assertEquals(1, cities.size)
        assertEquals("İstanbul", cities.first().name)
        assertEquals(0, backupServer.requestCount)
    }

    @Test
    fun `search falls back to backup when primary fails`() = runTest {
        primaryServer.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        backupServer.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = repository.search("İstanbul")

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.size)
    }

    /**
     * Birincil kaynak **başarılı olup boş liste** döndürdüğünde yedeğe düşülür.
     *
     * Konum adı çözümlemesi buna bağlıdır: sorgu `"41.01,28.98"` biçiminde
     * bir koordinattır ve Open-Meteo geocoding'i *ad* arar, koordinat değil.
     * Yani birincil kaymak HTTP 200 ve boş döner. Yalnız `Failure` durumunda
     * yedeğe düşülsaydı wttr.in hiç çağrılmazdı ve konum kartı sürekli
     * "Konum" adını gösterirdi — ağ sağlıklı olsa bile.
     */
    @Test
    fun `search falls back to backup when primary returns empty result`() = runTest {
        primaryServer.enqueue(MockResponse().setBody(EMPTY_GEOCODING_JSON))
        backupServer.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = repository.search("41.0100,28.9800")

        assertTrue("Boş sonuçta yedeğe düşülmeli", result is AppResult.Success)
        val cities = (result as AppResult.Success).data
        assertEquals(1, cities.size)
        assertEquals(1, backupServer.requestCount)
    }

    @Test
    fun `both sources empty returns empty success`() = runTest {
        primaryServer.enqueue(MockResponse().setBody(EMPTY_GEOCODING_JSON))
        backupServer.enqueue(MockResponse().setResponseCode(500).setBody("{}"))

        val result = repository.search("Bilinmeyen Yer")

        assertTrue("İki kaynak da bulamazsa hata değil boş sonuç döner", result is AppResult.Success)
        assertTrue((result as AppResult.Success).data.isEmpty())
    }

    @Test
    fun `both sources failing returns failure`() = runTest {
        val cities = testCities(5)
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        repeat(5) {
            backupServer.enqueue(MockResponse().setResponseCode(500))
        }

        val result = repository.cityWeathers(cities)

        assertTrue(result is AppResult.Failure)
    }

    @Test
    fun `cityWeathers verilen listenin tamamina hava durumu doner`() = runTest {
        val cities = testCities(2)
        repeat(2) {
            primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))
        }

        val result = repository.cityWeathers(cities)

        assertEquals(2, (result as AppResult.Success).data.size)
        assertEquals(
            cities.map { it.id }.toSet(),
            result.data.map { it.city.id }.toSet()
        )
    }

    @Test
    fun `listeye eklenmis dinamik sehir icin tahmin doner`() = runTest {
        // Regresyon: forecast(cityId) şehri sabit listede arıyordu; eklenen her
        // şehir NotFound alıyordu. Artık City doğrudan alınıyor.
        primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))

        val result = repository.forecast(city = dynamicCity())

        assertTrue("Sonuç başarılı olmalıydı, $result", result is AppResult.Success)
    }

    @Test
    fun `forecast falls back to backup`() = runTest {
        primaryServer.enqueue(MockResponse().setResponseCode(500))
        backupServer.enqueue(MockResponse().setBody(WTTR_JSON))

        val result = repository.forecast(city = dynamicCity())

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.hourlyForecast.size)
    }

    @Test
    fun `basarili tur onbellege yazar`() = runTest {
        val cities = testCities(2)
        repeat(2) {
            primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))
        }

        val result = repository.cityWeathers(cities)

        assertTrue(result is AppResult.Success)
        val cached = repository.cachedWeathers().first()
        assertEquals(
            cities.map { it.id }.toSet(),
            cached!!.weathers.map { it.city.id }.toSet()
        )
        assertEquals(fixedNow, cached.fetchedAt)
    }

    @Test
    fun `basarisiz tur yazmaz`() = runTest {
        repeat(2) {
            primaryServer.enqueue(MockResponse().setResponseCode(500))
            backupServer.enqueue(MockResponse().setResponseCode(500))
        }

        assertTrue(repository.cityWeathers(testCities(2)) is AppResult.Failure)
        assertNull(repository.cachedWeathers().first())
    }

    @Test
    fun `kismi tur yazmaz`() = runTest {
        // İki paralel istekten biri 500 alır: `batchAll` kalanı `Success(eksik
        // liste)` döner. Hangi şehrin düştüğü belirsizdir (paralel yarış) ama
        // sonuç her durumda eksiktir → yazma atlanır.
        primaryServer.enqueue(MockResponse().setBody(FORECAST_JSON))
        primaryServer.enqueue(MockResponse().setResponseCode(500))

        val result = repository.cityWeathers(testCities(2))

        assertTrue(result is AppResult.Success)
        assertEquals(1, (result as AppResult.Success).data.size)
        assertNull(repository.cachedWeathers().first())
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
        /** Birincil kaynağın koordinat sorgusuna verdiği tipik cevap: 200, boş. */
        const val EMPTY_GEOCODING_JSON = """{"results":[]}"""

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
