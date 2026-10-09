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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.ConditionIcon
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherIcon

@Composable
fun ForecastContent(uiState: DetailUiState, onRetry: () -> Unit, onRefresh: () -> Unit, isDark: Boolean, modifier: Modifier = Modifier) {
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
                    item {
                        CurrentWeatherCard(
                            temperatureText = content.data.currentWeather.temperatureText,
                            icon = content.data.currentWeather.icon,
                            conditionLabel = content.data.currentWeather.conditionLabel.asString(),
                            windSpeedText = content.data.currentWeather.windSpeedText,
                            humidityText = content.data.currentWeather.humidityText,
                            condition = content.data.currentWeather.condition,
                            isDay = content.data.currentWeather.isDay,
                            isDark = isDark
                        )
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_hourly))
                    }
                    if (content.data.hourlyForecast.isNotEmpty()) {
                        item(key = "hourly_chart") {
                            HourlyForecastChart(items = content.data.hourlyForecast)
                        }
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_daily))
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
                            modifier = Modifier.padding(HavaSpacing.xl)
                        )
                        is UiState.Error -> {
                            Text(
                                text = content.message.asString(),
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

@Composable
private fun CurrentWeatherCard(
    temperatureText: String,
    icon: WeatherIcon,
    conditionLabel: String,
    windSpeedText: String,
    humidityText: String,
    condition: WeatherCondition,
    isDay: Boolean,
    isDark: Boolean
) {
    val palette = ConditionPalette.of(condition, isDark = isDark, isDay = isDay)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("current_weather_card"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = palette.container,
            contentColor = palette.onContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(HavaSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.section_current),
                style = MaterialTheme.typography.labelSmall
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                ConditionIcon(icon = icon, size = 40.dp)
                Text(
                    text = temperatureText,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(start = HavaSpacing.md)
                )
            }
            Text(
                text = conditionLabel,
                style = MaterialTheme.typography.titleMedium
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HavaSpacing.lg)) {
                Text(
                    text = windSpeedText,
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = humidityText,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
            uiState = DetailUiState(content = UiState.Loading),
            onRetry = {},
            onRefresh = {},
            isDark = false
        )
    }
}
