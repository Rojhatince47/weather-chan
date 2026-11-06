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
                isDay = currentIsDay(dto, current)
            )
        )
    }

    fun toForecast(city: City, dto: WttrInDto): Forecast {
        val current = dto.currentCondition.firstOrNull() ?: WttrInCurrentDto()
        val days = dto.weather
        val windows: Map<LocalDate, DaylightWindow> = days.mapNotNull { day ->
            val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@mapNotNull null
            val astro = day.astronomy.firstOrNull() ?: return@mapNotNull null
            val rise = parseClock(astro.sunrise) ?: return@mapNotNull null
            val set = parseClock(astro.sunset) ?: return@mapNotNull null
            if (rise >= set) return@mapNotNull null
            date to DaylightWindow(rise, set)
        }.toMap()
        return Forecast(
            city = city,
            currentWeather = CurrentWeather(
                temperatureCelsius = current.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                condition = toCondition(current.weatherCode.toIntOrNull()),
                windSpeedKmh = current.windSpeedKmph.toDoubleOrNull()?.roundToInt() ?: 0,
                humidityPercent = current.humidity.toIntOrNull() ?: 0,
                isDay = currentIsDay(dto, current)
            ),
            hourlyForecast = days.flatMap { day ->
                val date = runCatching { LocalDate.parse(day.date) }.getOrNull() ?: return@flatMap emptyList()
                day.hourly.mapNotNull { hourly ->
                    val total = hourly.time?.trim()?.toIntOrNull() ?: return@mapNotNull null
                    val hour = total / 100
                    if (total < 0 || hour !in 0..23) return@mapNotNull null
                    HourlyForecast(
                        localDateTime = LocalDateTime.of(date, LocalTime.of(hour, 0)),
                        hourOfDay = hour,
                        temperatureCelsius = hourly.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                        condition = toCondition(hourly.weatherCode.toIntOrNull()),
                        precipitationProbabilityPercent = hourly.chanceOfRain.toIntOrNull() ?: 0,
                        isDay = Daylight.isDay(hour, windows[date])
                    )
                }
            },
            dailyForecast = days.mapIndexed { index, day ->
                DailyForecast(
                    dayOffset = index,
                    minTemperatureCelsius = day.minTempC.toDoubleOrNull()?.roundToInt() ?: 0,
                    maxTemperatureCelsius = day.maxTempC.toDoubleOrNull()?.roundToInt() ?: 0,
                    condition = toCondition(day.hourly.getOrNull(4)?.weatherCode?.toIntOrNull())
                )
            },
            utcOffsetSeconds = approximateOffsetSeconds(city)
        )
    }

    /**
     * wttr.in UTC farkı vermez; boylamdan yaklaşık dilim hesaplanır
     * (15° ≈ 1 saat). Open-Meteo varken bu yol kullanılmaz.
     */
    private fun approximateOffsetSeconds(city: City): Int = (city.coordinates.longitude / 15.0).roundToInt() * 3600

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

    /**
     * Yedek kaynakta `current.is_day` yoktur; yerel gözlem saatini bugünün
     * gün doğumu/batımı penceresiyle karşılaştırır. Saat veya astronomi
     * eksikse gündüz varsayılır (eski davranış).
     */
    private fun currentIsDay(dto: WttrInDto, current: WttrInCurrentDto): Boolean {
        val time = parseClock(current.observationTime) ?: return true
        val day = dto.weather.firstOrNull() ?: return true
        val astro = day.astronomy.firstOrNull() ?: return true
        val rise = parseClock(astro.sunrise) ?: return true
        val set = parseClock(astro.sunset) ?: return true
        if (rise >= set) return true
        return Daylight.isDay(time.hour, DaylightWindow(rise, set))
    }

    private val clockFormat = DateTimeFormatter.ofPattern("h:mm a", Locale.ENGLISH)

    private fun parseClock(raw: String): LocalTime? =
        runCatching { LocalTime.parse(raw.trim().uppercase(Locale.ENGLISH), clockFormat) }.getOrNull()
}
