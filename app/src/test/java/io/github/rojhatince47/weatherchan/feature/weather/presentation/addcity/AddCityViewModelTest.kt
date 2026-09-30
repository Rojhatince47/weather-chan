package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySuggestionProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.DeviceCountryProvider
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionData
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.location.LocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
        countryCode: String? = "TR",
        searchResult: AppResult<List<City>> = AppResult.Success(listOf(kocaeli))
    ): AddCityViewModel {
        weather.searchResult = searchResult
        return AddCityViewModel(
            searchCities = SearchCitiesUseCase(weather),
            suggestCities = SuggestCitiesUseCase(
                suggestionProvider = CitySuggestionProvider { suggestions },
                locationProvider = LocationProvider { coordinates },
                deviceCountryProvider = DeviceCountryProvider { countryCode }
            ),
            cityRepository = cities
        )
    }

    @Test
    fun `acilista oneriler yuklenir`() = runTest(dispatcher) {
        viewModel().uiState.test {
            val state = awaitItem()
            assertEquals(listOf("Kocaeli", "Bursa"), state.suggestions?.suggestions?.map { it.city.name })
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
        // Debounce gerçek bir gecikmedir: `onQueryChange` anında yalnızca
        // sorgu güncellenir, ağ turu 300 ms sonra başlar. Repo deseni:
        // `MainViewModelTest` → `live search fires after debounce`.
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
        // `search()` yalnız debounce bittikten sonra çağrılır; iptal edilen
        // ilk iş hiç depoya ulaşmaz. Depoya düşen sorgular bu yüzden
        // yalnız "Koca" olmalıdır.
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
    fun `sehir eklenince oneri listesinden duser`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = emptyList())
        val vm = viewModel(cities = cities)
        vm.uiState.test {
            awaitItem()
            vm.onAddCity(kocaeli)
            assertEquals(listOf("Bursa"), expectMostRecentItem().suggestions?.suggestions?.map { it.city.name })
        }
    }

    @Test
    fun `eklenen sehir arama sonucunda listeye gecer`() = runTest(dispatcher) {
        // Bayrak bayat kalırsa kullanıcı aynı şehri iki kez eklemeye çalışır;
        // `MainViewModelTest` → `adding a city flips its in-list flag` muadili.
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
            assertEquals(listOf("Kocaeli", "Bursa"), state.suggestions?.suggestions?.map { it.city.name })
        }
    }

    @Test
    fun `zaten listede olan sehir oneri olarak gelmez`() = runTest(dispatcher) {
        val cities = FakeCityRepository(initial = listOf(kocaeli))
        viewModel(cities = cities).uiState.test {
            assertEquals(listOf("Bursa"), awaitItem().suggestions?.suggestions?.map { it.city.name })
        }
    }
}
