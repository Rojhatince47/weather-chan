package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
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

    private fun TestScope.mainViewModel(fakeWeather: FakeWeatherRepository, fakeCities: CityRepository = FakeCityRepository()) =
        MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeWeather),
            searchCities = SearchCitiesUseCase(fakeWeather),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = fakeCities
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

    @Test
    fun `emits loading while repository is slow`() = runTest {
        val fakeRepository = FakeWeatherRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeRepository),
            searchCities = SearchCitiesUseCase(fakeRepository),
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
    fun `search filters cities`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            searchCities = SearchCitiesUseCase(FakeWeatherRepository()),
            uiMapper = WeatherUiMapper(),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
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
            cityRepository = FakeCityRepository()
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
            cityRepository = FakeCityRepository()
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
            cityRepository = FakeCityRepository()
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
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val celsiusState = awaitItem()
            assertTrue(
                ((celsiusState.content as UiState.Success).data.first().temperatureText)
                    .endsWith("°C")
            )

            unitRepository.setUnit(TemperatureUnit.FAHRENHEIT)
            val fahrenheitState = awaitItem()
            assertTrue(fahrenheitState.content is UiState.Success)
            assertTrue(
                (fahrenheitState.content as UiState.Success).data.all {
                    it.temperatureText.endsWith("°F")
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
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val celsiusState = awaitItem()
            assertTrue(
                ((celsiusState.content as UiState.Success).data.first().temperatureText)
                    .endsWith("°C")
            )

            settingsViewModel.onToggleUnit()
            val fahrenheitState = awaitItem()
            assertTrue(fahrenheitState.content is UiState.Success)
            assertTrue(
                (fahrenheitState.content as UiState.Success).data.all {
                    it.temperatureText.endsWith("°F")
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
            searchCities = SearchCitiesUseCase(fakeRepository),
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
            searchCities = SearchCitiesUseCase(fakeRepository),
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
            searchCities = SearchCitiesUseCase(fakeRepository),
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
            searchCities = SearchCitiesUseCase(fakeRepository),
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
            searchCities = SearchCitiesUseCase(fakeRepository),
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
}
