package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
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
                    onToggleUnit = {}
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
                    onToggleUnit = {}
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
                    onToggleUnit = { toggled = true }
                )
            }
        }

        composeRule.onNodeWithText("°C").performClick()
        assert(toggled)
    }
}
