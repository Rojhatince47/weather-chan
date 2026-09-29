package io.github.rojhatince47.weatherchan.feature.weather.data.remote.openmeteo

import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.common.result.batchAll
import io.github.rojhatince47.weatherchan.core.common.result.runCatchingApp
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import javax.inject.Inject

class OpenMeteoWeatherDataSource @Inject constructor(
    private val api: OpenMeteoApi,
    private val geocodingApi: OpenMeteoGeocodingApi,
    private val mapper: OpenMeteoWeatherMapper,
    private val errorMapper: ErrorMapper
) {

    suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> = errorMapper.batchAll(cities) { city ->
        val dto = api.forecast(
            latitude = city.coordinates.latitude.toString(),
            longitude = city.coordinates.longitude.toString()
        )
        mapper.toCityWeather(city, dto)
    }

    suspend fun forecast(city: City): AppResult<Forecast> = errorMapper.runCatchingApp {
        val dto = api.forecast(
            latitude = city.coordinates.latitude.toString(),
            longitude = city.coordinates.longitude.toString()
        )
        mapper.toForecast(city, dto)
    }

    suspend fun searchCities(query: String): AppResult<List<City>> = errorMapper.runCatchingApp {
        geocodingApi.search(name = query).results.map(mapper::toCity)
    }
}
