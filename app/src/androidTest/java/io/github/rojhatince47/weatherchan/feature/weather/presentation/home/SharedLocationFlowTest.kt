package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.testing.FakeCityRepository
import io.github.rojhatince47.weatherchan.feature.notifications.testing.FakeNotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsCitiesRoute
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeAppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeSoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeTemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.testing.FakeThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.WeatherAdviceEngine
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedForecastsUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetForecastUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetLocationCityUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.MainRoute
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.MainViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeLocationProvider
import io.github.rojhatince47.weatherchan.feature.weather.testing.FakeWeatherRepository
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import java.io.FileInputStream
import kotlinx.coroutines.CompletableDeferred
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
            uiMapper = WeatherUiMapper(WeatherAdviceEngine()),
            getLocationCity = GetLocationCityUseCase(FakeLocationProvider(Coordinates(39.20, 30.10)), fakeWeather)
        )
        val settingsViewModel = SettingsViewModel(
            repository = FakeTemperatureUnitRepository(),
            themeModeRepository = FakeThemeModeRepository(),
            appLanguageApplier = FakeAppLanguageApplier(),
            soundEffectsRepository = FakeSoundEffectsRepository(),
            notificationSettingsRepository = FakeNotificationSettingsRepository()
        )

        composeRule.setContent {
            HavaTheme {
                Column {
                    // Ayarlar üstte (tıklanabilir); Ev aşağıda (assertExists —
                    // kırpılsa da semantik ağacı korunur).
                    SettingsCitiesRoute(
                        onBack = {},
                        onOpenCityList = {},
                        evViewModel = sharedViewModel
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

    @Test
    fun sehirlerimdenEklenenKonumUcRouteTarafindanPaylasilir() {
        val fakeWeather = FakeWeatherRepository().apply {
            autoCompleteGates = true
            searchResult = AppResult.Success(listOf(testKoy))
        }
        // Boş başlangıç şart: FakeCityRepository() default'u 5 örnek şehir.
        val fakeCities = FakeCityRepository(initial = emptyList())
        // Gate: refreshLocation Loading→Resolved'ı tek coroutine'de yazar;
        // gatesiz akışta Loading asla gözlemlenemez. Gate açık kalır → Loading.
        val gate = CompletableDeferred<Coordinates?>()
        val sharedViewModel = EvSayfasiViewModel(
            cityRepository = fakeCities,
            getForecast = GetForecastUseCase(fakeWeather),
            getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
            unitRepository = FakeTemperatureUnitRepository(),
            uiMapper = WeatherUiMapper(WeatherAdviceEngine()),
            getLocationCity = GetLocationCityUseCase(
                FakeLocationProvider(Coordinates(39.20, 30.10), gate),
                fakeWeather
            )
        )
        val settingsViewModel = SettingsViewModel(
            repository = FakeTemperatureUnitRepository(),
            themeModeRepository = FakeThemeModeRepository(),
            appLanguageApplier = FakeAppLanguageApplier(),
            soundEffectsRepository = FakeSoundEffectsRepository(),
            notificationSettingsRepository = FakeNotificationSettingsRepository()
        )
        // MainViewModel, AYNI FakeCityRepository örneğine bağlı: kart düşmesi gözlemlenir.
        val mainViewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(fakeWeather),
            getCachedForecasts = GetCachedForecastsUseCase(fakeWeather),
            uiMapper = WeatherUiMapper(WeatherAdviceEngine()),
            unitRepository = FakeTemperatureUnitRepository(),
            cityRepository = fakeCities
        )

        var showSettings by mutableStateOf(false)
        composeRule.setContent {
            HavaTheme {
                // Route'lar aynı anda MOUNT EDİLMEZ: aynı düğümler AmbiguousNodes
                // fırlatırdı. Tıklanacak olan açıktır.
                if (showSettings) {
                    SettingsCitiesRoute(
                        onBack = {},
                        onOpenCityList = {},
                        evViewModel = sharedViewModel
                    )
                } else {
                    MainRoute(
                        onSelectCity = {},
                        onBack = {},
                        evViewModel = sharedViewModel,
                        onAddCityClick = {},
                        viewModel = mainViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }

        // İçerik Loading→Empty'ye iki asenkron hop ile geçer.
        composeRule.waitUntil(5_000) { mainViewModel.uiState.value.content is UiState.Empty }
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").performClick()
        composeRule.waitUntil(5_000) {
            sharedViewModel.uiState.value.locationState == LocationState.Loading
        }

        composeRule.runOnUiThread { showSettings = true }
        composeRule.waitForIdle()
        composeRule.onNodeWithText("Konum").assertIsDisplayed()
        composeRule.onNodeWithText("Konum okunuyor…").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()

        gate.complete(Coordinates(39.20, 30.10))
        composeRule.waitUntil(5_000) {
            sharedViewModel.uiState.value.locationState == LocationState.Resolved
        }
        composeRule.onNodeWithText("Konum").assertDoesNotExist()

        composeRule.runOnUiThread { showSettings = false }
        composeRule.waitForIdle()
        composeRule.waitUntil(5_000) { mainViewModel.uiState.value.content is UiState.Success }
        composeRule.onNodeWithText("TestKöy").assertIsDisplayed()
        assertTrue(fakeCities.current().any { it.isLocation })
    }
}
