package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Daylight
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DaylightWindow
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject
import kotlin.math.roundToInt

class WttrInWeatherMapper @Inject constructor() {

    fun toCityWeather(city: City, dto: WttrInDto): CityWeather {
        val current = dto.currentCondition.firstOrNull() ?: WttrInCurrentDto()
        return CityWeather(
            city = city,
            currentWeather = CurrentWeather(
                temperatureCelsius = current.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                condition = toCondition(current.weatherCode.toIntOrNull()),
                windSpeedKmh = current.windSpeedKmph.toDoubleOrNull()?.roundToInt() ?: 0,
                humidityPercent = current.humidity.toIntOrNull() ?: 0,
                // Yedek yolda saat-türetme yoktur; gündüz varsayımı bilinçlidir.
                isDay = true
            )
        )
    }

    fun toForecast(city: City, dto: WttrInDto): Forecast {
        val current = dto.currentCondition.firstOrNull() ?: WttrInCurrentDto()
        val days = dto.weather
        val firstDay = days.firstOrNull()
        val dayDate = firstDay?.let { runCatching { LocalDate.parse(it.date) }.getOrNull() }
        val daylightWindow = days.firstOrNull()?.astronomy?.firstOrNull()?.let { astro ->
            val rise = parseClock(astro.sunrise) ?: return@let null
            val set = parseClock(astro.sunset) ?: return@let null
            if (rise >= set) null else DaylightWindow(rise, set)
        }
        return Forecast(
            city = city,
            currentWeather = CurrentWeather(
                temperatureCelsius = current.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                condition = toCondition(current.weatherCode.toIntOrNull()),
                windSpeedKmh = current.windSpeedKmph.toDoubleOrNull()?.roundToInt() ?: 0,
                humidityPercent = current.humidity.toIntOrNull() ?: 0,
                // Yedek yolda saat-türetme yoktur; gündüz varsayımı bilinçlidir.
                isDay = true
            ),
            hourlyForecast = if (dayDate == null) {
                // Tarihsiz gün damgalanamaz — o günün dilimleri pencereye giremez.
                emptyList()
            } else {
                firstDay?.hourly?.take(8)?.map { hourly ->
                    val hour = hourly.time.toIntOrNull()?.div(100) ?: 0
                    HourlyForecast(
                        localDateTime = LocalDateTime.of(dayDate, LocalTime.of(hour, 0)),
                        hourOfDay = hour,
                        temperatureCelsius = hourly.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                        condition = toCondition(hourly.weatherCode.toIntOrNull()),
                        precipitationProbabilityPercent = hourly.chanceOfRain.toIntOrNull() ?: 0,
                        isDay = Daylight.isDay(hour, daylightWindow)
                    )
                }.orEmpty()
            },
            dailyForecast = days.mapIndexed { index, day ->
                DailyForecast(
                    dayOffset = index,
                    minTemperatureCelsius = day.minTempC.toDoubleOrNull()?.roundToInt() ?: 0,
                    maxTemperatureCelsius = day.maxTempC.toDoubleOrNull()?.roundToInt() ?: 0,
                    condition = toCondition(day.hourly.getOrNull(4)?.weatherCode?.toIntOrNull())
                )
            }
        )
    }

    fun toCity(dto: WttrInDto, fallbackName: String): City {
        val area = dto.nearestArea.firstOrNull() ?: WttrInAreaDto()
        val name = area.areaName.firstOrNull()?.value?.ifBlank { null } ?: fallbackName
        val country = area.country.firstOrNull()?.value
        val region = area.region.firstOrNull()?.value
        return City(
            name = name,
            region = region,
            country = country,
            coordinates = Coordinates(
                latitude = area.latitude.toDoubleOrNull() ?: 0.0,
                longitude = area.longitude.toDoubleOrNull() ?: 0.0
            )
        )
    }

    internal fun toCondition(code: Int?): WeatherCondition = when (code) {
        113 -> WeatherCondition.CLEAR
        116 -> WeatherCondition.PARTLY_CLOUDY
        119, 122, 143, 248, 260 -> WeatherCondition.CLOUDY
        // 266 hafif çisenti: WMO 51'in karşılığı, yumuşak kategoriye düşer.
        266 -> WeatherCondition.LIGHT_SHOWERS
        176, 263, 281, 284, 293, 296, 299, 302, 305, 308, 311, 314, 317, 320, 353, 356, 359, 362, 365 -> WeatherCondition.RAIN
        179, 182, 227, 230, 323, 326, 329, 332, 335, 338, 350, 368, 371, 374, 377 -> WeatherCondition.SNOW
        200, 386, 389, 392, 395 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    private fun parseClock(raw: String): LocalTime? =
        runCatching { LocalTime.parse(raw.trim().uppercase(Locale.ENGLISH), clockFormat) }.getOrNull()
}
