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

private const val WIDE_WIDTH_DP = 200

@Composable
internal fun ChatWidgetContent(snapshot: WidgetSnapshot?) {
    val condition = snapshot.conditionOrCloudy()
    val colors = ConditionPalette.of(
        condition = condition,
        isDark = snapshot?.isDay == false,
        isDay = snapshot?.isDay ?: true
    )
    val wide = LocalSize.current.width >= WIDE_WIDTH_DP.dp
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.container)
            .padding(12.dp)
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

        Row(verticalAlignment = Alignment.Vertical.CenterVertically) {
            Image(
                provider = ImageProvider(poseDrawable(snapshot.pose)),
                contentDescription = context.getString(R.string.widget_character_description),
                modifier = GlanceModifier.size(if (wide) 56.dp else 72.dp)
            )
            Column(
                modifier = GlanceModifier.padding(start = 10.dp),
                verticalAlignment = Alignment.Vertical.CenterVertically
            ) {
                Text(
                    text = temperatureText(snapshot),
                    style = TextStyle(
                        color = ColorProvider(colors.onContainer),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                )
                Text(
                    text = context.getString(conditionLabelRes(condition)),
                    style = TextStyle(
                        color = ColorProvider(colors.onContainer),
                        fontSize = 12.sp
                    )
                )
                Text(
                    text = snapshot.cityName,
                    style = TextStyle(
                        color = ColorProvider(colors.onContainer),
                        fontSize = 11.sp
                    )
                )
            }
        }
        if (wide && snapshot.hourly.isNotEmpty()) {
            HourlyStrip(snapshot, colors.onContainer)
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
    val wide = LocalSize.current.width >= WIDE_WIDTH_DP.dp
    val context = LocalContext.current

    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(colors.container)
            .padding(12.dp)
            .clickable(actionStartActivity(Intent(context, MainActivity::class.java))),
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

        Text(
            text = temperatureText(snapshot),
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold
            )
        )
        Text(
            text = context.getString(conditionLabelRes(condition)),
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = 12.sp
            )
        )
        Text(
            text = snapshot.cityName,
            style = TextStyle(
                color = ColorProvider(colors.onContainer),
                fontSize = 11.sp
            )
        )
        if (wide && snapshot.hourly.isNotEmpty()) {
            HourlyStrip(snapshot, colors.onContainer)
        }
    }
}

@Composable
private fun HourlyStrip(snapshot: WidgetSnapshot, contentColor: Color) {
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
                modifier = GlanceModifier.padding(horizontal = 6.dp),
                horizontalAlignment = Alignment.Horizontal.CenterHorizontally
            ) {
                Text(
                    text = "%02d".format(hour.hourOfDay),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = 10.sp)
                )
                Text(
                    text = conditionGlyph(condition, snapshot.isDay),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = 14.sp)
                )
                Text(
                    text = temperatureText(hour.temperatureCelsius, snapshot.unit),
                    style = TextStyle(color = ColorProvider(contentColor), fontSize = 11.sp)
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
