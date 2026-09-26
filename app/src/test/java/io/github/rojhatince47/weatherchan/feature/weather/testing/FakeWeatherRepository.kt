package io.github.rojhatince47.weatherchan.feature.weather.testing
import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.data.local.SampleWeatherDataSource
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred

class FakeWeatherRepository(
    private val dataSource: SampleWeatherDataSource = SampleWeatherDataSource()
) : WeatherRepository {

    var loadGate = CompletableDeferred<Unit>()
    var forecastGate = CompletableDeferred<Unit>()
    var throwCancellation = false
    var failForecast = false

    override suspend fun cityWeathers(): AppResult<List<CityWeather>> {
        if (throwCancellation) {
            throw CancellationException("Test iptali")
        }
        loadGate.await()
        return AppResult.Success(dataSource.cityWeathers)
    }

    override suspend fun forecast(cityId: Long): AppResult<Forecast> {
        forecastGate.await()
        if (failForecast) {
            return AppResult.Failure(AppError.NotFound)
        }
        return AppResult.Success(dataSource.forecast(cityId))
    }
}
