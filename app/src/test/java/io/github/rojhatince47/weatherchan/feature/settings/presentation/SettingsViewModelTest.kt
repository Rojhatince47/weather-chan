package io.github.rojhatince47.weatherchan.feature.settings.presentation

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.data.repository.InMemoryThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
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
        val viewModel = newViewModel()

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
        val viewModel = newViewModel()

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
        val viewModel = newViewModel()

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

    @Test
    fun `initial theme mode is system`() = runTest {
        val viewModel = newViewModel()

        viewModel.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `cycle theme mode moves system to light to dark`() = runTest {
        val viewModel = newViewModel()

        viewModel.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())

            viewModel.onCycleThemeMode()
            assertEquals(ThemeMode.LIGHT, awaitItem())

            viewModel.onCycleThemeMode()
            assertEquals(ThemeMode.DARK, awaitItem())

            viewModel.onCycleThemeMode()
            assertEquals(ThemeMode.SYSTEM, awaitItem())
        }
    }

    @Test
    fun `theme selection emits selected mode`() = runTest {
        val viewModel = newViewModel()

        viewModel.themeMode.test {
            awaitItem()
            viewModel.onThemeModeSelected(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    private fun newViewModel(): SettingsViewModel = SettingsViewModel(
        InMemoryTemperatureUnitRepository(),
        InMemoryThemeModeRepository()
    )
}
