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

    private val cityWeathersGates = ArrayDeque<CompletableDeferred<Unit>>()
    var forecastGate = CompletableDeferred<Unit>()
    var throwCancellation = false
    var failForecast = false
    var failCityWeathers = false
    var autoCompleteGates = false
    var finishedCityWeathersCalls = 0
        private set

    val cityWeathersCallCount: Int
        get() = cityWeathersGates.size

    fun gateForCityWeathersCall(index: Int): CompletableDeferred<Unit> = cityWeathersGates[index]

    fun completeAllCityWeathersGates() {
        cityWeathersGates
            .filterNot { it.isCompleted }
            .forEach { it.complete(Unit) }
    }

    override suspend fun cityWeathers(): AppResult<List<CityWeather>> {
        if (throwCancellation) {
            throw CancellationException("Test iptali")
        }
        if (!autoCompleteGates) {
            val gate = CompletableDeferred<Unit>()
            cityWeathersGates.addLast(gate)
            gate.await()
        }
        finishedCityWeathersCalls++
        if (failCityWeathers) {
            return AppResult.Failure(AppError.Network)
        }
        return AppResult.Success(dataSource.cityWeathers)
    }

    override suspend fun forecast(cityId: Long): AppResult<Forecast> {
        if (!autoCompleteGates) {
            forecastGate.await()
        }
        if (failForecast) {
            return AppResult.Failure(AppError.NotFound)
        }
        return AppResult.Success(dataSource.forecast(cityId))
    }
}
