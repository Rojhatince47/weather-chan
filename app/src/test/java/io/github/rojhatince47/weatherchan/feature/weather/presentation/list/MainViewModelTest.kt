package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedForecastsUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    /**
     * City kimliği artık koordinattan türetiyor; testler sabit 1L/2L değil
     * üretim kodundaki aynı City'yi okuyarak id alır.
     */
    private val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))

    private fun TestScope.mainViewModel(
        fakeWeather: FakeWeatherRepository,
        fakeCities: CityRepository = FakeCityRepository(),
        clock: Clock = Clock.systemDefaultZone()
    ) = MainViewModel(
        getCityWeathers = GetCityWeathersUseCase(fakeWeather),
        getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
        uiMapper = WeatherUiMapper(),
        unitRepository = FakeTemperatureUnitRepository(),
        cityRepository = fakeCities,
        clock = clock
    )

    private val fixedNow = Instant.parse("2026-10-02T10:00:00Z")

    private fun fixedClockAt(base: Instant = fixedNow) = Clock.fixed(base, ZoneOffset.UTC)

    private fun cachedWeather(city: City) = CityWeather(
        city = city,
        currentWeather = CurrentWeather(
            temperatureCelsius = 21,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        )
    )

    private fun forecastOf(city: City) = Forecast(
        city = city,
        currentWeather = cachedWeather(city).currentWeather,
        hourlyForecast = emptyList(),
        dailyForecast = emptyList()
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is success with cities`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            getCachedForecasts = GetCachedForecastsUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Success)
            assertEquals(
                5,
                (state.content as UiState.Success).data.size
            )
        }
    }

    /**
     * Regresyon (cihazda gözlendi): İzmir eklendi, isteği 429'a düştü ve şehir
     * listeden kayboldu. Kayıt diskte duruyordu; kullanıcı "eklediğim şehir
     * silindi" izlenimi edindi. Yüklenemeyen şehir listede kalmalı.
     */
    @Test
    fun `yuklenemeyen sehir hata karti olarak listede kalir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(ankara.id)
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul, ankara))
        )

        viewModel.uiState.test {
            val state = awaitItem()
            val items = (state.content as UiState.Success).data
            assertEquals(2, items.size)
            assertTrue(items.first { it.cityId == istanbul.id }.weather is CardWeather.Loaded)
            assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Unavailable)
        }
    }

    @Test
    fun `yuksuz sehirler de konum siralamasini bozmaz`() = runTest {
        val location = istanbul.copy(isLocation = true)
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(location.id)
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(location, ankara))
        )

        viewModel.uiState.test {
            val items = (awaitItem().content as UiState.Success).data
            assertEquals(
                "Konum kaydı listenin başında kalmalı; hava durumu yüklensin ya da yüklenmesin",
                listOf(location.id, ankara.id),
                items.map { it.cityId }
            )
            assertTrue(items.first().weather is CardWeather.Unavailable)
        }
    }

    /**
     * Hata kartı yalnız **kısmi** başarısızlık içindir. Hiçbir şehir
     * yüklenemezse batchAll Failure döner ve akış tam ekran hataya gider;
     * kullanıcıya nedeni ve tek "Tekrar dene" eylemi doğru olanıdır.
     */
    @Test
    fun `hicbir sehir yuklenemezse tam ekran hata gosterilir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            failCityWeathers = true
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul))
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Error)
        }
    }

    /**
     * Yeni eklenen şehir, verisi gelene kadar "alınamadı" değil "yükleniyor"
     * göstermelidir: ağ turu (~sn mertebesi) bitmeden hata metni, yanlış bir
     * başarısızlık sinyalidir. Kapı deseni: `autoCompleteGates = false` iken
     * `cityWeathers` kapıda bekler, tur bitmeden ara durum gözlenir.
     */
    @Test
    fun `yeni eklenen sehir verisi gelene kadar yukleniyor gosterir`() = runTest {
        val fakeWeather = FakeWeatherRepository()
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        val viewModel = mainViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)

        runCurrent()
        // İlk tur kapıda: İstanbul'un verisi henüz yok → yükleniyor kümesinde.
        assertEquals(setOf(istanbul.id), viewModel.uiState.value.loadingCityIds)

        fakeWeather.completeAllCityWeathersGates()
        runCurrent()
        // İlk tur bitti: küme boşaldı, kart yüklendi.
        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)

        // Yeni şehir eklendi; ikinci tur kapıda → yalnız yenisi kümede.
        fakeCities.add(ankara)
        runCurrent()
        assertEquals(setOf(ankara.id), viewModel.uiState.value.loadingCityIds)

        fakeWeather.completeAllCityWeathersGates()
        runCurrent()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Loaded)
    }

    /**
     * Tur başarısız biterse yükleme kümesi boşalır: spinner durur, kart
     * gerçekten hata metnine döner ve "Tekrar dene" geri gelir. Küme
     * boşalmazsa gösterge sonsuza dek döner, düğme hiç dönmez.
     */
    @Test
    fun `ekleme sonrasi tur basarisiz olunca spinner durur ve hata metni doner`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        val viewModel = mainViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)
        runCurrent()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)

        // Ağ bozuldu, yeni şehir eklendi → ikinci tur başarısız bitecek.
        fakeWeather.failCityWeathers = true
        fakeCities.add(ankara)
        runCurrent()

        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.first { it.cityId == istanbul.id }.weather is CardWeather.Loaded)
        assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Unavailable)
    }

    /**
     * Kısmi başarıda gelmeyen şehir de kümeden düşer: İstanbul yüklenir,
     * Ankara hata metnine döner; ikisi de takılı kalmaz.
     */
    @Test
    fun `kismi basarida gelmeyen sehir yukleme kumesinden duser`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(ankara.id)
        }
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        val viewModel = mainViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)
        runCurrent()

        fakeCities.add(ankara)
        runCurrent()

        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.first { it.cityId == istanbul.id }.weather is CardWeather.Loaded)
        assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Unavailable)
    }

    /**
     * İptal edilen tur, kendinden sonra başlayan turun kümesini silmez:
     * birinci tur kapıda beklerken ekleme olursa ikinci tur başlar ve
     * birinciyi iptal eder; birinci turun `finally`'si ikinci turun
     * kimliklerine dokunmamalıdır (nesil koruması).
     */
    @Test
    fun `iptal edilen tur yeni turun kumesini silmez`() = runTest {
        val fakeWeather = FakeWeatherRepository()
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        val viewModel = mainViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)
        runCurrent()
        assertEquals(setOf(istanbul.id), viewModel.uiState.value.loadingCityIds)

        fakeCities.add(ankara)
        runCurrent()
        assertEquals(setOf(istanbul.id, ankara.id), viewModel.uiState.value.loadingCityIds)

        fakeWeather.completeAllCityWeathersGates()
        runCurrent()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)
    }

    @Test
    fun `taze onbellekle acilista aga gidilmez`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )

        runCurrent()
        // Ağ turu YOK ama kart anında yüklü.
        assertEquals(0, fakeWeather.cityWeathersRequests.size)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.single().weather is CardWeather.Loaded)
        assertEquals("21°C", (items.single().weather as CardWeather.Loaded).temperatureText)
    }

    @Test
    fun `bayat onbellekte arka planda tazelenir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(31 * 60))
        )

        runCurrent()
        // Önce eski veri görünür, tur sessizce başlar.
        assertEquals(1, fakeWeather.cityWeathersRequests.size)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.single().weather is CardWeather.Loaded)
    }

    @Test
    fun `yeni sehir her zaman cekilir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = fakeCities,
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )

        runCurrent()
        assertEquals(0, fakeWeather.cityWeathersRequests.size)
        fakeCities.add(ankara)
        runCurrent()
        assertEquals(1, fakeWeather.cityWeathersRequests.size)
    }

    @Test
    fun `cek-yenile tazelige bakmaz`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )

        runCurrent()
        assertEquals(0, fakeWeather.cityWeathersRequests.size)
        viewModel.onRefresh()
        runCurrent()
        assertEquals(1, fakeWeather.cityWeathersRequests.size)
    }

    @Test
    fun `konum tohumu sentinel anahtardan gelir`() = runTest {
        val location = istanbul.copy(isLocation = true)
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            cachedForecasts = mapOf(CachedForecast.LOCATION_KEY to CachedForecast(fixedNow, forecastOf(location)))
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(location)),
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )

        runCurrent()
        assertEquals(0, fakeWeather.cityWeathersRequests.size)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertEquals(1, items.size)
        assertTrue(items.single().weather is CardWeather.Loaded)
    }

    @Test
    fun `tohumda olmayan sehir spinner gosterir`() = runTest {
        // Önbellekten gelen kartlar `Loaded`, eksik şehir spinner: `loadingCityIds`
        // mekanizması tohumla da çalışır (Task 9'da kilitli davranış, burada doğrulanır).
        val fakeWeather = FakeWeatherRepository().apply {
            cachedForecasts = mapOf(istanbul.id to CachedForecast(fixedNow, forecastOf(istanbul)))
        }
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = fakeCities,
            clock = fixedClockAt(fixedNow.plusSeconds(10 * 60))
        )
        runCurrent()
        assertEquals(0, fakeWeather.cityWeathersRequests.size)
        fakeCities.add(ankara)
        runCurrent()
        // Yeni tur kapıda: Ankara kümede, kartı Unavailable (gösterge yolu).
        assertEquals(setOf(ankara.id), viewModel.uiState.value.loadingCityIds)
        val items = (viewModel.uiState.value.content as UiState.Success).data
        assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Unavailable)
        fakeWeather.completeAllCityWeathersGates()
        runCurrent()
        assertEquals(emptySet<Long>(), viewModel.uiState.value.loadingCityIds)
    }

    @Test
    fun `tekrar deneme basarili olunca kart yuklenmis olur`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(ankara.id)
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul, ankara))
        )

        // Ağ düzeldi: aynı istek artık ankara'yı da döndürüyor.
        fakeWeather.omitCities = emptySet()
        viewModel.onRetryCity(ankara.id)

        viewModel.uiState.test {
            val state = awaitItem()
            val items = (state.content as UiState.Success).data
            // Ankara yüklendi VE İstanbul korunmalı: merge'in tanımlayıcı
            // özelliği budur. Önbellek ezilip yalnız ankara yazılsaydı
            // İstanbul Unavailable'a düşerdi ve bu assertion yakalardı.
            assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Loaded)
            assertTrue(items.first { it.cityId == istanbul.id }.weather is CardWeather.Loaded)
            assertTrue("Deneme bitince kilit kalmalı", state.retryingCityIds.isEmpty())
        }
    }

    /**
     * Gerçekte batchAll, tek şehrin denemesi başarısız olursa Failure döndürür
     * (hiçbir öğe başarılı olmadığı için). Kart değişmez ve kilit açılır —
     * kullanıcı tekrar deneyebilir.
     */
    @Test
    fun `tekrar deneme hata dondururse kart hata olarak kalir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(ankara.id)
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul, ankara))
        )

        // Deneme bu kez tümüyle başarısız: depo Failure döndürür.
        // (failCityWeathers, sahtede omit filtresinden önce döndüğü için
        // ayrıca omitCities temizlemek gereksizdir.)
        fakeWeather.failCityWeathers = true
        viewModel.onRetryCity(ankara.id)

        viewModel.uiState.test {
            val state = awaitItem()
            val items = (state.content as UiState.Success).data
            assertTrue(items.first { it.cityId == ankara.id }.weather is CardWeather.Unavailable)
            // Başarısız deneme önbelleğe dokunmaz: İstanbul yüklü kalmalı.
            assertTrue(items.first { it.cityId == istanbul.id }.weather is CardWeather.Loaded)
            assertTrue("Başarısız deneme kilidi bırakmalı", state.retryingCityIds.isEmpty())
        }
    }

    /**
     * Deneme sırasında şehir silinmiş olabilir; akış çökmemeli ve boşa istek
     * atılmamalı.
     */
    @Test
    fun `silinmis sehir icin tekrar deneme istek atmaz`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(fakeWeather = fakeWeather)

        viewModel.onRetryCity(999L)

        assertEquals(
            "Yalnız ilk listenin yüklenmesi istek atmış olmalı",
            1,
            fakeWeather.cityWeathersRequests.size
        )
        assertTrue(viewModel.uiState.value.retryingCityIds.isEmpty())
    }

    @Test
    fun `emits loading while repository is slow`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)

            fakeRepository.completeAllCityWeathersGates()
            val loaded = awaitItem()
            assertTrue(loaded.content is UiState.Success)
            assertEquals(
                5,
                ((loaded.content as UiState.Success).data.size)
            )
        }
    }

    @Test
    fun `unit change from shared source converts temperatures to fahrenheit`() = runTest {
        val unitRepository = FakeTemperatureUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            getCachedForecasts = GetCachedForecastsUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val celsiusState = awaitItem()
            assertTrue(
                ((celsiusState.content as UiState.Success).data.first().weather as CardWeather.Loaded)
                    .temperatureText.endsWith("°C")
            )

            unitRepository.setUnit(TemperatureUnit.FAHRENHEIT)
            val fahrenheitState = awaitItem()
            assertTrue(fahrenheitState.content is UiState.Success)
            assertTrue(
                (fahrenheitState.content as UiState.Success).data.all {
                    (it.weather as CardWeather.Loaded).temperatureText.endsWith("°F")
                }
            )
        }
    }

    @Test
    fun `loadData emits loading then success`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.completeAllCityWeathersGates()
            assertEquals(LoadState.Success, awaitItem())
        }

        viewModel.loadData()
        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.completeAllCityWeathersGates()
            assertEquals(LoadState.Success, awaitItem())
        }
    }

    @Test
    fun `cancellation is not presented as error`() = runTest {
        val fakeRepository = FakeWeatherRepository().apply {
            throwCancellation = true
        }
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Loading)
        }
        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
        }
    }

    @Test
    fun `retry uses loadData`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.completeAllCityWeathersGates()
            assertEquals(LoadState.Success, awaitItem())
        }

        viewModel.onRetry()
        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.completeAllCityWeathersGates()
            assertEquals(LoadState.Success, awaitItem())
        }
    }

    @Test
    fun `retry cancels in-flight load and starts new call`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        // Init başlattı; arama/sorgu yok, yalnızca depo akışları tetiklendi.
        // Yeni çağrı askıdayken yeniden dene: önceki iptal edilir, yeni başlar.
        viewModel.onRetry()
        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.completeAllCityWeathersGates()
            val state = awaitItem()
            assertEquals(LoadState.Success, state)
        }

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Success)
            assertEquals(
                5,
                ((state.content as UiState.Success).data.size)
            )
        }

        // İptal edilen çağrılar tamamlanmadı; yalnızca son çağrı sonuç üretti.
        assertEquals(1, fakeRepository.finishedCityWeathersCalls)
    }

    @Test
    fun `refresh keeps content and toggles refreshing flag`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)
            fakeRepository.completeAllCityWeathersGates()
            val loaded = awaitItem()
            assertTrue(loaded.content is UiState.Success)
            assertTrue(loaded.isRefreshing.not())
        }

        viewModel.onRefresh()
        viewModel.uiState.test {
            val refreshing = awaitItem()
            assertTrue(refreshing.isRefreshing)
            assertTrue(refreshing.content is UiState.Success)

            fakeRepository.completeAllCityWeathersGates()
            val refreshed = awaitItem()
            assertTrue(refreshed.isRefreshing.not())
            assertEquals(
                5,
                ((refreshed.content as UiState.Success).data.size)
            )
        }
    }

    @Test
    fun `failed refresh keeps old content and emits snackbar event`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            getCachedForecasts = GetCachedForecastsUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)
            fakeRepository.completeAllCityWeathersGates()
            assertTrue(awaitItem().content is UiState.Success)
        }

        fakeRepository.failCityWeathers = true
        viewModel.onRefresh()
        viewModel.uiState.test {
            val refreshing = awaitItem()
            assertTrue(refreshing.isRefreshing)
            assertTrue(refreshing.content is UiState.Success)

            fakeRepository.completeAllCityWeathersGates()
            val failed = awaitItem()
            assertTrue(failed.isRefreshing.not())
            assertTrue(failed.content is UiState.Success)
            assertEquals(
                5,
                ((failed.content as UiState.Success).data.size)
            )
        }

        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is MainEvent.ShowSnackbar)
        }
    }

    /**
     * Gerçek depoda yazım asenkrondur: `DataStoreCityRepository.mutate` diske
     * yazar, `cities` akışı ise **bir sonraki okumada** yansıtır. Bu fake
     * mutasyonu hemen kaydeder ama akışa `flush()` ile geçirir; test böylece
     * üretimdeki sırayı yeniden üretir.
     *
     * ViewModel yalnız akış yayımladığında veri çektiği için, `flush()` çağrılana
     * kadar listede hiçbir değişiklik görünmemelidir — `flush()`'ın kendisi
     * yenilemenin tetikleyicisidir.
     */
    private class GatedCityRepository(initial: List<City>) : CityRepository {
        private val state = MutableStateFlow(initial)
        private var pending: List<City>? = null
        override val cities: Flow<List<City>> = state.asStateFlow()

        override suspend fun add(city: City): List<City> {
            val current = pending ?: state.value
            val updated = if (current.any { it.id == city.id }) current else current + city
            pending = updated
            return updated
        }

        override suspend fun remove(cityId: Long): List<City> {
            val updated = (pending ?: state.value).filterNot { it.id == cityId }
            pending = updated
            return updated
        }

        override suspend fun restore(city: City, index: Int): List<City> {
            val current = pending ?: state.value
            val updated = if (current.any { it.id == city.id }) {
                current
            } else {
                current.toMutableList().apply { add(index.coerceIn(0, current.size), city) }
            }
            pending = updated
            return updated
        }

        override suspend fun addAsLocation(city: City): List<City> {
            val location = city.copy(isLocation = true)
            // Gerçek depo sözleşmesi: konum tektir, kimlik değişse bile eski
            // konum kaydı düşer.
            val updated = listOf(location) +
                (pending ?: state.value).filterNot { it.isLocation || it.id == location.id }
            pending = updated
            return updated
        }

        /** Bekleyen yazımı tamamlar; DataStore'ın akışa yansıması budur. */
        fun flush() {
            pending?.let { state.value = it }
            pending = null
        }
    }

    /**
     * Bildirilen hata zincirinin başı: silinen kart ekranda kalıyordu.
     * Çünkü depo yayınladığında liste, silme sonrası başlatılan **ağ turu**
     * bitene kadar eski içeriği gösteriyordu. Kullanıcı "silinmedi" sanıp
     * aynı şehri yeniden silmeye çalışıyor (ikinci deneme sessizce yutuluyor,
     * çünkü şehir depoda yok) ve ardından yanlış şehrin snackbar'ında geri al
     * düğmesine basıyor.
     *
     * Depo gerçeği ağ turundan bağımsız olduğu için kart anında çıkmalı:
     * havası bilinmeyen şehir `CardWeather.Unavailable` olarak zaten
     * çizilebiliyor.
     */
    @Test
    fun `silinen sehir ag turu bitmeden listeden cikar`() = runTest {
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(weather, FakeCityRepository())
        viewModel.loadData()
        runCurrent()

        // Sonraki tur kıpırdamasın: kart yalnız depodan gelen gerçekle güncellenebilir.
        weather.autoCompleteGates = false
        viewModel.onDeleteCity(istanbul.id)
        runCurrent()

        val content = viewModel.uiState.value.content
        assertTrue("Icerik Success olmali", content is UiState.Success)
        content as UiState.Success
        assertTrue(
            "Silinen sehir ag turu tamamlanmadan da listeden cikmalı",
            content.data.none { it.cityId == istanbul.id }
        )
    }

    @Test
    fun `silinen sehir geri alininca ag turu bitmeden geri gelir`() = runTest {
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(weather, FakeCityRepository())
        viewModel.loadData()
        runCurrent()

        weather.autoCompleteGates = false
        viewModel.onDeleteCity(istanbul.id)
        runCurrent()
        assertTrue(
            "Once silinen sehir listeden cikmali",
            (viewModel.uiState.value.content as UiState.Success).data.none { it.cityId == istanbul.id }
        )

        viewModel.onUndoDelete(istanbul)
        runCurrent()

        val content = viewModel.uiState.value.content
        assertTrue("Icerik Success olmali", content is UiState.Success)
        content as UiState.Success
        assertEquals(
            "Geri alinan sehir ag turu tamamlanmadan da listede gorunmeli ve bir kez",
            1,
            content.data.count { it.cityId == istanbul.id }
        )
    }

    /**
     * Geri al, şehri listenin sonuna değil **eski yerine** koyar: kart geri
     * geldiğinde liste aynı sırayı korur, kullanıcı hangi kartın neden
     * yer değiştirdiğini görmez.
     */
    @Test
    fun `geri alinan sehir eski sirasina doner`() = runTest {
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val store = FakeCityRepository()
        val viewModel = mainViewModel(weather, store)
        viewModel.loadData()
        runCurrent()

        val before = store.current()
        val middle = before[2]
        assertEquals("Konum yoksa sira denemesi anlamsiz", false, middle.isLocation)

        viewModel.onDeleteCity(middle.id)
        runCurrent()
        viewModel.onUndoDelete(middle)
        runCurrent()

        assertEquals("Geri alinan sehir eski sirasina donmeli", before.map { it.id }, store.current().map { it.id })
        val content = viewModel.uiState.value.content
        assertTrue("Icerik Success olmali", content is UiState.Success)
        content as UiState.Success
        assertEquals(
            "Geri alinan sehir ekranda da eski sirasinda gorunmeli",
            before.map { it.name },
            content.data.map { it.name }
        )
    }

    @Test
    fun `deleting a city removes it from the visible list even when the write lands late`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val gatedCities = GatedCityRepository(listOf(istanbul, ankara))
        val viewModel = mainViewModel(fakeWeather, gatedCities)
        viewModel.loadData()
        runCurrent()

        viewModel.onDeleteCity(istanbul.id)
        runCurrent()
        gatedCities.flush()
        runCurrent()

        val content = viewModel.uiState.value.content
        assertTrue("Silinen sehir listeden cikmali", content is UiState.Success)
        content as UiState.Success
        assertTrue(
            "Silinen sehir hava durumu onbelleginde kalinsa da gosterilmemeli",
            content.data.none { it.cityId == istanbul.id }
        )
    }

    @Test
    fun `undo adds the city to the next refresh even when the write lands late`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val gatedCities = GatedCityRepository(listOf(istanbul, ankara))
        val viewModel = mainViewModel(fakeWeather, gatedCities)
        viewModel.loadData()
        runCurrent()

        viewModel.onDeleteCity(istanbul.id)
        runCurrent()
        gatedCities.flush()
        runCurrent()

        viewModel.onUndoDelete(istanbul)
        runCurrent()
        gatedCities.flush()
        runCurrent()

        assertTrue(
            "Geri alinan sehir yenilenen hava durumunda yer almali",
            fakeWeather.cityWeathersRequests.last().any { it.id == istanbul.id }
        )
        val content = viewModel.uiState.value.content
        assertTrue("Geri alinan sehir listede gorunmeli", content is UiState.Success)
        content as UiState.Success
        assertTrue(
            "Geri alinan sehir listede gorunmeli",
            content.data.any { it.cityId == istanbul.id }
        )
    }

    @Test
    fun `deleting a city removes it without refetching fresh data`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)
        val requestsBefore = fakeWeather.cityWeathersRequests.size

        viewModel.onDeleteCity(istanbul.id)

        assertTrue(fakeCities.current().none { it.id == istanbul.id })
        // Kalanların verisi taze olduğu için tur atılmaz (istek tasarrufu);
        // liste yine de hemen güncellenir.
        assertEquals(requestsBefore, fakeWeather.cityWeathersRequests.size)
        val content = viewModel.uiState.value.content
        assertTrue(content is UiState.Success)
        assertTrue(
            "Silinen şehir listeden çıkmalı",
            (content as UiState.Success).data.none { it.cityId == istanbul.id }
        )
    }

    @Test
    fun `delete emits snackbar with city name and undo action label`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(fakeWeather)

        viewModel.onDeleteCity(istanbul.id)

        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is MainEvent.ShowSnackbar)
            event as MainEvent.ShowSnackbar
            assertEquals(UiText.Resource(R.string.city_deleted, istanbul.name), event.message)
            assertEquals(UiText.Resource(R.string.undo_delete), event.actionLabel)
            assertTrue("Geri al aksiyonu tanimli olmali", event.action != null)
        }
    }

    @Test
    fun `undo action restores the deleted city and refreshes`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)

        viewModel.onDeleteCity(istanbul.id)
        var action: (() -> Unit)? = null
        viewModel.events.test {
            val event = awaitItem() as MainEvent.ShowSnackbar
            action = event.action
        }
        assertTrue(action != null)
        val requestsBefore = fakeWeather.cityWeathersRequests.size

        action!!.invoke()
        runCurrent()

        assertTrue(fakeCities.current().any { it.id == istanbul.id })
        assertTrue(fakeWeather.cityWeathersRequests.size > requestsBefore)
    }

    /**
     * Üst üste silmede yalnız **en son** silme geri alınabilir.
     *
     * Ekrandaki snackbar her yeni silmede değişir; eskisinin aksiyonu artık
     * yanlış şehri geri getirmemelidir. Aksi halde kullanıcı bir şehri silip
     * ekranda gördüğü başka bir "Geri al" düğmesine basıyor ve beklediği şehir
     * değil, başka bir şehir geri geliyordu.
     */
    @Test
    fun `ust uste silmede eski geri al aksiyonu hicbir seyi geri almaz`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)

        viewModel.onDeleteCity(istanbul.id)
        val firstAction = (viewModel.events.first() as MainEvent.ShowSnackbar).action
        viewModel.onDeleteCity(ankara.id)
        val secondAction = (viewModel.events.first() as MainEvent.ShowSnackbar).action
        runCurrent()

        firstAction!!.invoke()
        runCurrent()

        assertTrue(
            "Ekrani degismis bir geri al aksiyonu hicbir seyi geri almamali",
            fakeCities.current().none { it.id == istanbul.id }
        )
        assertTrue(
            "Yeni silinen sehir de yerinde kalmali",
            fakeCities.current().none { it.id == ankara.id }
        )

        secondAction!!.invoke()
        runCurrent()

        assertTrue(
            "Yalnizca en son silinen sehir geri alinmali",
            fakeCities.current().any { it.id == ankara.id }
        )
        assertTrue(fakeCities.current().none { it.id == istanbul.id })
    }

    @Test
    fun `deleting an unknown city is a no-op`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)
        val requestsBefore = fakeWeather.cityWeathersRequests.size

        viewModel.onDeleteCity(-1L)

        assertEquals(5, fakeCities.current().size)
        assertEquals(requestsBefore, fakeWeather.cityWeathersRequests.size)
    }

    /**
     * Açılışta veri çekimi **yalnız** depo akışının yayımladığı listeden başlar.
     *
     * Depo artık `StateFlow` olmadığı için dinleyen ilk değer gerçeğin kendisidir;
     * "başlangıç (tohum) değeri" diye bir ikinci değer yoktur. Bu testin asıl
     * güvencesi şudur: **ilk** hava durumu isteği zaten listenin tamamını
     * içerir. Eski hata tam olarak buradaydı — ilk istek yalnız tohumdaki tek
     * şehri taşıyor, eklenen şehirler hiç istenmiyordu.
     */
    @Test
    fun `ilk istek depodaki tum sehirleri icerir`() = runTest {
        val izmir = City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14))
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(weather),
            getCachedForecasts = GetCachedForecastsUseCase(weather),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(initial = listOf(istanbul, ankara, izmir))
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Success)
            assertEquals(3, (state.content as UiState.Success).data.size)
        }

        val firstRequest = weather.cityWeathersRequests.first()
        assertEquals(
            "Ilk istek eksik liste taşimamalı; eklenen şehirler atlanmamalı",
            3,
            firstRequest.size
        )
    }

    @Test
    fun `adisiz konum silinince konum silindi mesaji cikar`() = runTest {
        val konum = City("", null, null, Coordinates(41.01, 28.98), isLocation = true)
        val cities = FakeCityRepository(initial = listOf(konum))
        val viewModel = mainViewModel(
            fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true },
            fakeCities = cities
        )
        viewModel.loadData()
        runCurrent()

        viewModel.onDeleteCity(konum.id)
        runCurrent()

        viewModel.events.test {
            val event = expectMostRecentItem() as MainEvent.ShowSnackbar
            assertEquals(UiText.Resource(R.string.location_deleted), event.message)
        }
    }
}
