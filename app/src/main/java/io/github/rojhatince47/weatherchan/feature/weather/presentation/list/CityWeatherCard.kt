package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

@Composable
fun CityWeatherCard(cityWeather: CityWeatherUiModel, onClick: () -> Unit, onFavoriteClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(modifier = Modifier.padding(16.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = cityWeather.name,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = listOfNotNull(cityWeather.region, cityWeather.country)
                        .joinToString(", "),
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "${cityWeather.conditionEmoji} ${cityWeather.temperatureText}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            IconButton(onClick = onFavoriteClick) {
                Icon(
                    imageVector = if (cityWeather.isFavorite) {
                        Icons.Filled.Favorite
                    } else {
                        Icons.Filled.FavoriteBorder
                    },
                    contentDescription = if (cityWeather.isFavorite) {
                        "Favoriden çıkar"
                    } else {
                        "Favorilere ekle"
                    }
                )
            }
        }
    }
}
