package io.github.rojhatince47.weatherchan.feature.weather.testing

import io.github.rojhatince47.weatherchan.core.common.error.AppError
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CompletableDeferred

/**
 * Test amaçlı hava durumu deposu. Sentetik veriyi kendisi üretir; örnek veri
 * kaynağı üretim kodundan tamamen kaldırıldığı için artık geçirilen şehirlerden
 * hesaplanır.
 */
class FakeWeatherRepository : WeatherRepository {

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

    override suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> {
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
        return AppResult.Success(cities.map { it.toCityWeather() })
    }

    override suspend fun forecast(city: City): AppResult<Forecast> {
        if (!autoCompleteGates) {
            forecastGate.await()
        }
        if (failForecast) {
            return AppResult.Failure(AppError.NotFound)
        }
        return AppResult.Success(city.toForecast())
    }

    private fun City.toCityWeather(): CityWeather = CityWeather(
        city = this,
        currentWeather = syntheticCurrentWeather()
    )

    private fun City.syntheticCurrentWeather(): CurrentWeather = CurrentWeather(
        temperatureCelsius = 18 + (id % 7).toInt(),
        condition = WeatherCondition.CLEAR,
        windSpeedKmh = 8 + (id % 5).toInt(),
        humidityPercent = 50 + (id % 20).toInt()
    )

    private fun City.toForecast(): Forecast = Forecast(
        city = this,
        currentWeather = syntheticCurrentWeather(),
        hourlyForecast = (0 until 24).map { hour ->
            HourlyForecast(
                hourOfDay = hour,
                temperatureCelsius = 15 + ((id + hour) % 10).toInt(),
                condition = WeatherCondition.PARTLY_CLOUDY,
                precipitationProbabilityPercent = (hour * 3) % 100
            )
        },
        dailyForecast = (0 until 7).map { day ->
            DailyForecast(
                dayOffset = day,
                minTemperatureCelsius = 10 + ((id + day) % 5).toInt(),
                maxTemperatureCelsius = 20 + ((id + day) % 8).toInt(),
                condition = WeatherCondition.CLOUDY
            )
        }
    )
}
