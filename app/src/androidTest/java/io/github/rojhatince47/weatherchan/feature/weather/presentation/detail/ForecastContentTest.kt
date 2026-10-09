package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class ForecastContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun detailFixture(hourlyCount: Int = 24) = ForecastDetailUiModel(
        city = CityUiModel(1, "İstanbul", "Marmara", "Türkiye", 41.01, 28.98),
        currentWeather = CurrentWeatherUiModel(
            temperatureText = "18°C",
            conditionEmoji = "☀️",
            conditionLabel = UiText.Resource(R.string.condition_clear),
            windSpeedText = "8 km/s",
            humidityText = "%50"
        ),
        hourlyForecast = List(hourlyCount) { hour ->
            HourlyForecastUiModel(
                timeText = "%02d:00".format(hour),
                temperatureText = "2${hour % 10}°C",
                conditionEmoji = "🌤️",
                precipitationProbabilityText = "%10"
            )
        },
        dailyForecast = List(7) { day ->
            DailyForecastUiModel(
                dayOffset = day,
                dateText = UiText.Resource(R.string.forecast_today),
                minTemperatureText = "10°C",
                maxTemperatureText = "20°C",
                conditionEmoji = "☁️"
            )
        }
    )

    private fun setForecastContent(hourlyCount: Int = 24) {
        composeRule.setContent {
            HavaTheme {
                // LazyColumn sanallaştırması görünmeyen öğeleri compose etmez;
                // "her düğüm doğrulanır" şartı için yükseklik serbest bırakılır
                // (genişlik kısıtı ekranla aynı kalır). İçerik ~2600dp; 4000dp yeter.
                Box(Modifier.requiredHeight(4000.dp)) {
                    ForecastContent(
                        uiState = DetailUiState(content = UiState.Success(detailFixture(hourlyCount))),
                        onRetry = {},
                        onRefresh = {}
                    )
                }
            }
        }
    }

    private fun widthDp(tag: String): Float {
        val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    private fun widthDp(index: Int, tag: String): Float {
        val bounds = composeRule.onAllNodesWithTag(tag)[index].getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    @Test
    fun evKartlariTamGenislikCizilir() {
        setForecastContent()
        val expectedWidth = widthDp("pull_refresh") - 32f

        assertEquals(expectedWidth, widthDp("current_weather_card"), 1f)
        repeat(24) { index ->
            assertEquals(expectedWidth, widthDp(index, "hourly_forecast_item"), 1f)
        }
        repeat(7) { index ->
            assertEquals(expectedWidth, widthDp(index, "daily_card"), 1f)
        }
    }
}
