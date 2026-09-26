package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.repository.WeatherRepositoryImpl
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
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
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource())),
            uiMapper = WeatherUiMapper()
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
    fun `search filters cities`() = runTest {
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(WeatherRepositoryImpl(SampleWeatherDataSource())),
            uiMapper = WeatherUiMapper()
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
}
