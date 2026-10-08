package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsRoute
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeAppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedForecastsUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeLocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import java.io.FileInputStream
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test

/**
 * Tek `EvSayfasiViewModel` örneğinin Ev ve Ayarlar route'ları tarafından
 * paylaşıldığını sınar: Ayarlar'dan konum eklenir, bölüm canlı gizlenir,
 * Ev route'u aynı state'i görür.
 */
class SharedLocationFlowTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
    private val testKoy = City("TestKöy", null, "Türkiye", Coordinates(39.20, 30.10))

    @Before
    fun grantLocation() {
        val packageName = InstrumentationRegistry.getInstrumentation().targetContext.packageName
        val uiAutomation = InstrumentationRegistry.getInstrumentation().uiAutomation
        listOf(
            "pm grant $packageName android.permission.ACCESS_FINE_LOCATION",
            "pm grant $packageName android.permission.ACCESS_COARSE_LOCATION",
            "appops set $packageName android:fine_location allow",
            "appops set $packageName android:coarse_location allow"
        ).forEach { cmd ->
            uiAutomation.executeShellCommand(cmd).use { pfd ->
                FileInputStream(pfd.fileDescriptor).use { it.readBytes() }
            }
        }
    }

    @Test
    fun ayarlardanEklenenKonumIkiRouteTarindanGorulur() {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Success(listOf(testKoy))
        }
        val fakeCities = FakeCityRepository(initial = listOf(istanbul))
        // cityPermissions = null BİLİNÇLİ: `restoreLocationPermissionState`
        // erken döner → permissionState `Restoring` kalır → hook otomatik
        // diyalog AÇMAZ ve init'te otomatik refreshLocation ÇALIŞMAZ. Test
        // deterministik kalır: Idle → tıkla → (@Before pm grant ile izin
        // zaten verili) ReadLocation → Loading → Resolved.
        val sharedViewModel = EvSayfasiViewModel(
            cityRepository = fakeCities,
            getForecast = GetForecastUseCase(fakeWeather),
            getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
            unitRepository = FakeTemperatureUnitRepository(),
            uiMapper = WeatherUiMapper(),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(Coordinates(39.20, 30.10)), fakeWeather)
        )
        val settingsViewModel = SettingsViewModel(
            repository = FakeTemperatureUnitRepository(),
            themeModeRepository = FakeThemeModeRepository(),
            appLanguageApplier = FakeAppLanguageApplier()
        )

        composeRule.setContent {
            HavaTheme {
                Column {
                    // Ayarlar üstte (tıklanabilir); Ev aşağıda (assertExists —
                    // kırpılsa da semantik ağacı korunur).
                    SettingsRoute(
                        onBack = {},
                        evViewModel = sharedViewModel,
                        settingsViewModel = settingsViewModel
                    )
                    EvSayfasiRoute(
                        onAddCityClick = {},
                        onSettingsClick = {},
                        onOpenList = {},
                        viewModel = sharedViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }

        composeRule.waitUntil(5_000) { sharedViewModel.uiState.value.pages.isNotEmpty() }
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()

        composeRule.onNodeWithText("Konumumu ekle").performClick()

        composeRule.waitUntil(5_000) {
            sharedViewModel.uiState.value.locationState == LocationState.Resolved
        }
        // Ayarlar route'u aynı örneği görür: bölüm canlı gizlenir.
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
        // Ev route'u aynı örneği görür: konum sayfası başlıkta.
        composeRule.waitUntil(5_000) { sharedViewModel.uiState.value.pages.any { it.isLocation } }
        composeRule.onNodeWithText("TestKöy").assertExists()
        assertTrue(fakeCities.current().any { it.isLocation })
    }
}
