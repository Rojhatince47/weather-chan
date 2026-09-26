package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.favorites.data.repository.InMemoryFavoriteRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
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
            getForecast = GetForecastUseCase(WeatherRepositoryImpl(SampleWeatherDataSource())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
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
            getForecast = GetForecastUseCase(WeatherRepositoryImpl(SampleWeatherDataSource())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
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
            getForecast = GetForecastUseCase(WeatherRepositoryImpl(SampleWeatherDataSource())),
            uiMapper = WeatherUiMapper(),
            unitRepository = InMemoryTemperatureUnitRepository(),
            favoriteRepository = InMemoryFavoriteRepository()
        )

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state.content is UiState.Error)
        }
    }
}
