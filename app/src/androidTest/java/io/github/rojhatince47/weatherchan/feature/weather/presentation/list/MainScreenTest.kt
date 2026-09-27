package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import org.junit.Rule
import org.junit.Test

class MainScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mainScreenShowsCities() {
        val mapper = WeatherUiMapper()
        val cityWeathers = SampleWeatherDataSource().cityWeathers.map {
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
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = {},
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
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = {},
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
        val cityWeathers = SampleWeatherDataSource().cityWeathers.map {
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
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = {},
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    listState = LazyListState(firstVisibleItemIndex = 3)
                )
            }
        }

        composeRule.onNodeWithText("Antalya").assertIsDisplayed()
    }

    @Test
    fun mainScreenShowsFavoritesFilterAndNotifiesChange() {
        var filterChangedTo: Boolean? = null
        val mapper = WeatherUiMapper()
        val cityWeathers = SampleWeatherDataSource().cityWeathers.map {
            mapper.toCityWeatherUiModel(it, isFavorite = false)
        }

        composeRule.setContent {
            HavaTheme {
                MainScreen(
                    uiState = MainUiState(
                        content = UiState.Success(cityWeathers)
                    ),
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    themeMode = ThemeMode.SYSTEM,
                    onQueryChange = {},
                    onCityClick = {},
                    onFavoriteClick = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = { showFavoritesOnly ->
                        filterChangedTo = showFavoritesOnly
                    }
                )
            }
        }

        composeRule.onNodeWithText("Favoriler").performClick()
        assert(filterChangedTo == true)
    }

    @Test
    fun mainScreenShowsUnitToggleAndNotifiesClick() {
        var toggled = false
        val mapper = WeatherUiMapper()
        val cityWeathers = SampleWeatherDataSource().cityWeathers.map {
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
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = { toggled = true },
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = {},
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
        val cityWeathers = SampleWeatherDataSource().cityWeathers.map {
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
                    onRetry = {},
                    onRefresh = { refreshed = true },
                    onToggleUnit = {},
                    onToggleThemeMode = {},
                    onFavoritesOnlyChange = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithTag("refresh_indicator").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Yenile").performClick()
        assert(refreshed)
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
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {},
                    onToggleThemeMode = { themeMode.value = current.next() },
                    onFavoritesOnlyChange = {}
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
}
