package io.github.rojhatince47.weatherchan.feature.weather.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import javax.inject.Inject

class SampleWeatherDataSource @Inject constructor() {
    val cities: List<City> = listOf(
        City(1L, "İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98)),
        City(2L, "Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85)),
        City(3L, "İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14)),
        City(4L, "Antalya", "Akdeniz", "Türkiye", Coordinates(36.90, 30.70)),
        City(5L, "Trabzon", "Karadeniz", "Türkiye", Coordinates(41.00, 39.72))
    )

    val cityWeathers: List<CityWeather> = cities.map { city ->
        CityWeather(
            city = city,
            currentWeather = currentWeatherFor(city.id)
        )
    }

    fun forecast(cityId: Long): Forecast {
        val city = cities.first { it.id == cityId }
        return Forecast(
            city = city,
            currentWeather = currentWeatherFor(cityId),
            hourlyForecast = (0 until 24).map { hour ->
                HourlyForecast(
                    hourOfDay = hour,
                    temperatureCelsius = 15 + ((city.id + hour) % 10).toInt(),
                    condition = WeatherCondition.PARTLY_CLOUDY,
                    precipitationProbabilityPercent = (hour * 3) % 100
                )
            },
            dailyForecast = (0 until 7).map { day ->
                DailyForecast(
                    dayOffset = day,
                    minTemperatureCelsius = 10 + ((city.id + day) % 5).toInt(),
                    maxTemperatureCelsius = 20 + ((city.id + day) % 8).toInt(),
                    condition = WeatherCondition.CLOUDY
                )
            }
        )
    }

    private fun currentWeatherFor(cityId: Long): CurrentWeather = CurrentWeather(
        temperatureCelsius = 18 + (cityId % 7).toInt(),
        condition = WeatherCondition.CLEAR,
        windSpeedKmh = 8 + (cityId % 5).toInt(),
        humidityPercent = 50 + (cityId % 20).toInt()
    )
}
