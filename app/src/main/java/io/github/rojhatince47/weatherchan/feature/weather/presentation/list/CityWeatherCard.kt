package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
    isRetrying: Boolean = false,
    onRetryCity: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    CityWeatherCardContent(
        name = cityWeather.name,
        region = cityWeather.region,
        country = cityWeather.country,
        isLocation = cityWeather.isLocation,
        cardWeather = cityWeather.weather,
        isDarkTheme = isDarkTheme,
        isLoading = false,
        isRetrying = isRetrying,
        onRetryCity = onRetryCity,
        onClick = onClick,
        onLongClick = onLongClick,
        trailing = trailing,
        modifier = modifier
    )
}

/**
 * Ortak kart iskeleti — hem [CityWeatherCard] (MainScreen) hem
 * AddCityScreen arama/öneri satırları tarafından kullanılır.
 *
 * Görsel-parametreler (emoji, detay, palet) content içi [cardVisuals]
 * ile türetilir; çağıran yalnız alan verisi ve durum taşır.
 */
@Composable
fun CityWeatherCardContent(
    name: String,
    region: String?,
    country: String?,
    isLocation: Boolean,
    cardWeather: CardWeather?,
    isDarkTheme: Boolean,
    isLoading: Boolean,
    isRetrying: Boolean = false,
    onRetryCity: (() -> Unit)? = null,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    distanceKm: Int? = null,
    onAddClick: (() -> Unit)? = null,
    isInList: Boolean = false,
    enabled: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val effectiveEnabled = enabled && !isLoading
    val visuals = cardVisuals(cardWeather, isDarkTheme, isLoading, isRetrying)
    Card(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onLongClick != null) {
                    Modifier.combinedClickable(enabled = effectiveEnabled, onClick = onClick, onLongClick = onLongClick)
                } else {
                    Modifier.clickable(enabled = effectiveEnabled, onClick = onClick)
                }
            ),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = visuals.containerColor)
    ) {
        Row(
            modifier = Modifier.padding(HavaSpacing.lg),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = visuals.emoji,
                style = MaterialTheme.typography.headlineMedium
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = HavaSpacing.md),
                verticalArrangement = Arrangement.spacedBy(HavaSpacing.xs)
            ) {
                if (isLocation) {
                    Text(
                        text = "📍 ${stringResource(R.string.location_card_label)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = visuals.contentColor.copy(alpha = 0.8f)
                    )
                }
                Text(
                    text = name.ifBlank { stringResource(R.string.location_default_name) },
                    style = MaterialTheme.typography.titleMedium,
                    color = visuals.contentColor
                )
                val subtitle = if (distanceKm != null) {
                    listOfNotNull(stringResource(R.string.suggestion_distance_km, distanceKm))
                        .plus(listOfNotNull(region, country))
                        .joinToString(" · ")
                } else {
                    listOfNotNull(
                        if (isLocation) null else region,
                        country
                    ).joinToString(", ")
                }
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = visuals.contentColor.copy(alpha = 0.7f)
                )
                Text(
                    text = visuals.detailText,
                    style = MaterialTheme.typography.titleLarge,
                    color = visuals.contentColor
                )
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .padding(top = HavaSpacing.sm),
                        strokeWidth = 2.dp
                    )
                }
            }
            when {
                isLoading -> Unit
                trailing != null -> trailing()
                onAddClick != null -> AddOrCheckSlot(onAddClick, isInList, effectiveEnabled)
                onRetryCity != null && cardWeather is CardWeather.Unavailable ->
                    RetrySlot(onRetryCity, isRetrying)
                else -> Unit
            }
        }
    }
}

// --- Private helpers ---

private data class CardVisuals(
    val emoji: String,
    val detailText: String,
    val containerColor: Color,
    val contentColor: Color
)

/**
 * Görsel-parametre türetmesi — content içi. Exhaustive `when (weather)`:
 * yeni [CardWeather] varyantı eklenirse derleme hatası.
 */
@Composable
private fun cardVisuals(weather: CardWeather?, isDark: Boolean, isLoading: Boolean, isRetrying: Boolean): CardVisuals {
    if (isLoading || weather == null) {
        return CardVisuals(
            emoji = "⏳",
            detailText = stringResource(R.string.card_weather_loading),
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
    return when (weather) {
        is CardWeather.Loaded -> {
            val p = ConditionPalette.of(weather.condition, isDark = isDark, isDay = weather.isDay)
            CardVisuals(weather.conditionEmoji, weather.temperatureText, p.container, p.onContainer)
        }
        is CardWeather.Unavailable -> {
            if (isRetrying) {
                CardVisuals(
                    emoji = "🌐",
                    detailText = stringResource(R.string.card_weather_loading),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                CardVisuals(
                    emoji = "🌐",
                    detailText = stringResource(R.string.card_weather_unavailable),
                    containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun AddOrCheckSlot(onAddClick: () -> Unit, isInList: Boolean, enabled: Boolean) {
    if (isInList) {
        // Hafif "pop": tik belirirken scale (spring). `Animatable` +
        // `LaunchedEffect` sayesinde satır ekran açılıştan beri
        // tikliyken de aynı geçişi oynar.
        val scale = remember { Animatable(0.75f) }
        LaunchedEffect(Unit) {
            scale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMediumLow
                )
            )
        }
        Icon(
            imageVector = Icons.Filled.Check,
            contentDescription = stringResource(R.string.search_result_in_list),
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
        )
    } else {
        IconButton(onClick = onAddClick, enabled = enabled) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.action_add_city)
            )
        }
    }
}

@Composable
private fun RetrySlot(onRetryCity: () -> Unit, isRetrying: Boolean) {
    if (isRetrying) {
        // Gösterge anlamsız bir Canvas'tır: düğme anlamlar ağacından
        // kalkarken TalkBack odakını ve "yeniden deneniyor" bilgisini
        // bu etiket devralır.
        val retryingLabel = stringResource(R.string.city_retrying)
        CircularProgressIndicator(
            modifier = Modifier
                .size(24.dp)
                .testTag("city_retry_spinner")
                .semantics { contentDescription = retryingLabel },
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
}
