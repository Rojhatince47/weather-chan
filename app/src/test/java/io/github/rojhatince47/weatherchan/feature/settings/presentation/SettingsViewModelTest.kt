package io.github.rojhatince47.weatherchan.feature.settings.presentation

import app.cash.turbine.test
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.notifications.testing.FakeNotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.SoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeSoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeThemeModeRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    private fun newViewModel(
        applier: AppLanguageApplier = FakeAppLanguageApplier(),
        soundEffects: SoundEffectsRepository = FakeSoundEffectsRepository(),
        notifications: NotificationSettingsRepository = FakeNotificationSettingsRepository()
    ): SettingsViewModel = SettingsViewModel(
        FakeTemperatureUnitRepository(),
        FakeThemeModeRepository(),
        applier,
        soundEffects,
        notifications
    )

    // --- dil ---

    private class FakeAppLanguageApplier : AppLanguageApplier {
        var applied: AppLanguage? = null
        override var current: AppLanguage = AppLanguage.SYSTEM
        override fun apply(language: AppLanguage) {
            applied = language
            current = language
        }
    }

    @Test
    fun `dil secimi appliera tasinir`() = runTest {
        val applier = FakeAppLanguageApplier()
        val viewModel = newViewModel(applier = applier)
        viewModel.onAppLanguageSelected(AppLanguage.ENGLISH)
        assertEquals(AppLanguage.ENGLISH, applier.applied)
        assertEquals(AppLanguage.ENGLISH, viewModel.appLanguage.value)
    }

    @Test
    fun `dil durumu dis kaynak degisince tazelenir`() = runTest {
        val applier = FakeAppLanguageApplier()
        val viewModel = newViewModel(applier = applier)
        applier.current = AppLanguage.TURKISH // API 33+ sistem ayarlarından değişim
        viewModel.refreshAppLanguage()
        assertEquals(AppLanguage.TURKISH, viewModel.appLanguage.value)
    }

    // --- ses efektleri ---

    @Test
    fun `ses efektleri varsayilan olarak acik`() = runTest {
        val viewModel = newViewModel()

        assertTrue(viewModel.soundEffectsEnabled.value)
    }

    @Test
    fun `ses efektleri kapatilinca akista yayilir`() = runTest {
        val viewModel = newViewModel()

        viewModel.soundEffectsEnabled.test {
            assertTrue(awaitItem())
            viewModel.onSoundEffectsEnabledChanged(false)
            assertFalse(awaitItem())
        }
    }

    // --- bildirimler ---

    @Test
    fun `bildirimler varsayilan olarak acik`() = runTest {
        val viewModel = newViewModel()

        assertTrue(viewModel.notificationSettings.value.masterEnabled)
    }

    @Test
    fun `bildirim anahtari ve tur secimi akisa yayilir`() = runTest {
        val viewModel = newViewModel()

        viewModel.notificationSettings.test {
            assertTrue(awaitItem().masterEnabled)

            viewModel.onNotificationTypeChanged(NotificationType.RAIN_SOON, false)
            assertFalse(awaitItem().typeEnabled.getValue(NotificationType.RAIN_SOON))

            viewModel.onNotificationMasterChanged(false)
            assertFalse(awaitItem().masterEnabled)
        }
    }
}
