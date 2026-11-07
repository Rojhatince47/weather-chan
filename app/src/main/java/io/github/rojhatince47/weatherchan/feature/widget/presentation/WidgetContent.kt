package io.github.rojhatince47.weatherchan.feature.widget.presentation

import android.content.Intent
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.clickable
import androidx.glance.appwidget.action.actionStartActivity
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import io.github.rojhatince47.weatherchan.MainActivity
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.ConditionPalette
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.advice.CompanionPose
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.widget.domain.WidgetSnapshot

private data class WidgetMetrics(
    val character: androidx.compose.ui.unit.Dp,
    val temperature: androidx.compose.ui.unit.TextUnit,
    val label: androidx.compose.ui.unit.TextUnit,
    val city: androidx.compose.ui.unit.TextUnit,
    val hour: androidx.compose.ui.unit.TextUnit,
    val glyph: androidx.compose.ui.unit.TextUnit,
    val bigGlyph: androidx.compose.ui.unit.TextUnit
)

/**
 * İçerik boyutu widget'ın gerçek hücresine göre ölçeklenir. 2x2 hücre
 * cihazdan cihaza 150–250 dp arasında değişir; sabit dp kullanmak küçük
 * telefonlarda taşırır, büyüklerde boşluk bırakır.
 */
@Composable
private fun rememberWidgetMetrics(): WidgetMetrics {
    val size = LocalSize.current
    val minDim = if (size.width < size.height) size.width else size.height
    val wide = size.width >= 250.dp
    val stacked = !wide && size.height >= 200.dp

    val character = if (wide) {
        (size.height * 0.68f).coerceIn(80.dp, 240.dp)
    } else {
        (minDim * 0.68f).coerceIn(72.dp, 190.dp)
    }
    val temperature = (minDim.value * if (wide) 0.12f else 0.13f).coerceIn(22f, 40f).sp
    val label = (minDim.value * 0.07f).coerceIn(11f, 18f).sp
    val city = (minDim.value * 0.055f).coerceIn(10f, 15f).sp
    val hour = (minDim.value * 0.052f).coerceIn(9f, 14f).sp
    val glyph = (minDim.value * 0.072f).coerceIn(12f, 20f).sp
    val bigGlyph = (minDim.value * 0.2f).coerceIn(24f, 56f).sp
    return WidgetMetrics(character, temperature, label, city, hour, glyph, bigGlyph)
}

@Composable
internal fun ChatWidgetContent(snapshot: WidgetSnapshot?) {
    val condition = snapshot.conditionOrCloudy()
    val size = LocalSize.current
    val wide = size.width >= 250.dp
    val metrics = rememberWidgetMetrics()
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            // Bilinçli olarak arka plan yok: karakter duvar kağıdının üzerinde
            // yüzsün. Yazı okunurluğu küçük yarı saydam zeminlerle sağlanır.
            .padding(8.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (snapshot == null) {
            Text(
                text = context.getString(R.string.widget_no_data),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 13.sp
                ),
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_scrim))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
            return@Column
        }

        if (wide) {
            Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
                Image(
                    provider = ImageProvider(poseDrawable(snapshot.pose)),
                    contentDescription = context.getString(
                        R.string.widget_character_description
                    ),
                    modifier = GlanceModifier.size(metrics.character)
                )
                TextPill(
                    snapshot = snapshot,
                    condition = condition,
                    metrics = metrics,
                    showCity = true,
                    modifier = GlanceModifier.padding(start = 10.dp)
                )
            }
            if (snapshot.hourly.isNotEmpty()) {
                ChatHourlyStrip(snapshot, metrics)
            }
        } else {
            // Kare widget'ta karakter ana öğe; yazı altta ince şeritte.
            Image(
                provider = ImageProvider(poseDrawable(snapshot.pose)),
                contentDescription = context.getString(
                    R.string.widget_character_description
                ),
                modifier = GlanceModifier.size(metrics.character)
            )
            TextPill(
                snapshot = snapshot,
                condition = condition,
                metrics = metrics,
                showCity = false
            )
        }
    }
}

/** Şeffaf zeminde okunurluk için yalnız yazının arkasında yarı saydam kutu. */
@Composable
private fun TextPill(
    snapshot: WidgetSnapshot,
    condition: WeatherCondition,
    metrics: WidgetMetrics,
    showCity: Boolean,
    modifier: GlanceModifier = GlanceModifier
) {
    Column(
        modifier = modifier
            .background(ImageProvider(R.drawable.widget_scrim))
            .padding(horizontal = 12.dp, vertical = 5.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally
    ) {
        Text(
            text = temperatureText(snapshot),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = metrics.temperature,
                fontWeight = FontWeight.Bold
            ),
            maxLines = 1
        )
        Text(
            text = LocalContext.current.getString(conditionLabelRes(condition)),
            style = TextStyle(
                color = ColorProvider(Color.White),
                fontSize = metrics.label
            )
        )
        if (showCity) {
            Text(
                text = snapshot.cityName,
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = metrics.city
                )
            )
        }
    }
}

@Composable
private fun ChatHourlyStrip(snapshot: WidgetSnapshot, metrics: WidgetMetrics) {
    Row(
        modifier = GlanceModifier
            .padding(top = 8.dp)
            .background(ImageProvider(R.drawable.widget_scrim))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        snapshot.hourly.forEach { hour ->
            val condition = runCatching {
                WeatherCondition.valueOf(hour.condition)
            }.getOrDefault(WeatherCondition.CLOUDY)
            Column(
                modifier = GlanceModifier.padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "%02d".format(hour.hourOfDay),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = metrics.hour)
                )
                Text(
                    text = conditionGlyph(condition, snapshot.isDay),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = metrics.glyph)
                )
                Text(
                    text = temperatureText(hour.temperatureCelsius, snapshot.unit),
                    style = TextStyle(color = ColorProvider(Color.White), fontSize = metrics.label)
                )
            }
        }
    }
}

@Composable
internal fun CharacterOnlyContent(snapshot: WidgetSnapshot?) {
    val context = LocalContext.current
    androidx.glance.layout.Box(
        modifier = GlanceModifier
            .fillMaxSize()
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        contentAlignment = Alignment.Center
    ) {
        if (snapshot == null) {
            Text(
                text = context.getString(R.string.widget_no_data),
                style = TextStyle(
                    color = ColorProvider(Color.White),
                    fontSize = 12.sp
                ),
                modifier = GlanceModifier
                    .background(ImageProvider(R.drawable.widget_scrim))
                    .padding(horizontal = 10.dp, vertical = 6.dp)
            )
        } else {
            Image(
                provider = ImageProvider(poseDrawable(snapshot.pose)),
                contentDescription = context.getString(R.string.widget_character_description),
                modifier = GlanceModifier
                    .fillMaxSize()
                    .padding(8.dp),
                contentScale = androidx.glance.layout.ContentScale.Fit
            )
        }
    }
}

@Composable
internal fun InfoWidgetContent(snapshot: WidgetSnapshot?) {
    val condition = snapshot.conditionOrCloudy()
    val colors = ConditionPalette.of(
        condition = condition,
        isDark = snapshot?.isDay == false,
        isDay = snapshot?.isDay ?: true
    )
    val size = LocalSize.current
    val wide = size.width >= 250.dp
    val metrics = rememberWidgetMetrics()
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.container)
            .padding(10.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        if (snapshot == null) {
            Text(
                text = context.getString(R.string.widget_no_data),
                style = TextStyle(
                    color = ColorProvider(colors.onContainer),
                    fontSize = 13.sp
                )
            )
            return@Column
        }

        if (!wide) {
            Text(
                text = conditionGlyph(condition, snapshot.isDay),
                style = TextStyle(
                    color = ColorProvider(colors.onContainer),
                    fontSize = metrics.bigGlyph
                )
            )
        }
        Text(
            text = temperatureText(snapshot),
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = metrics.temperature,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = context.getString(conditionLabelRes(condition)),
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = metrics.label
            )
        )
        Text(
            text = snapshot.cityName,
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = metrics.city
            )
        )
        if (wide && snapshot.hourly.isNotEmpty()) {
            HourlyStrip(snapshot, colors.onContainer, metrics)
        }
    }
}

@Composable
private fun HourlyStrip(snapshot: WidgetSnapshot, contentColor: Color, metrics: WidgetMetrics) {
    Row(
        modifier = GlanceModifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalAlignment = Alignment.Horizontal.CenterHorizontally,
        verticalAlignment = Alignment.Vertical.CenterVertically
    ) {
        snapshot.hourly.forEach { hour ->
            val condition = runCatching {
                WeatherCondition.valueOf(hour.condition)
            }.getOrDefault(WeatherCondition.CLOUDY)
            Column(
                modifier = GlanceModifier.padding(horizontal = 8.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "%02d".format(hour.hourOfDay),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = metrics.hour)
                )
                Text(
                    text = conditionGlyph(condition, snapshot.isDay),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = metrics.glyph)
                )
                Text(
                    text = temperatureText(hour.temperatureCelsius, snapshot.unit),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = metrics.label)
                )
            }
        }
    }
}

private fun WidgetSnapshot?.conditionOrCloudy(): WeatherCondition =
    this?.condition?.let { runCatching { WeatherCondition.valueOf(it) }.getOrNull() }
        ?: WeatherCondition.CLOUDY

internal fun temperatureText(snapshot: WidgetSnapshot): String = temperatureText(snapshot.temperatureCelsius, snapshot.unit)

internal fun temperatureText(celsius: Int, unitName: String): String {
    val unit = runCatching { TemperatureUnit.valueOf(unitName) }.getOrDefault(TemperatureUnit.CELSIUS)
    return when (unit) {
        TemperatureUnit.CELSIUS -> "$celsius°C"
        TemperatureUnit.FAHRENHEIT -> "${celsius * 9 / 5 + 32}°F"
    }
}

internal fun poseDrawable(poseName: String): Int {
    val pose = runCatching { CompanionPose.valueOf(poseName) }.getOrDefault(CompanionPose.CHEERFUL)
    return when (pose) {
        CompanionPose.CHEERFUL -> R.drawable.companion_cheerful
        CompanionPose.RAINY -> R.drawable.companion_rainy
        CompanionPose.COLD -> R.drawable.companion_cold
        CompanionPose.WINDY -> R.drawable.companion_windy
        CompanionPose.CALM -> R.drawable.companion_calm
        CompanionPose.SLEEPY -> R.drawable.companion_sleepy
        CompanionPose.SURPRISED -> R.drawable.companion_surprised
        CompanionPose.TAP_GIGGLE,
        CompanionPose.TAP_LAUGH,
        CompanionPose.TAP_HUM,
        CompanionPose.TAP_SIGH -> R.drawable.companion_cheerful
    }
}

internal fun conditionLabelRes(condition: WeatherCondition): Int = when (condition) {
    WeatherCondition.CLEAR -> R.string.condition_clear
    WeatherCondition.PARTLY_CLOUDY -> R.string.condition_partly_cloudy
    WeatherCondition.CLOUDY -> R.string.condition_cloudy
    WeatherCondition.LIGHT_SHOWERS -> R.string.condition_light_showers
    WeatherCondition.RAIN -> R.string.condition_rain
    WeatherCondition.SNOW -> R.string.condition_snow
    WeatherCondition.THUNDERSTORM -> R.string.condition_thunderstorm
}

internal fun conditionGlyph(condition: WeatherCondition, isDay: Boolean): String = when (condition) {
    WeatherCondition.CLEAR -> if (isDay) "☀" else "🌙"
    WeatherCondition.PARTLY_CLOUDY -> if (isDay) "⛅" else "☁"
    WeatherCondition.CLOUDY -> "☁"
    WeatherCondition.LIGHT_SHOWERS -> if (isDay) "🌦" else "☔"
    WeatherCondition.RAIN -> "🌧"
    WeatherCondition.SNOW -> "❄"
    WeatherCondition.THUNDERSTORM -> "⛈"
}
