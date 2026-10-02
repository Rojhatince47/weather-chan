package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import javax.inject.Inject
import kotlin.math.roundToInt

class OpenMeteoWeatherMapper @Inject constructor() {

    fun toCityWeather(city: City, dto: OpenMeteoForecastDto): CityWeather = CityWeather(
        city = city,
        currentWeather = toCurrentWeather(dto.current)
    )

    fun toForecast(city: City, dto: OpenMeteoForecastDto): Forecast = Forecast(
        city = city,
        currentWeather = toCurrentWeather(dto.current),
        hourlyForecast = dto.hourly.temperature.indices.take(24).map { index ->
            HourlyForecast(
                hourOfDay = hourOf(dto.hourly.time.getOrNull(index)),
                temperatureCelsius = dto.hourly.temperature.getOrNull(index)?.roundToInt() ?: 0,
                condition = toCondition(dto.hourly.weatherCode.getOrNull(index)),
                precipitationProbabilityPercent = dto.hourly.precipitationProbability.getOrNull(index) ?: 0
            )
        },
        dailyForecast = dto.daily.time.indices.map { index ->
            DailyForecast(
                dayOffset = index,
                minTemperatureCelsius = dto.daily.temperatureMin.getOrNull(index)?.roundToInt() ?: 0,
                maxTemperatureCelsius = dto.daily.temperatureMax.getOrNull(index)?.roundToInt() ?: 0,
                condition = toCondition(dto.daily.weatherCode.getOrNull(index))
            )
        }
    )

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

    private fun hourOf(isoTime: String?): Int = isoTime?.substringAfter('T', "")?.substringBefore(':')?.toIntOrNull() ?: 0

    internal fun toCondition(code: Int?): WeatherCondition = when (code) {
        0 -> WeatherCondition.CLEAR
        1, 2 -> WeatherCondition.PARTLY_CLOUDY
        3, 45, 48 -> WeatherCondition.CLOUDY
        in 51..57, in 61..67, in 80..82 -> WeatherCondition.RAIN
        in 71..77, 85, 86 -> WeatherCondition.SNOW
        95, 96, 99 -> WeatherCondition.THUNDERSTORM
        else -> WeatherCondition.CLOUDY
    }
}
