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
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel

@Composable
fun ForecastContent(uiState: DetailUiState, onRetry: () -> Unit, onRefresh: () -> Unit, modifier: Modifier = Modifier) {
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
                            conditionEmoji = content.data.currentWeather.conditionEmoji,
                            conditionLabel = content.data.currentWeather.conditionLabel.asString(),
                            windSpeedText = content.data.currentWeather.windSpeedText,
                            humidityText = content.data.currentWeather.humidityText
                        )
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_hourly))
                    }
                    items(content.data.hourlyForecast, key = { it.timeText }) { hourly ->
                        HourlyForecastItem(hourly)
                    }
                    item {
                        SectionHeader(text = stringResource(R.string.section_daily))
                    }
                    // key Bundle'da saklanabilir olmalı; UiText saklanamaz,
                    // bu yüzden dayOffset (Int) kullanılır.
                    items(content.data.dailyForecast, key = { it.dayOffset }) { daily ->
                        DailyForecastItem(daily)
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
    conditionEmoji: String,
    conditionLabel: String,
    windSpeedText: String,
    humidityText: String
) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(HavaSpacing.xl),
            verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
        ) {
            Text(
                text = stringResource(R.string.section_current),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = conditionEmoji, style = MaterialTheme.typography.displayLarge)
                Text(
                    text = temperatureText,
                    style = MaterialTheme.typography.displayLarge,
                    modifier = Modifier.padding(start = HavaSpacing.md)
                )
            }
            Text(
                text = conditionLabel,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(horizontalArrangement = Arrangement.spacedBy(HavaSpacing.lg)) {
                Text(
                    text = windSpeedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = humidityText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
private fun HourlyForecastItem(item: HourlyForecastUiModel) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(HavaSpacing.md)) {
            Text(text = item.timeText)
            Text(text = "${item.conditionEmoji} ${item.temperatureText}")
            Text(text = item.precipitationProbabilityText)
        }
    }
}

@Composable
private fun DailyForecastItem(item: DailyForecastUiModel) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Column(modifier = Modifier.padding(HavaSpacing.md)) {
            Text(text = item.dateText.asString())
            Text(
                text = "${item.conditionEmoji} " +
                    "${item.minTemperatureText} / ${item.maxTemperatureText}"
            )
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
            onRefresh = {}
        )
    }
}
