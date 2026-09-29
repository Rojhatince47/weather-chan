package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

/**
 * Şehir kartı. Yüzey rengi koşula göre değişir; bu, listedeki kartları
 * birbirinden ayırır ve koşulu isim okumadan da anlatır.
 *
 * Hava durumu alınamayan şehir için bkz. [CardWeather.Unavailable]: kart
 * nedeni söyler ve yalnız o şehri yeniden denemeyi önerir. Kart tıklanabilir
 * kalır — detay ekranı kendi hata durumunu zaten gösteriyor.
 */
@Composable
fun CityWeatherCard(
    cityWeather: CityWeatherUiModel,
    isDarkTheme: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onRetryCity: () -> Unit = {},
    isRetrying: Boolean = false,
    modifier: Modifier = Modifier
) {
    val loaded = cityWeather.weather as? CardWeather.Loaded
    // Yüksüz kart bir hava koşulu temsil etmez; koşul paleti yanıltıcı olurdu.
    val palette = loaded?.let { ConditionPalette.of(it.condition, isDark = isDarkTheme) }
    val containerColor = palette?.container ?: MaterialTheme.colorScheme.surfaceVariant
    val contentColor = palette?.onContainer ?: MaterialTheme.colorScheme.onSurfaceVariant

    Card(
        // Tıklama kartın kendisinde: ripple yuvarlak yüzeye kırpılır. İç Row'da
        // olsaydı ripple dikdörtgen kalır ve yuvarlatılmış köşeleri kapatırdı.
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Row(
            modifier = Modifier.padding(HavaSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = loaded?.conditionEmoji ?: "🌐",
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
                    color = contentColor
                )
                Text(
                    text = listOfNotNull(cityWeather.region, cityWeather.country).joinToString(", "),
                    style = MaterialTheme.typography.bodySmall,
                    color = contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = loaded?.temperatureText
                        ?: stringResource(R.string.card_weather_unavailable),
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }
            if (loaded == null) {
                // Deneme sürerken düğme kilitlenir; çift dokunuş ikinci istek
                // atamaz. Kart yüklenince bu dal tamamen kaybolur.
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    FilledTonalButton(onClick = onRetryCity) {
                        Text(text = stringResource(R.string.action_retry))
                    }
                }
            }
        }
    }
}
