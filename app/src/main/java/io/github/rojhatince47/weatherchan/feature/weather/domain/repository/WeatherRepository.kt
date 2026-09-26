package io.github.rojhatince47.weatherchan.feature.weather.domain.repository

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast

interface WeatherRepository {
    fun cityWeathers(): List<CityWeather>

    fun forecast(cityId: Long): Forecast
}
