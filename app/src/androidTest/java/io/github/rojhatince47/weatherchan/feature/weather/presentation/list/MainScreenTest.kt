package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithText("Şehir ara").assertIsDisplayed()
    }

    @Test
    fun mainScreenShowsEmptyState() {
        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Empty),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("Sonuç bulunamadı").assertIsDisplayed()
    }

    @Test
    fun mainScreenRestoresScrollPositionFromListState() {
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = { toggled = true },
                    onToggleThemeMode = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM
                )
            }
        }

        composeRule.onNodeWithText("°C").performClick()
        assert(toggled)
    }

    @Test
    fun mainScreenRefreshKeepsContentAndShowsIndicator() {
        var refreshed = false
        val mapper = WeatherUiMapper()
        val cityWeathers = syntheticCityWeathers().map {
            mapper.toCityWeatherUiModel(it, isFavorite = false)
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
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = { refreshed = true },
                    onToggleUnit = {},
                    onToggleThemeMode = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithTag("refresh_indicator").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Yenile").performClick()
        assert(refreshed)
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
                        onFavoriteClick = {},
                        onAddCity = {},
                        onOpenCity = {},
                        onDeleteCity = {},
                        onRetry = {},
                        onRefresh = {},
                        onToggleUnit = {},
                        onToggleThemeMode = {}
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
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = { themeMode.value = current.next() }
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }
        val istanbulId = syntheticCityWeathers().first().city.id

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = { deletedId = it },
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(content = UiState.Success(cityWeathers)),
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onAddCity = {},
                    onOpenCity = {},
                    onDeleteCity = { deleted = true },
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
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
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }
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
