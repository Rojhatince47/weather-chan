package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Density
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import org.junit.Assert.assertEquals
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithText("Şehir ara").assertIsDisplayed()
    }

    /**
     * Boş listenin nedeni "sonuç bulunamadı" değildir: liste, içinde ne konum
     * ne de şehir bulunmadığı için boştur. Ekran bu yüzden konum eklemeyi
     * önerir ve arama kutusu durur.
     */
    @Test
    fun mainScreenShowsEmptyState() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
        composeRule.onNodeWithText("Sonuç bulunamadı").assertDoesNotExist()
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    listState = LazyListState(firstVisibleItemIndex = 3)
                )
            }
        }

        composeRule.onNodeWithText("Antalya").assertIsDisplayed()
    }

    @Test
    fun mainScreenShowsUnitToggleAndNotifiesClick() {
        var toggled = false
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = { toggled = true },
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("°C").performClick()
        assert(toggled)
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
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = { refreshed = true },
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
                        unit = TemperatureUnit.CELSIUS.toUiModel(),
                        themeMode = ThemeMode.SYSTEM,
                        onQueryChange = {},
                        onCityClick = {},
                        onAddCity = {},
                        onOpenCity = {},
                        onDeleteCity = {},
                        onRetry = {},
                        onRefresh = {},
                        onToggleUnit = {},
                        onToggleThemeMode = {},
                        onAddLocation = {},
                        onRetryLocation = {}
                    )
                }
            }
        }
        composeRule.onNodeWithText("İstanbul").assertExists()
    }

    @Test
    fun themeToggleCyclesThroughAllModes() {
        val themeMode = mutableStateOf(ThemeMode.SYSTEM)

        composeRule.setContent {
            val current = themeMode.value
            HavaTheme(themeMode = current) {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = current,
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = { themeMode.value = current.next() },
                    onAddLocation = {},
                    onRetryLocation = {}
                )
            }
        }

        composeRule.onNodeWithContentDescription("Tema: Sistem").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tema: Sistem").performClick()
        composeRule.onNodeWithContentDescription("Tema: Açık").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tema: Açık").performClick()
        composeRule.onNodeWithContentDescription("Tema: Koyu").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Tema: Koyu").performClick()
        composeRule.onNodeWithContentDescription("Tema: Sistem").assertIsDisplayed()
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = { deletedId = it },
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = { deleted = true },
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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

    private fun sampleCities() = WeatherUiMapper().let { mapper ->
        syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it)
        }
    }

    /**
     * Regresyon: arama kutusu iki iş yapar — yeni şehir arar ve (ayrıca) kayıtlı
     * listeyi filtreler. Kullanıcı "Paris" yazıp eklemek istediğinde kayıtlı
     * hiçbir şehir tutmaz, liste `Empty` olur ve ekran "Sonuç bulunamadı" der —
     * oysa aynı anda canlı arama sonuçları listelenmektedir. Kullanıcı hem
     * sonuçları hem "sonuç bulunamadı" görür; mesaj kayıtlı listeyi anlatır,
     * aramayı değil.
     */
    @Test
    fun searchingDoesNotShowListEmptyMessageAlongsideResults() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        query = "Par",
                        content = UiState.Empty,
                        searchState = SearchUiState.Success(
                            listOf(
                                SearchResultUiModel(
                                    city = City("Paris", "Île-de-France", "Fransa", Coordinates(48.85, 2.35)),
                                    isInList = false
                                )
                            )
                        )
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Paris").assertIsDisplayed()
        composeRule.onNodeWithText("Sonuç bulunamadı").assertDoesNotExist()
    }

    /**
     * Arama gerçekten sonuç döndürmediyse kullanıcı bunu bilmeli — ama liste
     * boşluğunun mesajıyla değil, aramanın kendi mesajıyla.
     */
    @Test
    fun searchingWithNoMatchesShowsSearchMessage() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        query = "Zzzz",
                        content = UiState.Empty,
                        searchState = SearchUiState.Success(emptyList())
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Aramanızla eşleşen şehir bulunamadı").assertIsDisplayed()
        composeRule.onNodeWithText("Sonuç bulunamadı").assertDoesNotExist()
    }

    /**
     * Arama sürerken liste boşsa ekran "sonuç bulunamadı" demez: henüz
     * cevap dönmedi, henüz bir şey bilinmiyor.
     */
    @Test
    fun searchingInProgressDoesNotShowAnyEmptyMessage() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        query = "Par",
                        content = UiState.Empty,
                        searchState = SearchUiState.Loading
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Sonuç bulunamadı").assertDoesNotExist()
        composeRule.onNodeWithText("Aramanızla eşleşen şehir bulunamadı").assertDoesNotExist()
    }

    // --- konum: liste ilk kez boş olduğunda -----------------------------------

    @Test
    fun emptyListShowsAddMyLocationAction() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Konumumu ekle").assertIsDisplayed()
    }

    @Test
    fun emptyListShowsSearchHintAlongsideLocationAction() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        // Kullanıcı konum ekleyemezse aramayla şehir bulabilmeli.
        composeRule.onNodeWithText("Şehir ara").assertIsDisplayed()
    }

    @Test
    fun addMyLocationActionInvokesCallback() {
        var clicked = false
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = { clicked = true },
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = { retried = true },
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = { retried = true },
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    onQueryChange = {},
                    onCityClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onAddLocation = {},
                    onRetryLocation = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        // Konum listedeyken gereksiz "ekle" satırı gösterilmemeli.
        composeRule.onNodeWithText("Konumumu ekle").assertDoesNotExist()
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
