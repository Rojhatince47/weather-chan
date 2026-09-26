package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailScreen(
    uiState: DetailUiState,
    unit: TemperatureUnitUiModel,
    onBack: () -> Unit,
    onFavoriteClick: () -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onToggleUnit: () -> Unit,
    modifier: Modifier = Modifier,
    snackbarHostState: SnackbarHostState = remember { SnackbarHostState() }
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    val content = uiState.content
                    if (content is UiState.Success) {
                        Text(content.data.city.name)
                    } else {
                        Text("Hava Durumu")
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Geri"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRefresh) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = "Yenile"
                        )
                    }
                    TextButton(onClick = onToggleUnit) {
                        Text(
                            text = if (unit.unit == TemperatureUnit.CELSIUS) {
                                "°C"
                            } else {
                                "°F"
                            }
                        )
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            // Yenileme sırasında mevcut içerik korunur; yalnızca ince
            // ilerleme çubuğu gösterilir.
            if (uiState.isRefreshing) {
                LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag("refresh_indicator")
                )
            }
            when (val content = uiState.content) {
                is UiState.Loading -> CircularProgressIndicator(
                    modifier = Modifier.padding(24.dp)
                )
                is UiState.Empty -> Text(
                    text = "Veri bulunamadı",
                    modifier = Modifier.padding(24.dp)
                )
                is UiState.Error -> Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(text = content.message.asString())
                    Button(onClick = onRetry) {
                        Text(text = "Tekrar dene")
                    }
                }
                is UiState.Success -> LazyColumn(
                    modifier = Modifier.weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
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
                        Text(
                            text = "Saatlik Tahmin",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(content.data.hourlyForecast, key = { it.timeText }) { hourly ->
                        HourlyForecastItem(hourly)
                    }
                    item {
                        Text(
                            text = "Günlük Tahmin",
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                    items(content.data.dailyForecast, key = { it.dateText }) { daily ->
                        DailyForecastItem(daily)
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
    Card {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "$conditionEmoji $temperatureText",
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = conditionLabel,
                style = MaterialTheme.typography.bodyLarge
            )
            Text(text = windSpeedText)
            Text(text = humidityText)
        }
    }
}

@Composable
private fun HourlyForecastItem(item: HourlyForecastUiModel) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.timeText)
            Text(text = "${item.conditionEmoji} ${item.temperatureText}")
            Text(text = item.precipitationProbabilityText)
        }
    }
}

@Composable
private fun DailyForecastItem(item: DailyForecastUiModel) {
    Card {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(text = item.dateText)
            Text(
                text = "${item.conditionEmoji} " +
                    "${item.minTemperatureText} / ${item.maxTemperatureText}"
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DetailScreenPreview() {
    HavaTheme {
        DetailScreen(
            uiState = DetailUiState(content = UiState.Loading),
            unit = TemperatureUnit.CELSIUS.toUiModel(),
            onBack = {},
            onFavoriteClick = {},
            onRetry = {},
            onRefresh = {},
            onToggleUnit = {}
        )
    }
}
