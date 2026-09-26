package io.github.rojhatince47.weatherchan.feature.weather.testing

import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import kotlinx.coroutines.CompletableDeferred

class FakeWeatherRepository(
    private val dataSource: SampleWeatherDataSource = SampleWeatherDataSource()
) : WeatherRepository {

    val loadGate = CompletableDeferred<Unit>()

    override suspend fun cityWeathers(): AppResult<List<CityWeather>> {
        loadGate.await()
        return AppResult.Success(dataSource.cityWeathers)
    }

    override suspend fun forecast(cityId: Long): AppResult<Forecast> = AppResult.Success(dataSource.forecast(cityId))
}
