package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

/**
 * Şehir kartı. Yüzey rengi koşula göre değişir; bu, listedeki kartları
 * birbirinden ayırır ve koşulu isim okmadan da anlatır.
 */
@Composable
fun CityWeatherCard(
    cityWeather: CityWeatherUiModel,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onFavoriteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val conditionColors = ConditionPalette.of(cityWeather.condition, isDark = isDarkTheme)

    Card(
        // Tıklama kartın kendisinde: ripple yuvarlak yüzeye kırpılır. İç Row'da
        // olsaydı ripple dikdörtgen kalır ve yuvarlatılmış köşeleri kapatırdı.
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = conditionColors.container)
    ) {
        Row(
            modifier = Modifier.padding(HavaSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = cityWeather.conditionEmoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = HavaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(HavaSpacing.xs)
            ) {
                Text(
                    text = cityWeather.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = conditionColors.onContainer
                )
                Text(
                    text = listOfNotNull(cityWeather.region, cityWeather.country).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = conditionColors.onContainer.copy(alpha = 0.7f)
                )
                Text(
                    text = cityWeather.temperatureText,
                    style = MaterialTheme.typography.titleLarge,
                    color = conditionColors.onContainer
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
                        stringResource(R.string.favorite_remove)
                    } else {
                        stringResource(R.string.favorite_add)
                    },
                    tint = conditionColors.onContainer
                )
            }
        }
    }
}
