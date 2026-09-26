package io.github.rojhatince47.weatherchan.feature.settings.presentation

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
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
class SettingsViewModelTest {
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
    fun `initial state is celsius`() = runTest {
        val viewModel = SettingsViewModel(InMemoryTemperatureUnitRepository())

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals(
                TemperatureUnit.CELSIUS,
                (state as UiState.Success).data.unit
            )
        }
    }

    @Test
    fun `toggle switches between celsius and fahrenheit`() = runTest {
        val viewModel = SettingsViewModel(InMemoryTemperatureUnitRepository())

        viewModel.uiState.test {
            assertEquals(
                TemperatureUnit.CELSIUS,
                (awaitItem() as UiState.Success).data.unit
            )

            viewModel.onToggleUnit()
            assertEquals(
                TemperatureUnit.FAHRENHEIT,
                (awaitItem() as UiState.Success).data.unit
            )

            viewModel.onToggleUnit()
            assertEquals(
                TemperatureUnit.CELSIUS,
                (awaitItem() as UiState.Success).data.unit
            )
        }
    }

    @Test
    fun `selecting a unit emits it`() = runTest {
        val viewModel = SettingsViewModel(InMemoryTemperatureUnitRepository())

        viewModel.uiState.test {
            awaitItem()
            viewModel.onUnitSelected(TemperatureUnit.FAHRENHEIT)

            val state = awaitItem()
            assertTrue(state is UiState.Success)
            assertEquals(
                TemperatureUnit.FAHRENHEIT,
                (state as UiState.Success).data.unit
            )
        }
    }
}
