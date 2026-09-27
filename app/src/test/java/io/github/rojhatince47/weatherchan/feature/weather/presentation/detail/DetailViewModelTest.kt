package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.LoadState
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DetailViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loads forecast for selected city`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "cityId" to 1L,
                "name" to "İstanbul",
                "region" to "Marmara",
                "country" to "Türkiye",
                "latitude" to 41.01,
                "longitude" to 28.98
            )
        )

        val viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getForecast = GetForecastUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Success)
        }
    }

    @Test
    fun `missing navigation params shows error instead of crashing`() = runTest {
        val viewModel = DetailViewModel(
            savedStateHandle = SavedStateHandle(),
            getForecast = GetForecastUseCase(FakeWeatherRepository().apply { autoCompleteGates = true }),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Error)
        }
    }

    @Test
    fun `unknown city id shows error`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "cityId" to 999L,
                "name" to "Bilinmeyen",
                "region" to null,
                "country" to null,
                "latitude" to 0.0,
                "longitude" to 0.0
            )
        )

        val viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getForecast = GetForecastUseCase(
                FakeWeatherRepository().apply {
                    autoCompleteGates = true
                    failForecast = true
                }
            ),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Error)
        }
    }

    @Test
    fun `loadData emits error state for unknown city`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "cityId" to 1L,
                "name" to "İstanbul",
                "region" to "Marmara",
                "country" to "Türkiye",
                "latitude" to 41.01,
                "longitude" to 28.98
            )
        )
        val fakeRepository = FakeWeatherRepository().apply {
            failForecast = true
        }

        val viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getForecast = GetForecastUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.forecastGate.complete(Unit)
            assertTrue(awaitItem() is LoadState.Error)
        }

        fakeRepository.forecastGate = CompletableDeferred()
        viewModel.loadData()
        viewModel.loadState.test {
            assertEquals(LoadState.Loading, awaitItem())
            fakeRepository.forecastGate.complete(Unit)
            assertTrue(awaitItem() is LoadState.Error)
        }
    }

    @Test
    fun `refresh keeps content and toggles refreshing flag`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "cityId" to 1L,
                "name" to "İstanbul",
                "region" to "Marmara",
                "country" to "Türkiye",
                "latitude" to 41.01,
                "longitude" to 28.98
            )
        )
        val fakeRepository = FakeWeatherRepository()

        val viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getForecast = GetForecastUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)
            fakeRepository.forecastGate.complete(Unit)
            assertTrue(awaitItem().content is UiState.Success)
        }

        fakeRepository.forecastGate = CompletableDeferred()
        viewModel.onRefresh()
        viewModel.uiState.test {
            val refreshing = awaitItem()
            assertTrue(refreshing.isRefreshing)
            assertTrue(refreshing.content is UiState.Success)

            fakeRepository.forecastGate.complete(Unit)
            val refreshed = awaitItem()
            assertTrue(refreshed.isRefreshing.not())
            assertTrue(refreshed.content is UiState.Success)
        }
    }

    @Test
    fun `failed refresh keeps old content and emits snackbar event`() = runTest {
        val savedStateHandle = SavedStateHandle(
            mapOf(
                "cityId" to 1L,
                "name" to "İstanbul",
                "region" to "Marmara",
                "country" to "Türkiye",
                "latitude" to 41.01,
                "longitude" to 28.98
            )
        )
        val fakeRepository = FakeWeatherRepository()

        val viewModel = DetailViewModel(
            savedStateHandle = savedStateHandle,
            getForecast = GetForecastUseCase(fakeRepository),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            cityRepository = FakeCityRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)
            fakeRepository.forecastGate.complete(Unit)
            assertTrue(awaitItem().content is UiState.Success)
        }

        fakeRepository.failForecast = true
        fakeRepository.forecastGate = CompletableDeferred()
        viewModel.onRefresh()
        viewModel.uiState.test {
            val refreshing = awaitItem()
            assertTrue(refreshing.isRefreshing)
            assertTrue(refreshing.content is UiState.Success)

            fakeRepository.forecastGate.complete(Unit)
            val failed = awaitItem()
            assertTrue(failed.isRefreshing.not())
            assertTrue(failed.content is UiState.Success)
        }

        viewModel.events.test {
            val event = awaitItem()
            assertTrue(event is DetailEvent.ShowSnackbar)
        }
    }
}
