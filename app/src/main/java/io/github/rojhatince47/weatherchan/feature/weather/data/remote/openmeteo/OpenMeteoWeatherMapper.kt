package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

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
import javax.inject.Inject
import kotlin.math.roundToInt

class OpenMeteoWeatherMapper @Inject constructor() {

    fun toCityWeather(city: City, dto: OpenMeteoForecastDto): CityWeather = CityWeather(
        city = city,
        currentWeather = toCurrentWeather(dto.current)
    )

    fun toForecast(city: City, dto: OpenMeteoForecastDto): Forecast {
        val windows = sunWindows(dto)
        return Forecast(
            city = city,
            currentWeather = toCurrentWeather(dto.current),
            hourlyForecast = dto.hourly.temperature.indices.mapNotNull { index ->
                val stamp = dateTimeOf(dto.hourly.time.getOrNull(index)) ?: return@mapNotNull null
                HourlyForecast(
                    localDateTime = stamp,
                    hourOfDay = stamp.hour,
                    temperatureCelsius = dto.hourly.temperature.getOrNull(index)?.roundToInt() ?: 0,
                    condition = toCondition(dto.hourly.weatherCode.getOrNull(index)),
                    precipitationProbabilityPercent = dto.hourly.precipitationProbability.getOrNull(index) ?: 0,
                    isDay = Daylight.isDay(stamp.hour, windows[stamp.toLocalDate()])
                )
            },
            dailyForecast = dto.daily.time.indices.map { index ->
                DailyForecast(
                    dayOffset = index,
                    minTemperatureCelsius = dto.daily.temperatureMin.getOrNull(index)?.roundToInt() ?: 0,
                    maxTemperatureCelsius = dto.daily.temperatureMax.getOrNull(index)?.roundToInt() ?: 0,
                    condition = toCondition(dto.daily.weatherCode.getOrNull(index))
                )
            },
            utcOffsetSeconds = dto.utcOffsetSeconds
        )
    }

    fun toCity(dto: OpenMeteoGeocodingResultDto): City = City(
        name = dto.name,
        region = dto.region,
        country = dto.country,
        coordinates = Coordinates(
            latitude = dto.latitude,
            longitude = dto.longitude
        )
    )

    private fun toCurrentWeather(dto: OpenMeteoCurrentDto): CurrentWeather = CurrentWeather(
        temperatureCelsius = dto.temperature.roundToInt(),
        condition = toCondition(dto.weatherCode),
        windSpeedKmh = dto.windSpeed.roundToInt(),
        humidityPercent = dto.humidity,
        isDay = dto.isDay?.let { it == 1 } ?: true
    )

    /** ISO-8601 yerel damga (`yyyy-MM-ddTHH:mm[:ss]`); bozuksa null — kayıt elenir. */
    private fun dateTimeOf(isoTime: String?): LocalDateTime? = isoTime?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

    private fun sunWindows(dto: OpenMeteoForecastDto): Map<LocalDate, DaylightWindow> = dto.daily.time.indices.mapNotNull { i ->
        val date = runCatching { LocalDate.parse(dto.daily.time[i]) }.getOrNull() ?: return@mapNotNull null
        val rise = dateTimeOf(dto.daily.sunrise.getOrNull(i))?.toLocalTime() ?: return@mapNotNull null
        val set = dateTimeOf(dto.daily.sunset.getOrNull(i))?.toLocalTime() ?: return@mapNotNull null
        date to DaylightWindow(rise, set)
    }.toMap()

    internal fun toCondition(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3, 45, 48 -> WeatherCondition.CLOUDY
        // 51 (hafif çisenti) ve 80 (hafif sağanak): eser miktarı tam yağmur
        // gibi göstermek kullanıcı gözünde yanlıştır.
        51, 80 -> WeatherCondition.LIGHT_SHOWERS
        in 52..57, in 61..67, in 81..82 -> WeatherCondition.RAIN
        in 71..77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }
}
