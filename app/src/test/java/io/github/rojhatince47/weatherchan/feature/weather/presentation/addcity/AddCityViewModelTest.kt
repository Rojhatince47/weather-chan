package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySuggestionProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.DeviceCountryProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.FakeNearbyCitySuggestionProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionData
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionKind
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AddCityViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    private val seed = CitySeed("Kocaeli", "TR", "Türkiye", Coordinates(40.7654, 29.9408))
    private val seed2 = CitySeed("Bursa", "TR", "Türkiye", Coordinates(40.1885, 29.061))
    private val kocaeli = seed.toCity()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        weather: FakeWeatherRepository = FakeWeatherRepository(),
        cities: FakeCityRepository = FakeCityRepository(initial = emptyList()),
        suggestions: SuggestionData? = SuggestionData(listOf(seed, seed2), emptyList()),
        coordinates: Coordinates? = null,
        location: LocationProvider = LocationProvider { coordinates },
        countryCode: String? = "TR",
        nearby: FakeNearbyCitySuggestionProvider = FakeNearbyCitySuggestionProvider(),
        searchResult: AppResult<List<City>> = AppResult.Success(listOf(kocaeli)),
        weatherAutoComplete: Boolean = true,
        weatherFail: Boolean = false,
        unitRepository: FakeTemperatureUnitRepository = FakeTemperatureUnitRepository()
    ): AddCityViewModel {
        weather.searchResult = searchResult
        weather.autoCompleteGates = weatherAutoComplete
        weather.failCityWeathers = weatherFail
        return AddCityViewModel(
            searchCities = SearchCitiesUseCase(weather),
            suggestCities = SuggestCitiesUseCase(
                suggestionProvider = CitySuggestionProvider { suggestions },
                nearbyProvider = nearby,
                locationProvider = location,
                deviceCountryProvider = DeviceCountryProvider { countryCode }
            ),
            cityRepository = cities,
            getCityWeathers = GetCityWeathersUseCase(weather),
            unitRepository = unitRepository,
            weatherUiMapper = WeatherUiMapper(WeatherAdviceEngine())
        )
    }

    // --- Mevcut testler (SuggestionLists bucket'larına taşındı) ---

    @Test
    fun `konumlu acilista once onbellegi gosterir canli sonucu ikinci yayimla gunceller`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(
            result = listOf(seed2),
            refreshResult = listOf(seed2, seed),
            refreshDelayMs = 10
        )
        val vm = viewModel(coordinates = seed.coordinates, suggestions = null, nearby = fake)

        val phase1 = vm.uiState.value.suggestions
        assertTrue(phase1?.nearby?.isNotEmpty() == true)
        assertEquals(listOf("Bursa"), phase1?.nearby?.map { it.city.name })

        advanceTimeBy(11)
        runCurrent()

        val phase2 = vm.uiState.value.suggestions
        assertEquals(1, fake.refreshCalls)
        assertTrue(phase2?.nearby?.isNotEmpty() == true)
        assertEquals(listOf("Kocaeli", "Bursa"), phase2?.nearby?.map { it.city.name })
    }

    @Test
    fun `konumlu acilikta canli sonuc onbellege esitse ikinci yayim yapilmaz`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(
            result = listOf(seed2),
            refreshResult = listOf(seed2)
        )
        val vm = viewModel(coordinates = seed.coordinates, suggestions = null, nearby = fake)
        advanceTimeBy(11)
        runCurrent()
        assertEquals(1, fake.refreshCalls)
        assertEquals(listOf("Bursa"), vm.uiState.value.suggestions?.nearby?.map { it.city.name })
    }

    @Test
    fun `yakin saglayici bosken ulke dalina duser`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(result = null)
        val vm = viewModel(coordinates = seed.coordinates, nearby = fake)
        advanceTimeBy(11)
        runCurrent()
        assertTrue(vm.uiState.value.suggestions?.country?.isNotEmpty() == true)
        assertEquals(listOf("Kocaeli", "Bursa"), vm.uiState.value.suggestions?.country?.map { it.city.name })
        assertEquals(0, fake.refreshCalls)
    }

    @Test
    fun `dosya asamasi yakininda gecikmesine ragmen aninda yayimlanir`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(result = listOf(seed), nearbyDelayMs = 10)
        val vm = viewModel(coordinates = seed.coordinates, nearby = fake)

        val phaseA = vm.uiState.value.suggestions
        assertTrue(phaseA?.country?.isNotEmpty() == true)
        assertEquals(listOf("Kocaeli", "Bursa"), phaseA?.country?.map { it.city.name })

        advanceTimeBy(11)
        runCurrent()
        assertTrue(vm.uiState.value.suggestions?.nearby?.isNotEmpty() == true)
    }

    @Test
    fun `konumsuz acilikta canli tazeleme cagrilmaz`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(result = listOf(seed))
        val vm = viewModel(nearby = fake)
        advanceTimeBy(11)
        runCurrent()
        assertTrue(fake.origins.isEmpty())
        assertEquals(0, fake.refreshCalls)
        assertTrue(vm.uiState.value.suggestions?.country?.isNotEmpty() == true)
    }

    @Test
    fun `tazeleme beklerken eklenen sehir sonuc cikinca da dusuk kalir`() = runTest(dispatcher) {
        val fake = FakeNearbyCitySuggestionProvider(
            result = listOf(seed2),
            refreshResult = listOf(seed, seed2),
            refreshDelayMs = 10
        )
        val vm = viewModel(coordinates = seed.coordinates, suggestions = null, nearby = fake)
        vm.onAddCity(seed.toCity())

        advanceTimeBy(11)
        runCurrent()

        assertEquals(1, fake.refreshCalls)
        assertEquals(listOf("Bursa"), vm.uiState.value.suggestions?.nearby?.map { it.city.name })
    }

    @Test
    fun `acilista oneriler yuklenir`() = runTest(dispatcher) {
        viewModel().uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Kocaeli", "Bursa"), state.suggestions?.country?.map { it.city.name })
        }
    }

    @Test
    fun `veri yoksa oneriler olmaz ekran cokmez`() = runTest(dispatcher) {
        viewModel(suggestions = null).uiState.test {
            assertEquals(null, awaitItem().suggestions)
        }
    }

    @Test
    fun `sorgu iki karaktere kadar aramaz`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Ko")
            assertTrue(expectMostRecentItem().searchState is SearchUiState.Idle)
        }
    }

    @Test
    fun `sorgu uc karakteri gecince debounce sonrasi arar`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS - 1)
            runCurrent()
            assertTrue(expectMostRecentItem().searchState is SearchUiState.Idle)

            advanceTimeBy(1)
            runCurrent()
            val results = expectMostRecentItem().searchState
            assertTrue(results is SearchUiState.Success)
            assertEquals(listOf("Kocaeli"), (results as SearchUiState.Success).results.map { it.city.name })
        }
    }

    @Test
    fun `yeni karakter yazinca bekleyen istek iptal edilir`() = runTest(dispatcher) {
        val weather = FakeWeatherRepository()
        val vm = viewModel(weather = weather)
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS - 100)
            vm.onQueryChange("Koca")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            assertEquals(listOf("Koca"), weather.searchRequests)
            assertTrue(expectMostRecentItem().searchState is SearchUiState.Success)
        }
    }

    @Test
    fun `arama hatasi yuzeye cikar`() = runTest(dispatcher) {
        val vm = viewModel(searchResult = AppResult.Failure(AppError.Network))
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            assertTrue(expectMostRecentItem().searchState is SearchUiState.Failure)
        }
    }

    @Test
    fun `sehir eklenince oneri satiri aninda duser`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = emptyList())
        val vm = viewModel(cities = cities)
        vm.uiState.test {
            awaitItem()
            vm.onAddCity(kocaeli)
            assertEquals(listOf("Bursa"), expectMostRecentItem().suggestions?.country?.map { it.city.name })
        }
    }

    @Test
    fun `asama b beklerken eklenen sehir aninda duser`() = runTest(dispatcher) {
        val vm = viewModel(
            location = LocationProvider {
                delay(10_000)
                seed.coordinates
            }
        )
        vm.onAddCity(kocaeli)

        assertEquals(listOf("Bursa"), vm.uiState.value.suggestions?.country?.map { it.city.name })
    }

    @Test
    fun `silinen sehir oneri olarak geri gelir`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = emptyList())
        val vm = viewModel(cities = cities)
        vm.uiState.test {
            awaitItem()
            vm.onAddCity(kocaeli)
            assertEquals(listOf("Bursa"), expectMostRecentItem().suggestions?.country?.map { it.city.name })
            cities.remove(kocaeli.id)
            assertEquals(listOf("Kocaeli", "Bursa"), expectMostRecentItem().suggestions?.country?.map { it.city.name })
        }
    }

    @Test
    fun `eklenen sehir arama sonucunda listeye gecer`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = emptyList())
        val vm = viewModel(cities = cities)
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            val before = expectMostRecentItem().searchState
            assertTrue(before is SearchUiState.Success)
            assertFalse((before as SearchUiState.Success).results.single().isInList)

            vm.onAddCity(kocaeli)
            val after = expectMostRecentItem().searchState
            assertTrue(after is SearchUiState.Success)
            assertTrue((after as SearchUiState.Success).results.single().isInList)
        }
    }

    @Test
    fun `tum oneriler eklenince bolum cizilmez`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = emptyList())
        val vm = viewModel(cities = cities)
        vm.uiState.test {
            awaitItem()
            vm.onAddCity(kocaeli)
            vm.onAddCity(seed2.toCity())
            assertEquals(null, expectMostRecentItem().suggestions)
        }
    }

    @Test
    fun `sorgu bosalinca arama durur ve oneriler geri gelir`() = runTest(dispatcher) {
        val vm = viewModel()
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            assertTrue(expectMostRecentItem().searchState is SearchUiState.Success)

            vm.onQueryChange("")
            val state = expectMostRecentItem()
            assertTrue(state.searchState is SearchUiState.Idle)
            assertTrue(state.query.isEmpty())
            assertEquals(listOf("Kocaeli", "Bursa"), state.suggestions?.country?.map { it.city.name })
        }
    }

    @Test
    fun `zaten listedeki sehir onerilmez`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = listOf(kocaeli))
        viewModel(cities = cities).uiState.test {
            assertEquals(listOf("Bursa"), awaitItem().suggestions?.country?.map { it.city.name })
        }
    }

    @Test
    fun `listedeki sehir arama sonucunda tikli gelir`() = runTest(dispatcher) {
        val vm = viewModel(cities = FakeCityRepository(initial = listOf(kocaeli)))
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            val search = expectMostRecentItem().searchState
            assertTrue(search is SearchUiState.Success)
            assertTrue((search as SearchUiState.Success).results.single().isInList)
        }
    }

    @Test
    fun `arama sonucu yakinlikla tiklenir`() = runTest(dispatcher) {
        val gpsKocaeli = kocaeli.copy(coordinates = Coordinates(40.7660, 29.9410))
        val vm = viewModel(cities = FakeCityRepository(initial = listOf(gpsKocaeli)))
        vm.uiState.test {
            awaitItem()
            vm.onQueryChange("Koc")
            advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
            runCurrent()
            val search = expectMostRecentItem().searchState
            assertTrue(search is SearchUiState.Success)
            assertTrue((search as SearchUiState.Success).results.single().isInList)
        }
    }

    // --- Yeni testler: hava durumu çekimi ---

    @Test
    fun `arama sonuclari loading kartlarla gosterilir sonra dolu`() = runTest(dispatcher) {
        val vm = viewModel(weatherAutoComplete = true)
        vm.onQueryChange("Koc")
        advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        val results = (vm.uiState.value.searchState as SearchUiState.Success).results
        assertTrue(results.single().cardWeather is CardWeather.Loaded)
    }

    @Test
    fun `hava durumu basarisizsa unavailable olur`() = runTest(dispatcher) {
        val vm = viewModel(weatherAutoComplete = true, weatherFail = true)
        vm.onQueryChange("Koc")
        advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        val results = (vm.uiState.value.searchState as SearchUiState.Success).results
        assertEquals(CardWeather.Unavailable, results.single().cardWeather)
    }

    @Test
    fun `bos hava durumu sonucu unavailable yapar`() = runTest(dispatcher) {
        val weather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            omitCities = setOf(kocaeli.id)
        }
        val vm = viewModel(weather = weather, searchResult = AppResult.Success(listOf(kocaeli)))
        vm.onQueryChange("Koc")
        advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        val results = (vm.uiState.value.searchState as SearchUiState.Success).results
        assertEquals(CardWeather.Unavailable, results.single().cardWeather)
    }

    @Test
    fun `oneriler icin hava durumu cekilir`() = runTest(dispatcher) {
        val vm = viewModel(weatherAutoComplete = true)
        advanceUntilIdle()
        val lists = vm.uiState.value.suggestions
        assertNotNull(lists)
        assertTrue(lists!!.country.all { it.cardWeather is CardWeather.Loaded })
    }

    @Test
    fun `rapid typing stale fetch leri iptal eder`() = runTest(dispatcher) {
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val vm = viewModel(weather = weather)
        vm.onQueryChange("Koc")
        vm.onQueryChange("İst")
        advanceUntilIdle()
        val results = (vm.uiState.value.searchState as? SearchUiState.Success)?.results
        assertNotNull(results)
        assertTrue(results!!.all { it.cardWeather != null })
    }

    @Test
    fun `mini-cache vurusu aga cikmaz`() = runTest(dispatcher) {
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val vm = viewModel(weather = weather)
        vm.onQueryChange("Koc")
        advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        val callsBefore = weather.finishedCityWeathersCalls
        vm.onQueryChange("")
        advanceUntilIdle()
        assertEquals(callsBefore, weather.finishedCityWeathersCalls)
    }

    @Test
    fun `birim degistiginde cardweather yeniden haritalanir`() = runTest(dispatcher) {
        val unitRepo = FakeTemperatureUnitRepository()
        val weather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val vm = viewModel(weather = weather, unitRepository = unitRepo)
        vm.onQueryChange("Koc")
        advanceTimeBy(AddCityViewModel.SEARCH_DEBOUNCE_MS + 1)
        runCurrent()
        unitRepo.setUnit(TemperatureUnit.FAHRENHEIT)
        advanceUntilIdle()
        val results = (vm.uiState.value.searchState as SearchUiState.Success).results
        assertTrue(results.single().cardWeather.toString().contains("°F"))
    }
}
