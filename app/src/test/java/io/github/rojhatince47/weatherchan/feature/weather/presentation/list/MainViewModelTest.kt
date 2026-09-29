package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
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
        locationProvider: LocationProvider = FakeLocationProvider(coordinates = null),
        permissions: CityPermissions = FakeCityPermissions(asked = true)
    ) = MainViewModel(
        getCityWeathers = GetCityWeathersUseCase(fakeWeather),
        searchCities = SearchCitiesUseCase(fakeWeather),
        uiMapper = WeatherUiMapper(),
        unitRepository = FakeTemperatureUnitRepository(),
        cityRepository = fakeCities,
        getLocationCity = GetLocationCityUseCase(locationProvider, fakeWeather),
        cityPermissions = permissions
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
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
    fun `sorgu filtresi yuksuz sehire de uygulanir`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(ankara.id)
        }
        val viewModel = mainViewModel(
            fakeWeather = fakeWeather,
            fakeCities = FakeCityRepository(initial = listOf(istanbul, ankara))
        )

        viewModel.onQueryChange("Ankara")

        viewModel.uiState.test {
            val items = (awaitItem().content as UiState.Success).data
            assertEquals(listOf(ankara.id), items.map { it.cityId })
            assertTrue(items.single().weather is CardWeather.Unavailable)
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

    @Test
    fun `emits loading while repository is slow`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
    fun `search filters cities`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
        )

        viewModel.onQueryChange("Ankara")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Success)
            assertTrue(
                (state.content as UiState.Success).data.all {
                    it.name.contains("Ankara")
                }
            )
        }
    }

    /**
     * Regresyon: arama alanı kontrollü bir bileşendir (value = uiState.query).
     * Sorgu, ağ cevabı geldikten sonra state'e yazıldığı için yavaş/yeni kilitli
     * bir ağda kullanıcı yazarken hiçbir karakter ekranda görünmüyor ve her tuş
     * vuruşu bir öncekini iptal ediyordu. Sorgu, ağdan BAĞIMSIZ anında
     * görünür olmalı ve tuş vuruşu ağ çağrısı tetiklememeli.
     */
    @Test
    fun `typing is reflected immediately without waiting for the network`() = runTest {
        val repository = FakeWeatherRepository().apply { autoCompleteGates = false }
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(repository),
            searchCities = SearchCitiesUseCase(repository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
        )

        // İlk yükleme tamamlanır; sonrasında ağ kapılı kalır.
        repository.completeAllCityWeathersGates()
        assertTrue(viewModel.uiState.value.content is UiState.Success)
        val callsAfterLoad = repository.finishedCityWeathersCalls

        viewModel.onQueryChange("Ankara")

        // Ağ hâlâ bekliyor olmalı; buna rağmen sorgu anında state'te.
        assertEquals("Ankara", viewModel.uiState.value.query)
        assertEquals(
            "Yazmak yeni ag cagrisi tetiklememeli",
            callsAfterLoad,
            repository.finishedCityWeathersCalls
        )
    }

    @Test
    fun `typing filters already loaded cities without a new network call`() = runTest {
        val repository = FakeWeatherRepository().apply { autoCompleteGates = false }
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(repository),
            searchCities = SearchCitiesUseCase(repository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
        )
        repository.completeAllCityWeathersGates()
        val callsAfterLoad = repository.finishedCityWeathersCalls

        viewModel.onQueryChange("Ankara")

        val content = viewModel.uiState.value.content
        assertTrue("Ankara filtresi sonuc vermeli", content is UiState.Success)
        assertEquals(
            listOf("Ankara"),
            (content as UiState.Success).data.map { it.name }
        )
        assertEquals(
            callsAfterLoad,
            repository.finishedCityWeathersCalls
        )
    }

    @Test
    fun `empty search shows empty state`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
        )

        viewModel.onQueryChange("zzz")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Empty)
        }
    }

    @Test
    fun `unit change from shared source converts temperatures to fahrenheit`() = runTest {
        val unitRepository = FakeTemperatureUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
    fun `toggle from shared settings view model updates list temperatures`() = runTest {
        val unitRepository = FakeTemperatureUnitRepository()
        val settingsViewModel = SettingsViewModel(unitRepository, FakeThemeModeRepository())
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
        )

        viewModel.uiState.test {
            val celsiusState = awaitItem()
            assertTrue(
                ((celsiusState.content as UiState.Success).data.first().weather as CardWeather.Loaded)
                    .temperatureText.endsWith("°C")
            )

            settingsViewModel.onToggleUnit()
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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
            searchCities = SearchCitiesUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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

    @Test
    fun `query shorter than three characters keeps live search idle`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(fakeWeather)

        viewModel.onQueryChange("an")

        assertTrue(viewModel.uiState.value.searchState is SearchUiState.Idle)
        assertEquals(emptyList<String>(), fakeWeather.searchRequests)
    }

    @Test
    fun `live search fires after debounce`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val berlin = City("Berlin", "Berlin", "Almanya", Coordinates(52.52, 13.405))
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Success(listOf(berlin))
        }
        val viewModel = mainViewModel(fakeWeather)

        viewModel.onQueryChange("ber")
        advanceTimeBy(299)
        runCurrent()
        assertTrue(viewModel.uiState.value.searchState is SearchUiState.Idle)

        advanceTimeBy(1)
        runCurrent()

        val state = viewModel.uiState.value.searchState
        assertTrue(state is SearchUiState.Success)
        assertEquals(listOf("ber"), fakeWeather.searchRequests)
        assertEquals(listOf(berlin), (state as SearchUiState.Success).results.map { it.city })
    }

    @Test
    fun `typing again cancels the pending search`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = mainViewModel(fakeWeather)

        viewModel.onQueryChange("ank")
        advanceTimeBy(200)
        viewModel.onQueryChange("anka")
        advanceTimeBy(400)
        runCurrent()

        assertEquals(listOf("anka"), fakeWeather.searchRequests)
    }

    @Test
    fun `live search failure surfaces error`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Failure(AppError.Network)
        }
        val viewModel = mainViewModel(fakeWeather)

        viewModel.onQueryChange("zzz")
        advanceTimeBy(400)
        runCurrent()

        assertTrue(viewModel.uiState.value.searchState is SearchUiState.Failure)
    }

    @Test
    fun `adding a city from live results stores it and fetches its weather`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)
        val before = fakeWeather.cityWeathersRequests.size

        val berlin = City("Berlin", "Berlin", "Almanya", Coordinates(52.52, 13.405))
        viewModel.onAddCity(berlin)

        assertTrue(fakeCities.current().any { it.id == berlin.id })
        assertTrue(fakeWeather.cityWeathersRequests.size > before)
        assertTrue(fakeWeather.cityWeathersRequests.last().any { it.id == berlin.id })
    }

    @Test
    fun `adding a city flips its in-list flag in live results`() = runTest {
        Dispatchers.setMain(UnconfinedTestDispatcher(testScheduler))
        val berlin = City("Berlin", "Berlin", "Almanya", Coordinates(52.52, 13.405))
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Success(listOf(berlin))
        }
        val viewModel = mainViewModel(fakeWeather)
        viewModel.onQueryChange("ber")
        advanceTimeBy(400)
        runCurrent()
        val before = viewModel.uiState.value.searchState as SearchUiState.Success
        assertEquals(false, before.results.single().isInList)

        viewModel.onAddCity(berlin)
        runCurrent()

        val after = viewModel.uiState.value.searchState as SearchUiState.Success
        assertEquals(true, after.results.single().isInList)
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

        override suspend fun addAsLocation(city: City): List<City> {
            val location = city.copy(isLocation = true)
            val updated = listOf(location) + (pending ?: state.value).filterNot { it.id == location.id }
            pending = updated
            return updated
        }

        /** Bekleyen yazımı tamamlar; DataStore'ın akışa yansıması budur. */
        fun flush() {
            pending?.let { state.value = it }
            pending = null
        }
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
    fun `deleting a city removes it and triggers a refresh without it`() = runTest {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val fakeCities = FakeCityRepository()
        val viewModel = mainViewModel(fakeWeather, fakeCities)
        val requestsBefore = fakeWeather.cityWeathersRequests.size

        viewModel.onDeleteCity(istanbul.id)

        assertTrue(fakeCities.current().none { it.id == istanbul.id })
        assertTrue(
            "Silme sonrasi hava durumu istegi atilmali",
            fakeWeather.cityWeathersRequests.size > requestsBefore
        )
        assertTrue(
            "Yenileme artik silinen sehri icermemeli",
            fakeWeather.cityWeathersRequests.last().none { it.id == istanbul.id }
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
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository(initial = listOf(istanbul, ankara, izmir)),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(coordinates = null), FakeWeatherRepository()),
            cityPermissions = FakeCityPermissions(asked = true)
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

    // --- konum testleri için sahteler -----------------------------------------

    private val locationCoordinates = Coordinates(41.01, 28.98)

    /** Sabit konum döndüren sahte; `null` verirsen konum okunamaz. */
    private class FakeLocationProvider(private val coordinates: Coordinates?) : LocationProvider {
        var callCount: Int = 0
            private set

        override suspend fun currentCoordinates(): Coordinates? {
            callCount++
            return coordinates
        }
    }

    /** Kalıcı "diyalog gösterildi" bayrağının sahtesi. */
    private class FakeCityPermissions(private var asked: Boolean = false) : CityPermissions {
        val askedFlag: Boolean get() = asked

        override suspend fun wasAsked(): Boolean = asked

        override suspend fun markAsked() {
            asked = true
        }
    }

    /**
     * `wasAsked` okumasını [gate] açılana kadar bekleten sahte.
     *
     * Gerçekte bu okuma DataStore'dan gelir ve asenkrondur; ilk Compose
     * karesi bu okuma tamamlanmadan gelebilir. Gecikmeli sahte o pencereyi
     * testte kalıcılaştırır.
     */
    private class GatedCityPermissions(private val gate: CompletableDeferred<Boolean>) : CityPermissions {
        private var asked = false

        override suspend fun wasAsked(): Boolean = gate.await()

        override suspend fun markAsked() {
            asked = true
        }
    }

    private fun TestScope.viewModelWithLocation(
        cities: CityRepository = FakeCityRepository(),
        provider: LocationProvider = FakeLocationProvider(locationCoordinates),
        permissions: CityPermissions = FakeCityPermissions(asked = true),
        locationWeather: FakeWeatherRepository = FakeWeatherRepository(),
        weather: FakeWeatherRepository = FakeWeatherRepository().apply { autoCompleteGates = true }
    ) = MainViewModel(
        getCityWeathers = GetCityWeathersUseCase(weather),
        searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
        uiMapper = WeatherUiMapper(),
        unitRepository = FakeTemperatureUnitRepository(),
        cityRepository = cities,
        getLocationCity = GetLocationCityUseCase(provider, locationWeather),
        cityPermissions = permissions
    )

    @Test
    fun `ilk acilista izin sorulmadi durumundadir`() = runTest {
        val viewModel = viewModelWithLocation(permissions = FakeCityPermissions(asked = false))
        runCurrent()

        assertEquals(
            "Diyalog bir kez gösterilmeli",
            LocationPermissionState.NotAsked,
            viewModel.uiState.value.permissionState
        )
    }

    /**
     * Kalıcı bayrak okunana kadar izin durumu **karar verilemez** olmalıdır.
     *
     * Yoksa ilk karede durum `NotAsked` görünür ve UI diyaloğu ateşler.
     * Gerçek cihazda böyle oldu: kullanıcı izni reddettikten sonra uygulamayı
     * her açtığında sistem diyaloğu tekrar karşısına çıktı. Bayrağın DataStore
     * okuması ilk kareden önce tamamlanırsa sorun görünmez; tamamlanmazsa
     * olur. Yani hata **zamanlamaya bağlı** olduğu için gözle güvenilmez,
     * durumun kendisiyle engellenmelidir.
     */
    @Test
    fun `bayrak okunmadan once izin durumu karar verilemez`() = runTest {
        val gate = CompletableDeferred<Boolean>()
        val viewModel = viewModelWithLocation(permissions = GatedCityPermissions(gate))

        assertEquals(
            "Okuma sürerken UI karar verememeli; aksi halde diyalog yanlışlıkla açılır",
            LocationPermissionState.Restoring,
            viewModel.uiState.value.permissionState
        )

        gate.complete(true)
        runCurrent()

        assertEquals(
            "Bayrak okununca diyaloğa karar verilebilmeli",
            LocationPermissionState.Asked,
            viewModel.uiState.value.permissionState
        )
    }

    @Test
    fun `bayrak varsa diyalog yeniden acilmaz konum dogrudan okunur`() = runTest {
        val provider = FakeLocationProvider(locationCoordinates)
        val viewModel = viewModelWithLocation(provider = provider, permissions = FakeCityPermissions(asked = true))
        runCurrent()

        // Cevap bayrağa yazılmaz; konum okunarak gerçek durum belirlenir.
        assertEquals(LocationPermissionState.Asked, viewModel.uiState.value.permissionState)
        assertEquals("Bayrak varsa konum yine de okunmalı", 1, provider.callCount)
    }

    @Test
    fun `izin verildiginde konum listeye eklenir`() = runTest {
        val cities = FakeCityRepository(initial = emptyList())
        val viewModel = viewModelWithLocation(
            cities = cities,
            permissions = FakeCityPermissions(asked = false)
        )
        runCurrent()

        viewModel.onPermissionResult(granted = true)
        runCurrent()

        val stored = cities.current()
        assertEquals("Konum listeye eklenmeli", 1, stored.size)
        assertTrue("Konum olarak işaretlenmeli", stored.single().isLocation)
        assertEquals(LocationState.Resolved, viewModel.uiState.value.locationState)
    }

    @Test
    fun `izin reddedilince konum okunmaz bos durum gosterilir`() = runTest {
        val provider = FakeLocationProvider(locationCoordinates)
        val cities = FakeCityRepository(initial = emptyList())
        val viewModel = viewModelWithLocation(
            cities = cities,
            provider = provider,
            permissions = FakeCityPermissions(asked = false)
        )
        runCurrent()

        viewModel.onPermissionResult(granted = false)
        runCurrent()

        assertEquals(LocationPermissionState.Denied, viewModel.uiState.value.permissionState)
        assertEquals(0, provider.callCount)
        assertTrue("Reddedilince listeye eklenmemeli", cities.current().isEmpty())
        assertEquals(LocationState.Unavailable, viewModel.uiState.value.locationState)
    }

    @Test
    fun `konum okunamazsa akis durmaz bos durum gosterilir`() = runTest {
        val cities = FakeCityRepository(initial = emptyList())
        val viewModel = viewModelWithLocation(
            cities = cities,
            provider = FakeLocationProvider(coordinates = null)
        )
        runCurrent()

        assertEquals(
            "Konum alınamadıysa akış hata vermemeli",
            LocationState.Unavailable,
            viewModel.uiState.value.locationState
        )
        assertTrue(cities.current().isEmpty())
    }

    @Test
    fun `konum listenin basinda gorunur`() = runTest {
        val cities = FakeCityRepository(initial = listOf(istanbul, ankara))
        val viewModel = viewModelWithLocation(
            cities = cities,
            provider = FakeLocationProvider(ankara.coordinates),
            // Ad çözülemezse kullanıcı "Konum" görür; sıralama testi için
            // gerçek adın geldiği durum kurulur.
            locationWeather = FakeWeatherRepository().apply {
                searchResult = AppResult.Success(listOf(ankara.copy(isLocation = true)))
            },
            // Ağ cevabı depodaki sıranın TERSİ döner. Sıralama ağdan değil
            // depodan okunmalıdır; bu olmadan test tesadüfen geçer.
            weather = FakeWeatherRepository().apply {
                autoCompleteGates = true
                reverseCityWeathersOrder = true
            }
        )
        runCurrent()

        val content = viewModel.uiState.value.content
        assertTrue("Liste yüklenmeli", content is UiState.Success)
        val names = (content as UiState.Success).data.map { it.name }
        assertEquals(
            "Konum başa alınmalı, kalan sıra korunmalı",
            listOf("Ankara", "İstanbul"),
            names
        )
    }

    @Test
    fun `konum silinince geri ekleme gorunur olur`() = runTest {
        val konum = ankara.copy(isLocation = true)
        val cities = FakeCityRepository(initial = listOf(istanbul, konum))
        val viewModel = viewModelWithLocation(
            cities = cities,
            provider = FakeLocationProvider(konum.coordinates)
        )
        runCurrent()
        assertEquals(LocationState.Resolved, viewModel.uiState.value.locationState)

        viewModel.onDeleteCity(konum.id)
        runCurrent()

        assertEquals(
            "Konum silinince geri eklenebilmeli",
            LocationState.Unavailable,
            viewModel.uiState.value.locationState
        )
    }

    @Test
    fun `konum haricindeki sehir silinince konum durumu degismez`() = runTest {
        val konum = ankara.copy(isLocation = true)
        val cities = FakeCityRepository(initial = listOf(istanbul, konum))
        val viewModel = viewModelWithLocation(
            cities = cities,
            provider = FakeLocationProvider(konum.coordinates)
        )
        runCurrent()

        viewModel.onDeleteCity(istanbul.id)
        runCurrent()

        assertEquals(
            "Konum hâlâ listede, durum değişmemeli",
            LocationState.Resolved,
            viewModel.uiState.value.locationState
        )
    }
}
