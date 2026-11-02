package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.AdviceCategory
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.ConditionIcon
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.CompanionAudioPlayer
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.CompanionHero
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion.NoOpCompanionAudioPlayer
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CompanionUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherIcon

@Composable
fun ForecastContent(
    uiState: DetailUiState,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    isDark: Boolean,
    contentColor: Color,
    companionAudio: CompanionAudioPlayer = NoOpCompanionAudioPlayer,
    animationsEnabled: Boolean = false,
    modifier: Modifier = Modifier
) {
    PullToRefreshBox(
        isRefreshing = uiState.isRefreshing,
        onRefresh = onRefresh,
        modifier = Modifier.testTag("pull_refresh")
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            when (val content = uiState.content) {
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(HavaSpacing.lg),
                    verticalArrangement = Arrangement.spacedBy(HavaSpacing.lg)
                ) {
                    item(key = "companion_hero") {
                        CompanionHero(
                            advice = content.data.companion,
                            currentWeather = content.data.currentWeather,
                            colors = ConditionPalette.of(
                                condition = content.data.currentWeather.condition,
                                isDark = isDark,
                                isDay = content.data.currentWeather.isDay
                            ),
                            audio = companionAudio,
                            animationsEnabled = animationsEnabled
                        )
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_hourly), contentColor = contentColor)
                    }
                    if (content.data.hourlyForecast.isNotEmpty()) {
                        item(key = "hourly_chart") {
                            HourlyForecastChart(items = content.data.hourlyForecast)
                        }
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_daily), contentColor = contentColor)
                    }
                    // key Bundle'da saklanabilir olmalı; UiText saklanamaz,
                    // bu yüzden dayOffset (Int) kullanılır.
                    items(content.data.dailyForecast, key = { it.dayOffset }) { daily ->
                        DailyForecastItem(daily, isDark = isDark)
                    }
                }
                // Liste yokken (yükleme/boş/hata) kaydırılabilir bir yüzey gerekir:
                // aksi halde PullToRefreshBox aşma hareketini harcayamaz, çekme
                // çalışmaz.
                else -> Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(HavaSpacing.md)
                ) {
                    when (content) {
                        is UiState.Loading -> CircularProgressIndicator(
                            modifier = Modifier.padding(HavaSpacing.xl)
                        )
                        is UiState.Empty -> Text(
                            text = stringResource(R.string.state_empty_detail),
                            color = contentColor,
                            modifier = Modifier.padding(HavaSpacing.xl)
                        )
                        is UiState.Error -> {
                            Text(
                                text = content.message.asString(),
                                color = contentColor,
                                modifier = Modifier.padding(HavaSpacing.xl)
                            )
                            Button(onClick = onRetry) {
                                Text(text = stringResource(R.string.action_retry))
                            }
                        }
                        is UiState.Success -> Unit
                    }
                }
            }
        }
    }
}

/**
 * Arka plan üzerinde duran bölüm başlığı: tema rengi değil, aktif arka plan
 * paletinin AA-garantili contentColor'ı kullanılır (açık tema + koyu gece
 * arka planı kontrast kırılmasın — regresyon kilidi ForecastContentTest'te).
 */
@Composable
private fun SectionHeader(text: String, contentColor: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = contentColor
    )
}

@Composable
private fun DailyForecastItem(item: DailyForecastUiModel, isDark: Boolean) {
    val palette = ConditionPalette.of(item.condition, isDark = isDark, isDay = true)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("daily_card"),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = palette.container,
            contentColor = palette.onContainer
        )
    ) {
        Column(modifier = Modifier.padding(HavaSpacing.md)) {
            Text(text = item.dateText.asString())
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConditionIcon(icon = item.icon, size = 20.dp)
                Text(
                    text = "${item.minTemperatureText} / ${item.maxTemperatureText}",
                    modifier = Modifier.padding(start = HavaSpacing.sm)
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ForecastContentPreview() {
    HavaTheme {
        ForecastContent(
            uiState = DetailUiState(content = UiState.Success(previewDetail())),
            onRetry = {},
            onRefresh = {},
            isDark = false,
            contentColor = MaterialTheme.colorScheme.onBackground
        )
    }
}

private fun previewDetail() = ForecastDetailUiModel(
    city = CityUiModel(1, "İstanbul", "Marmara", "Türkiye", 41.01, 28.98),
    currentWeather = CurrentWeatherUiModel(
        temperatureText = "18°C",
        icon = WeatherIcon.Glyph("☀"),
        conditionLabel = UiText.Resource(R.string.condition_clear),
        windSpeedText = "8 km/s",
        humidityText = "%50",
        condition = WeatherCondition.CLEAR,
        isDay = true
    ),
    hourlyForecast = List(8) { hour ->
        HourlyForecastUiModel(
            timeLabel = if (hour == 0) {
                UiText.Resource(R.string.hourly_now)
            } else {
                UiText.Dynamic("%02d:00".format(hour))
            },
            temperatureText = "${18 + hour % 4}°C",
            temperatureValue = 18 + hour % 4,
            icon = WeatherIcon.Glyph("⛅"),
            precipitationProbabilityText = "%10"
        )
    },
    dailyForecast = List(7) { day ->
        DailyForecastUiModel(
            dayOffset = day,
            dateText = if (day == 0) {
                UiText.Resource(R.string.forecast_today)
            } else {
                UiText.Resource(R.string.day_friday)
            },
            minTemperatureText = "12°C",
            maxTemperatureText = "20°C",
            icon = WeatherIcon.Glyph("☁"),
            condition = WeatherCondition.CLOUDY
        )
    },
    companion = CompanionUiModel(
        pose = CompanionPose.CHEERFUL,
        category = AdviceCategory.CLEAR,
        variantSeed = 0
    )
)
