package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import org.junit.Rule
import org.junit.Test

class DetailScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun detailScreenShowsForecastSections() {
        val forecast = ForecastDetailUiModel(
            city = CityUiModel(
                id = 1L,
                name = "İstanbul",
                region = "Marmara",
                country = "Türkiye",
                latitude = 41.01,
                longitude = 28.98
            ),
            currentWeather = CurrentWeatherUiModel(
                temperatureText = "21°C",
                conditionEmoji = "☀️",
                conditionLabel = UiText.Dynamic("Açık"),
                windSpeedText = "10 km/s",
                humidityText = "%55"
            ),
            hourlyForecast = listOf(
                HourlyForecastUiModel(
                    timeText = "12:00",
                    temperatureText = "22°C",
                    conditionEmoji = "☀️",
                    precipitationProbabilityText = "%0"
                )
            ),
            dailyForecast = listOf(
                DailyForecastUiModel(
                    dateText = "Bugün",
                    minTemperatureText = "18°C",
                    maxTemperatureText = "24°C",
                    conditionEmoji = "☀️"
                )
            ),
            isFavorite = false
        )

        composeRule.setContent {
            HavaTheme {
                DetailScreen(
                    uiState = DetailUiState(content = UiState.Success(forecast)),
                    onBack = {},
                    onFavoriteClick = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithText("Saatlik Tahmin").assertIsDisplayed()
        composeRule.onNodeWithText("Günlük Tahmin").assertIsDisplayed()
    }

    @Test
    fun detailScreenErrorStateShowsMessageRetryAndBack() {
        composeRule.setContent {
            HavaTheme {
                DetailScreen(
                    uiState = DetailUiState(
                        content = UiState.Error(UiText.Dynamic("Şehir bilgisi bulunamadı"))
                    ),
                    onBack = {},
                    onFavoriteClick = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {}
                )
            }
        }

        composeRule.onNodeWithText("Şehir bilgisi bulunamadı").assertIsDisplayed()
        composeRule.onNodeWithText("Tekrar dene").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Geri").assertIsDisplayed()
    }

    @Test
    fun detailScreenShowsUnitToggleAndNotifiesClick() {
        var toggled = false

        composeRule.setContent {
            HavaTheme {
                DetailScreen(
                    uiState = DetailUiState(content = UiState.Loading),
                    onBack = {},
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
