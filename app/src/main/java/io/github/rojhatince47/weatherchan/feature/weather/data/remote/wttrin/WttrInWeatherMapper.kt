package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

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
            hourlyForecast = days.firstOrNull()?.hourly?.take(8)?.map { hourly ->
                HourlyForecast(
                    hourOfDay = hourly.time.toIntOrNull()?.div(100) ?: 0,
                    temperatureCelsius = hourly.tempC.toDoubleOrNull()?.roundToInt() ?: 0,
                    condition = toCondition(hourly.weatherCode.toIntOrNull()),
                    precipitationProbabilityPercent = hourly.chanceOfRain.toIntOrNull() ?: 0
                )
            }.orEmpty(),
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
}
