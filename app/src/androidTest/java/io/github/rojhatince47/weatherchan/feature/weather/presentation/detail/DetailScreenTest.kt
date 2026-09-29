package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import org.junit.Assert.assertTrue
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
                    dayOffset = 0,
                    dateText = UiText.Dynamic("Bugün"),
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
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
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

    @Test
    fun detailScreenRefreshesOnPullDown() {
        var refreshed = false

        composeRule.setContent {
            HavaTheme {
                DetailScreen(
                    uiState = DetailUiState(content = UiState.Success(detailUiModel())),
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    onBack = {},
                    onFavoriteClick = {},
                    onRetry = {},
                    onRefresh = { refreshed = true },
                    onToggleUnit = {}
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

        assertTrue(refreshed)
    }

    @Test
    fun detailScreenKeepsContentWhileRefreshing() {
        composeRule.setContent {
            HavaTheme {
                DetailScreen(
                    uiState = DetailUiState(
                        content = UiState.Success(detailUiModel()),
                        isRefreshing = true
                    ),
                    unit = TemperatureUnit.CELSIUS.toUiModel(),
                    onBack = {},
                    onFavoriteClick = {},
                    onRetry = {},
                    onRefresh = {},
                    onToggleUnit = {}
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").assertIsDisplayed()
        composeRule.onNodeWithTag("pull_refresh").assertIsDisplayed()
    }

    private fun detailUiModel(): ForecastDetailUiModel = WeatherUiMapper()
        .toForecastDetailUiModel(syntheticForecast(), isFavorite = false)

    private fun syntheticForecast() = Forecast(
        city = City(
            name = "İstanbul",
            region = "Marmara",
            country = "Türkiye",
            coordinates = Coordinates(latitude = 41.01, longitude = 28.98)
        ),
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        ),
        hourlyForecast = List(24) { hour ->
            HourlyForecast(
                hourOfDay = hour,
                temperatureCelsius = 15 + hour % 10,
                condition = WeatherCondition.PARTLY_CLOUDY,
                precipitationProbabilityPercent = hour * 3 % 100
            )
        },
        dailyForecast = List(7) { day ->
            DailyForecast(
                dayOffset = day,
                minTemperatureCelsius = 10 + day % 5,
                maxTemperatureCelsius = 20 + day % 8,
                condition = WeatherCondition.CLOUDY
            )
        }
    )
}
