package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
class EvSayfasiViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun city(name: String, lat: Double, isLocation: Boolean = false) =
        City(
            name = name,
            region = "",
            country = "",
            coordinates = Coordinates(lat, 28.0),
            isLocation = isLocation
        )

    private fun evViewModel(
        fakeWeather: FakeWeatherRepository = FakeWeatherRepository().apply { autoCompleteGates = true },
        fakeCities: FakeCityRepository = FakeCityRepository(),
        unitRepository: FakeTemperatureUnitRepository = FakeTemperatureUnitRepository()
    ) = EvSayfasiViewModel(
        cityRepository = fakeCities,
        getForecast = GetForecastUseCase(fakeWeather),
        unitRepository = unitRepository,
        uiMapper = WeatherUiMapper()
    )

    @Test
    fun `acilista sayfalar konum birinci sirayla yayinlanir`() = runTest(dispatcher) {
        val location = city("Konum", 10.0, isLocation = true)
        val first = city("İstanbul", 41.0)
        val second = city("Ankara", 39.0)
        val viewModel = evViewModel(
            fakeCities = FakeCityRepository(initial = listOf(first, location, second))
        )
        runCurrent()
        val pages = viewModel.uiState.value.pages
        assertEquals(3, pages.size)
        assertTrue(pages[0].isLocation)
        assertEquals(first.name, pages[1].name)
        assertEquals(second.name, pages[2].name)
    }

    @Test
    fun `bos depo bos sayfa listesi yayinlar`() = runTest(dispatcher) {
        val viewModel = evViewModel(fakeCities = FakeCityRepository(initial = emptyList()))
        runCurrent()
        assertTrue(viewModel.uiState.value.pages.isEmpty())
    }

    @Test
    fun `gorunur pencere disindaki sayfalar aga cikmaz`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        evViewModel(fakeWeather = fakeWeather)
        runCurrent()
        // Varsayılan 5 şehir; sayfa 0'da pencere {0, 1} → yalnız 2 istek.
        assertEquals(2, fakeWeather.forecastRequests.size)
    }

    @Test
    fun `sayfa degisince yeni pencere cekilir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply { autoCompleteGates = true }
        val viewModel = evViewModel(fakeWeather = fakeWeather)
        runCurrent()
        fakeWeather.forecastRequests.clear()
        viewModel.onPageChanged(3)
        runCurrent()
        // Sayfa 3'te pencere {2, 3, 4}.
        assertEquals(3, fakeWeather.forecastRequests.size)
    }

    @Test
    fun `selectCity sayfayi secer`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        val target = fakeCities.current()[3]
        viewModel.selectCity(target.id)
        assertEquals(3, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `sayfa sayisi kuculunce mevcut sayfa sinirlanir`() = runTest(dispatcher) {
        val first = city("İstanbul", 41.0)
        val second = city("Ankara", 39.0)
        val fakeCities = FakeCityRepository(initial = listOf(first, second))
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        viewModel.selectCity(second.id)
        assertEquals(1, viewModel.uiState.value.currentPage)
        fakeCities.remove(second.id)
        runCurrent()
        assertEquals(0, viewModel.uiState.value.currentPage)
    }

    @Test
    fun `birim degisince icerik yeniden eslenir`() = runTest(dispatcher) {
        val unitRepository = FakeTemperatureUnitRepository()
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(
            fakeCities = fakeCities,
            unitRepository = unitRepository
        )
        runCurrent()
        val cityId = fakeCities.current().first().id
        val before = viewModel.uiState.value.forecasts[cityId]?.content
        assertTrue(before is UiState.Success)
        val celsiusText = (before as UiState.Success).data.currentWeather.temperatureText
        unitRepository.setUnit(TemperatureUnit.FAHRENHEIT)
        runCurrent()
        val after = viewModel.uiState.value.forecasts[cityId]?.content
        assertTrue(after is UiState.Success)
        val fahrenheitText = (after as UiState.Success).data.currentWeather.temperatureText
        assertTrue(celsiusText != fahrenheitText)
    }

    @Test
    fun `tahmin hatasi UiState Error olarak yayinlanir`() = runTest(dispatcher) {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            failForecast = true
        }
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeWeather = fakeWeather, fakeCities = fakeCities)
        runCurrent()
        val cityId = fakeCities.current().first().id
        assertTrue(viewModel.uiState.value.forecasts[cityId]?.content is UiState.Error)
    }

    @Test
    fun `uiState ilk degerde sayfalar ve tahminler birlikte tasir`() = runTest(dispatcher) {
        val fakeCities = FakeCityRepository()
        val viewModel = evViewModel(fakeCities = fakeCities)
        runCurrent()
        val state = viewModel.uiState.value
        assertEquals(fakeCities.current().size, state.pages.size)
        assertTrue(state.forecasts.isNotEmpty())
    }
}
