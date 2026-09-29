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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
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
 *
 * Dallanma **exhaustive** `when` ile yapılır: [CardWeather]'a yeni bir varyant
 * eklenirse bu kart derlenmez; `as?` kaçamağı yeni varyantı sessizce
 * "hata kartı" gibi çizdirirdi.
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
    when (val weather = cityWeather.weather) {
        is CardWeather.Loaded -> {
            val palette = ConditionPalette.of(weather.condition, isDark = isDarkTheme)
            CityWeatherCardContent(
                cityWeather = cityWeather,
                emoji = weather.conditionEmoji,
                detailText = weather.temperatureText,
                containerColor = palette.container,
                contentColor = palette.onContainer,
                onClick = onClick,
                onLongClick = onLongClick,
                trailing = null,
                modifier = modifier
            )
        }
        // Yüksüz kart bir hava koşulu temsil etmez; koşul paleti yanıltıcı
        // olurdu. Nötr yüzey "hata değil, veri yok" demek için yeterli.
        CardWeather.Unavailable -> CityWeatherCardContent(
            cityWeather = cityWeather,
            emoji = "🌐",
            detailText = stringResource(R.string.card_weather_unavailable),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
            onClick = onClick,
            onLongClick = onLongClick,
            trailing = {
                // Deneme sürerken düğme kilitlenir; çift dokunuş ikinci istek
                // atamaz. Kart yüklenince bu dal tamamen kaybolur.
                if (isRetrying) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    FilledTonalButton(
                        onClick = onRetryCity,
                        modifier = Modifier.testTag("city_retry_button")
                    ) {
                        Text(text = stringResource(R.string.action_retry))
                    }
                }
            },
            modifier = modifier
        )
    }
}

/**
 * İki dalın paylaştığı kart iskeleti. Dal farkı yalnız görsel değerlerde ve
 * sağdaki [trailing] yuvasındadır; iskelet (tıklama, uzun basma, ad/bölge
 * düzeni) ortaktır.
 */
@Composable
private fun CityWeatherCardContent(
    cityWeather: CityWeatherUiModel,
    emoji: String,
    detailText: String,
    containerColor: Color,
    contentColor: Color,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    trailing: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier
) {
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
                text = emoji,
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
                    text = detailText,
                    style = MaterialTheme.typography.titleLarge,
                    color = contentColor
                )
            }
            trailing?.invoke()
        }
    }
}
