package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    @Test
    fun mainScreenShowsCities() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithText("Weather-chan").assertIsDisplayed()
    }

    /**
     * Boş liste yönetim ekranının bir durumudur: konum eylemi ve "Şehir ekle"
     * bu durumda görünür (spec: sehirlerim-konum-erisimi rev.4).
     */
    @Test
    fun mainScreenShowsEmptyState() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Henüz şehir yok").assertIsDisplayed()
    }

    @Test
    fun backActionInvokesCallback() {
        var backed = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    themeMode = ThemeMode.SYSTEM,
                    onBack = { backed = true },
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {}
                )
            }
        }
        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(backed)
    }

    @Test
    fun errorStateShowsMessageAndOffersRetry() {
        var retried = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Error(UiText.Dynamic("Bağlantı hatası"))),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = { retried = true },
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Bağlantı hatası").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertTrue(retried)
    }

    @Test
    fun mainScreenRestoresScrollPositionFromListState() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {},
                    listState = LazyListState(firstVisibleItemIndex = 3)
                )
            }
        }

        composeRule.onNodeWithText("Antalya").assertIsDisplayed()
    }

    @Test
    fun mainScreenRefreshesOnPullDown() {
        var refreshed = false
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {},
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = { refreshed = true }
                )
            }
        }

        composeRule.onNodeWithTag("pull_refresh").performTouchInput {
            down(center)
            moveBy(Offset(0f, 200f))
            moveBy(Offset(0f, 400f))
            moveBy(Offset(0f, 600f))
            up()
        }
        composeRule.waitForIdle()

        assert(refreshed)
    }

    @Test
    fun mainScreenKeepsContentWhileRefreshing() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers),
                        isRefreshing = true
                    ),
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {},
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {}
                )
            }
        }

        // Yenileme sırasında mevcut içerik korunur. Göstergeyi M3'ün kendi
        // IndicatorBox'ı üretir ve testTag taşıyamaz; görünürlüğü cihazda
        // doğrulanır.
        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithTag("pull_refresh").assertIsDisplayed()
    }

    @Test
    fun mainScreenSurvivesLargeFontScale() {
        composeRule.setContent {
            HavaTheme {
                CompositionLocalProvider(
                    LocalDensity provides Density(
                        density = LocalDensity.current.density,
                        fontScale = 2.0f
                    )
                ) {
                    MainScreen(
                        uiState = MainUiState(content = UiState.Success(sampleCities())),
                        themeMode = ThemeMode.SYSTEM,
                        onBack = {},
                        locationState = LocationState.Idle,
                        onAddLocation = {},
                        onRetryLocation = {},
                        onAddCityClick = {},
                        onCityClick = {},
                        onDeleteCity = {},
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
        composeRule.onNodeWithText("İstanbul").assertExists()
    }

    @Test
    fun longPressCityCardShowsDeleteConfirmationSheet() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").performTouchInput { longClick() }

        composeRule.onNodeWithText("İstanbul silinsin mi?").assertIsDisplayed()
        composeRule.onNodeWithText("Sil").assertIsDisplayed()
        composeRule.onNodeWithText("İptal").assertIsDisplayed()
    }

    @Test
    fun confirmingDeleteInvokesCallbackWithCityIdAndClosesSheet() {
        var deletedId: Long? = null
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }
        val istanbulId = syntheticCityWeathers().first().city.id

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = { deletedId = it },
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").performTouchInput { longClick() }
        composeRule.onNodeWithText("Sil").performClick()

        assertEquals(istanbulId, deletedId)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("İstanbul silinsin mi?").assertDoesNotExist()
    }

    @Test
    fun cancelingDeleteClosesSheetWithoutCallback() {
        var deleted = false
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = { deleted = true },
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").performTouchInput { longClick() }
        composeRule.onNodeWithText("İptal").performClick()

        assert(!deleted)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("İstanbul silinsin mi?").assertDoesNotExist()
    }

    private fun sampleCities() = WeatherUiMapper().let { mapper ->
        syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }
    }

    private fun unavailableCard() = WeatherUiMapper().toUnavailable(
        City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14))
    )

    private fun izmir() = City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14))

    @Test
    fun mainScreenShowsUnavailableCardWithRetryAction() {
        var retriedCityId = -1L

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(listOf(unavailableCard()))),
                    onRetryCity = { retriedCityId = it },
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İzmir").assertIsDisplayed()
        composeRule.onNodeWithText("Hava durumu alınamadı").assertIsDisplayed()
        composeRule.onNodeWithTag("city_retry_button").performClick()
        assertEquals(
            "Yeniden deneme yalnız o şehri hedeflemeli",
            izmir().id,
            retriedCityId
        )
    }

    @Test
    fun unavailableCardOpensDetail() {
        var clickedCityId = -1L

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(listOf(unavailableCard()))),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = { clickedCityId = it },
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İzmir").performClick()
        assertEquals(izmir().id, clickedCityId)
    }

    @Test
    fun unavailableCardCanBeDeletedWithLongPress() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(listOf(unavailableCard()))),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("İzmir").performTouchInput { longClick() }
        composeRule.onNodeWithText("Şehri sil").assertIsDisplayed()
    }

    @Test
    fun unavailableCardShowsProgressInsteadOfButtonWhileRetrying() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(listOf(unavailableCard())),
                        retryingCityIds = setOf(izmir().id)
                    ),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("city_retry_button").assertDoesNotExist()
        composeRule.onNodeWithTag("city_retry_spinner").assertIsDisplayed()
    }

    @Test
    fun yeniEklenenSehirVerisiGeleneKadarGostergeCizer() {
        // Tur bitmeden hata metni değil gösterge: "alınamadı", bitmemiş
        // bir işi değil başarısızlığı anlatmalıdır.
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(listOf(unavailableCard())),
                        loadingCityIds = setOf(izmir().id)
                    ),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithTag("city_retry_button").assertDoesNotExist()
        composeRule.onNodeWithTag("city_retry_spinner").assertIsDisplayed()
    }

    @Test
    fun yuklenirkenHataMetniDegilYuklemeMetniGosterir() {
        // Tur sürerken "alınamadı" yazmak yanlıştır: işlem bitmedi,
        // başarısız olmadı. Göstergeyle birlikte nötr metin çıkar.
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(listOf(unavailableCard())),
                        loadingCityIds = setOf(izmir().id)
                    ),
                    locationState = LocationState.Idle,
                    onAddLocation = {},
                    onRetryLocation = {},
                    onAddCityClick = {},
                    onCityClick = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    themeMode = ThemeMode.SYSTEM,
                    onBack = {}
                )
            }
        }

        composeRule.onNodeWithText("Hava durumu yükleniyor…").assertIsDisplayed()
        composeRule.onNodeWithText("Hava durumu alınamadı").assertDoesNotExist()
    }

    @Test
    fun bosListedeKonumSatiriVeSehirEkleGorunur() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Idle,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithText("Henüz şehir yok").assertIsDisplayed()
        composeRule.onNodeWithTag("add_location_row").assertIsDisplayed()
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
        composeRule.onNodeWithText("Şehir ekle").assertIsDisplayed()
    }

    @Test
    fun bosListedeLoadingIkenGostergeVarSatirYok() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Loading,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("location_loading").assertIsDisplayed()
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
        composeRule.onNodeWithText("Şehir ekle").assertIsDisplayed()
    }

    @Test
    fun bosListedeUnavailabledaTekrarDeneGorunur() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Unavailable,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("add_location_row").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").assertIsDisplayed()
    }

    @Test
    fun doluListedeKonumSatiriCizilmez() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map { mapper.toCityWeatherUiModel(it) }
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Idle,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
        composeRule.onNodeWithText("Şehir ekle").assertDoesNotExist()
    }

    @Test
    fun yuklemeIcerigindeKonumSatiriVeSehirEkleCizilmez() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Loading),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Unavailable,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
        composeRule.onNodeWithTag("location_loading").assertDoesNotExist()
        composeRule.onNodeWithText("Şehir ekle").assertDoesNotExist()
    }

    @Test
    fun hataIcerigindeKonumSatiriCizilmezVeTekrarDeneTek() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Error(UiText.Dynamic("Bağlantı hatası"))),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Idle,
                    onAddLocation = {}, onRetryLocation = {}, onAddCityClick = {}
                )
            }
        }
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
        composeRule.onNodeWithTag("location_loading").assertDoesNotExist()
        composeRule.onNodeWithText("Şehir ekle").assertDoesNotExist()
        // Hata metni + eylem satırdaki "Tekrar dene" ile karışmasın: tek düğme.
        assertEquals(1, composeRule.onAllNodesWithText("Tekrar dene").fetchSemanticsNodes().size)
    }

    @Test
    fun konumveSehirEkleDugmeleriTiklanincaCagriAlir() {
        var locationClicked = false
        var cityClicked = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {}, onBack = {}, onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    locationState = LocationState.Idle,
                    onAddLocation = { locationClicked = true },
                    onRetryLocation = {},
                    onAddCityClick = { cityClicked = true }
                )
            }
        }
        composeRule.onNodeWithText("Konumumu ekle").performClick()
        assertTrue(locationClicked)
        composeRule.onNodeWithText("Şehir ekle").performClick()
        assertTrue(cityClicked)
    }

    private fun syntheticCityWeathers() = listOf(
        City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)),
        City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)),
        City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14)),
        City("Antalya", "Akdeniz", "Türkiye", Coordinates(36.90, 30.70)),
        City("Trabzon", "Karadeniz", "Türkiye", Coordinates(41.00, 39.72))
    ).map { city ->
        CityWeather(
            city = city,
            currentWeather = CurrentWeather(
                temperatureCelsius = 18 + (city.id % 7).toInt(),
                condition = WeatherCondition.CLEAR,
                windSpeedKmh = 8 + (city.id % 5).toInt(),
                humidityPercent = 50 + (city.id % 20).toInt()
            )
        )
    }
}
