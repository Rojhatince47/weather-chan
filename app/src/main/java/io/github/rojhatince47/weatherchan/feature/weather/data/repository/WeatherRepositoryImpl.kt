package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo.OpenMeteoWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin.WttrInWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val primary: OpenMeteoWeatherDataSource,
    private val backup: WttrInWeatherDataSource
) : WeatherRepository {

    override suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> =
        when (val primaryResult = primary.cityWeathers(cities)) {
            is AppResult.Success -> primaryResult
            is AppResult.Failure -> backup.cityWeathers(cities)
        }

    override suspend fun forecast(city: City): AppResult<Forecast> = when (val primaryResult = primary.forecast(city)) {
        is AppResult.Success -> primaryResult
        is AppResult.Failure -> backup.forecast(city)
    }

    override suspend fun search(query: String): AppResult<List<City>> = when (val primaryResult = primary.searchCities(query)) {
        is AppResult.Success -> primaryResult
        is AppResult.Failure -> backup.searchCities(query)
    }
}
