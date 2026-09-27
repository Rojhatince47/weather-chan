package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class InMemoryThemeModeRepositoryTest {

    @Test
    fun `initial mode is system`() = runTest {
        val repository = InMemoryThemeModeRepository()
        assertEquals(ThemeMode.SYSTEM, repository.themeMode.value)
    }

    @Test
    fun `set mode emits new value`() = runTest {
        val repository = InMemoryThemeModeRepository()

        repository.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())
        }
    }

    @Test
    fun `two instances do not share state`() = runTest {
        val first = InMemoryThemeModeRepository()
        val second = InMemoryThemeModeRepository()

        first.setThemeMode(ThemeMode.LIGHT)

        assertEquals(ThemeMode.SYSTEM, second.themeMode.value)
    }
}
