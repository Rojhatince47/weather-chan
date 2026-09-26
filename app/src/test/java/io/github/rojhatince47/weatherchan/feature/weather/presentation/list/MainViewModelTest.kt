package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.common.error.DefaultErrorMapper
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.favorites.data.repository.InMemoryFavoriteRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
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
class MainViewModelTest {
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
    fun `initial state is success with cities`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
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
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
        )

        viewModel.uiState.test {
            assertTrue(awaitItem().content is UiState.Loading)

            fakeRepository.loadGate.complete(Unit)
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
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
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

    @Test
    fun `empty search shows empty state`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
        )

        viewModel.onQueryChange("zzz")

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Empty)
        }
    }

    @Test
    fun `unit change from shared source converts temperatures to fahrenheit`() = runTest {
        val unitRepository = InMemoryTemperatureUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            favoriteRepository = InMemoryFavoriteRepository()
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
        val unitRepository = InMemoryTemperatureUnitRepository()
        val settingsViewModel = SettingsViewModel(unitRepository)
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            favoriteRepository = InMemoryFavoriteRepository()
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
    fun `favorite selection is shared between list and detail view models`() = runTest {
        val favoriteRepository = InMemoryFavoriteRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = favoriteRepository
        )
        val detailViewModel = DetailViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(
                    "cityId" to 1L,
                    "name" to "İstanbul",
                    "region" to "Marmara",
                    "country" to "Türkiye",
                    "latitude" to 41.01,
                    "longitude" to 28.98
                )
            ),
            getForecast = GetForecastUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = favoriteRepository
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertTrue(
                ((initial.content as UiState.Success).data.first().isFavorite).not()
            )

            viewModel.onFavoriteClick(1L)
            val selected = awaitItem()
            assertTrue(
                ((selected.content as UiState.Success).data.first().isFavorite)
            )
        }

        detailViewModel.uiState.test {
            val detailState = awaitItem()
            assertTrue(detailState.content is UiState.Success)
            assertTrue(
                ((detailState.content as UiState.Success).data.isFavorite)
            )
        }
    }

    @Test
    fun `favorites only filter derives list from existing state`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource(), DefaultErrorMapper())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
        )

        viewModel.uiState.test {
            val initial = awaitItem()
            assertEquals(
                5,
                ((initial.content as UiState.Success).data.size)
            )

            viewModel.onFavoriteClick(2L)
            awaitItem()

            viewModel.onFavoritesOnlyChange(true)
            val filtered = awaitItem()
            assertTrue(filtered.showFavoritesOnly)
            val data = (filtered.content as UiState.Success).data
            assertEquals(1, data.size)
            assertEquals(2L, data.first().cityId)

            viewModel.onFavoritesOnlyChange(false)
            val restored = awaitItem()
            assertEquals(
                5,
                ((restored.content as UiState.Success).data.size)
            )
        }
    }
}
