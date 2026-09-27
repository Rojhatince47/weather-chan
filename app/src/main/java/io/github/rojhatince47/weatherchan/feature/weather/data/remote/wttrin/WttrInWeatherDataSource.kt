package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.common.result.runCatchingApp
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import javax.inject.Inject
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

class WttrInWeatherDataSource @Inject constructor(
    private val api: WttrInApi,
    private val mapper: WttrInWeatherMapper,
    private val errorMapper: ErrorMapper
) {

    suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> = errorMapper.runCatchingApp {
        coroutineScope {
            cities.map { city ->
                async {
                    val dto = api.weather(locationOf(city))
                    mapper.toCityWeather(city, dto)
                }
            }.awaitAll()
        }
    }

    suspend fun forecast(city: City): AppResult<Forecast> = errorMapper.runCatchingApp {
        val dto = api.weather(locationOf(city))
        mapper.toForecast(city, dto)
    }

    suspend fun searchCities(query: String): AppResult<List<City>> = errorMapper.runCatchingApp {
        val dto = api.weather(location = query.trim())
        listOf(mapper.toCity(dto, fallbackName = query.trim()))
    }

    private fun locationOf(city: City): String = "${city.coordinates.latitude},${city.coordinates.longitude}"
}
