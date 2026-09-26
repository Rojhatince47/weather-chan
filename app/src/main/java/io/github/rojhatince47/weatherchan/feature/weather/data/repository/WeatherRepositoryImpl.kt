package io.github.rojhatince47.weatherchan.feature.weather.data.repository

import io.github.rojhatince47.weatherchan.core.common.error.ErrorMapper
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.common.result.runCatchingApp
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import javax.inject.Inject

class WeatherRepositoryImpl @Inject constructor(
    private val sampleDataSource: SampleWeatherDataSource,
    private val errorMapper: ErrorMapper
) : WeatherRepository {

    override suspend fun cityWeathers(): AppResult<List<CityWeather>> = errorMapper.runCatchingApp { sampleDataSource.cityWeathers }

    override suspend fun forecast(cityId: Long): AppResult<Forecast> = errorMapper.runCatchingApp { sampleDataSource.forecast(cityId) }
}
