package io.github.rojhatince47.weatherchan.feature.weather.domain.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast

interface WeatherRepository {
    suspend fun cityWeathers(): AppResult<List<CityWeather>>

    suspend fun forecast(cityId: Long): AppResult<Forecast>
}
