package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithText("Weather-chan").assertIsDisplayed()
    }

    /**
     * Boş listenin nedeni açıktır: listede ne konum ne de şehir vardır. Ekran
     * bu yüzden konum eklemeyi önerir. (Arama kutusu bu ekranda artık yok;
     * şehir arama şehir ekleme ekranında yapılır.)
     */
    @Test
    fun mainScreenShowsEmptyState() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
    }

    @Test
    fun errorStateShowsMessageAndOffersRetry() {
        var retried = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Error(UiText.Dynamic("Bağlantı hatası"))),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = { retried = true },
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM,
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = { refreshed = true },
                    onAddLocation = {},
                    onRetryLocation = {}
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {}
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
                        onCityClick = {},
                        onAddCityClick = {},
                        onSettingsClick = {}, onDeleteCity = {},
                        onRetry = {},
                        onRefresh = {},
                        onAddLocation = {},
                        onRetryLocation = {}
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = { deletedId = it },
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = { deleted = true },
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").performTouchInput { longClick() }
        composeRule.onNodeWithText("İptal").performClick()

        assert(!deleted)
        composeRule.waitForIdle()
        composeRule.onNodeWithText("İstanbul silinsin mi?").assertDoesNotExist()
    }

    @Test
    fun addCityActionInvokesCallback() {
        var opened = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(emptyList())),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {},
                    onAddCityClick = { opened = true },
                    onSettingsClick = {},
                    onDeleteCity = {},
                    onRetryCity = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    onRetry = {},
                    onRefresh = {}
                )
            }
        }
        composeRule.onNodeWithTag("action_add_city").performClick()
        assertTrue(opened)
    }

    @Test
    fun settingsActionInvokesCallback() {
        var opened = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(emptyList())),
                    themeMode = ThemeMode.SYSTEM,
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = { opened = true },
                    onDeleteCity = {},
                    onRetryCity = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    onRetry = {},
                    onRefresh = {}
                )
            }
        }
        composeRule.onNodeWithTag("action_settings").performClick()
        assertTrue(opened)
    }

    private fun sampleCities() = WeatherUiMapper().let { mapper ->
        syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }
    }

    // --- konum: liste ilk kez boş olduğunda -----------------------------------

    @Test
    fun emptyListShowsAddMyLocationAction() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
    }

    @Test
    fun addMyLocationActionInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = { clicked = true },
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").performClick()

        assertEquals("Konum ekleme geri çağrısı tetiklenmeli", true, clicked)
    }

    @Test
    fun unavailableLocationOffersRetryInsteadOfAddAction() {
        var retried = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Empty,
                        locationState = LocationState.Unavailable
                    ),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = { retried = true },
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Tekrar dene").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertEquals("Yeniden deneme geri çağrısı tetiklenmeli", true, retried)
    }

    // --- konum: liste boş değilken ------------------------------------------

    @Test
    fun locationIsReAddableWhenListIsNotEmptyAndLocationMissing() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map { mapper.toCityWeatherUiModel(it) }
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers),
                        // Konum hiç okunmamış (ya da kartı silinmiş).
                        locationState = LocationState.Idle
                    ),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        // Liste doluyken de konum geri eklenebilmeli: aksi halde kullanıcı
        // konumu yalnız listeyi boşaltarak geri getirebilirdi.
        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Konumumu ekle"))
        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
    }

    @Test
    fun unavailableLocationRowOffersRetryWhenListIsNotEmpty() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map { mapper.toCityWeatherUiModel(it) }
        var retried = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers),
                        // Konum kartı silinmiş: okunamadı, yeniden denenecek.
                        locationState = LocationState.Unavailable
                    ),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = { retried = true },
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule
            .onNode(hasScrollAction())
            .performScrollToNode(hasText("Tekrar dene"))
        composeRule.onNodeWithText("Tekrar dene").performClick()
        assertEquals("Yeniden deneme geri çağrısı tetiklenmeli", true, retried)
    }

    @Test
    fun noAddLocationRowWhenLocationIsResolved() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map { mapper.toCityWeatherUiModel(it) }
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers),
                        locationState = LocationState.Resolved
                    ),
                    onCityClick = {},
                    onAddCityClick = {},
                    onSettingsClick = {}, onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        // Konum listedeyken gereksiz "ekle" satırı gösterilmemeli.
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
    }

    /**
     * Bildirilen hata: konum kartı listedeyken "Konumumu ekle" butonu
     * görünüyordu. Sebep, satırın görünürlüğünün yalnız `locationState`'e
     * bağlanmasıydı; o durum her açılışta `Idle`/`Loading` ile başlar ve
     * yalnız o anki yenileme denemesi bitince `Resolved` olur. Konum okuma
     * + geocoding + yedek ağ turu yavaş ağda ~25 s sürdüğü için buton o
     * süre boyunca yanlışlıkla görünüyordu.
     *
     * Karar veriyor olan veridir: konum kartı ekrandaysa ekleme satırının
     * gösterileceği bir durum yoktur.
     */
    @Test
    fun noAddLocationRowWhileLocationIsIdleAndCardIsOnScreen() {
        assertNoAddLocationRow(LocationState.Idle)
    }

    @Test
    fun noAddLocationRowWhileLocationIsLoadingAndCardIsOnScreen() {
        assertNoAddLocationRow(LocationState.Loading)
    }

    @Test
    fun noAddLocationRowWhileLocationIsUnavailableAndCardIsOnScreen() {
        assertNoAddLocationRow(LocationState.Unavailable)
    }

    private fun assertNoAddLocationRow(locationState: LocationState) {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(listOf(locationCard(), unavailableCard())),
                        locationState = locationState
                    ),
                    onCityClick = {}, onAddCityClick = {}, onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
        composeRule.onNodeWithTag("add_location_row").assertDoesNotExist()
    }

    private fun locationCard() = WeatherUiMapper().toUnavailable(
        City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983), isLocation = true)
    )

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
                    onCityClick = {}, onAddCityClick = {}, onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = { clickedCityId = it },
                    onAddCityClick = {},
                    onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = {}, onAddCityClick = {}, onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
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
                    onCityClick = {}, onAddCityClick = {}, onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithTag("city_retry_button").assertDoesNotExist()
        composeRule.onNodeWithTag("city_retry_spinner").assertIsDisplayed()
    }

    /**
     * Konum kartı, normal şehir kartlarından etiketle ayrışmalı: kullanıcının
     * hangi kartın "bulunduğum yer" olduğunu isim okumadan anlaması gerekir.
     */
    @Test
    fun locationCardShowsLocationLabel() {
        val mapper = WeatherUiMapper()
        val locationCard = mapper.toCityWeatherUiModel(
            CityWeather(
                city = City("Kucukpazar", "Istanbul", "Turkey", Coordinates(41.0, 28.983), isLocation = true),
                currentWeather = CurrentWeather(
                    temperatureCelsius = 21,
                    condition = WeatherCondition.CLEAR,
                    windSpeedKmh = 8,
                    humidityPercent = 50
                )
            )
        )
        val regularCard = mapper.toCityWeatherUiModel(
            CityWeather(
                city = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)),
                currentWeather = CurrentWeather(
                    temperatureCelsius = 14,
                    condition = WeatherCondition.CLOUDY,
                    windSpeedKmh = 6,
                    humidityPercent = 45
                )
            )
        )

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(listOf(locationCard, regularCard))
                    ),
                    onCityClick = {}, onAddCityClick = {}, onSettingsClick = {},
                    onDeleteCity = {}, onRetry = {}, onRefresh = {},
                    onAddLocation = {}, onRetryLocation = {},
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("📍 Konumunuz").assertIsDisplayed()
        assertEquals(
            "Etiket yalnız konum kartında olmalı",
            1,
            composeRule.onAllNodesWithText("📍 Konumunuz").fetchSemanticsNodes().size
        )
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
