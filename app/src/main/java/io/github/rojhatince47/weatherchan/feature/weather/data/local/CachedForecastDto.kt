package io.github.rojhatince47.weatherchan.feature.weather.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CachedForecastDto(
    @SerialName("fetchedAtEpochMillis") val fetchedAtEpochMillis: Long,
    @SerialName("forecast") val forecast: ForecastDto
)

@Serializable
internal data class ForecastDto(
    @SerialName("city") val city: CityDto,
    @SerialName("current") val current: CurrentWeatherDto,
    @SerialName("hourly") val hourly: List<HourlyForecastDto> = emptyList(),
    @SerialName("daily") val daily: List<DailyForecastDto> = emptyList()
)

@Serializable
internal data class HourlyForecastDto(
    @SerialName("hourOfDay") val hourOfDay: Int,
    @SerialName("temperatureCelsius") val temperatureCelsius: Int,
    @SerialName("condition") val condition: String,
    @SerialName("precipitationProbabilityPercent") val precipitationProbabilityPercent: Int,
    // Sonda kalmalı: WeatherCachePreferencesTest pozisyonel kurar.
    @SerialName("isDay") val isDay: Boolean = true
)

@Serializable
internal data class DailyForecastDto(
    @SerialName("dayOffset") val dayOffset: Int,
    @SerialName("minTemperatureCelsius") val minTemperatureCelsius: Int,
    @SerialName("maxTemperatureCelsius") val maxTemperatureCelsius: Int,
    @SerialName("condition") val condition: String
)

internal fun CachedForecastDto.toDomain(): CachedForecast = CachedForecast(
    fetchedAt = Instant.ofEpochMilli(fetchedAtEpochMillis),
    forecast = forecast.toDomain()
)

internal fun CachedForecast.toDto(): CachedForecastDto = CachedForecastDto(
    fetchedAtEpochMillis = fetchedAt.toEpochMilli(),
    forecast = forecast.toDto()
)

internal fun Forecast.toDto(): ForecastDto = ForecastDto(
    city = city.toDto(),
    current = currentWeather.toDto(),
    hourly = hourlyForecast.map { it.toDto() },
    daily = dailyForecast.map { it.toDto() }
)

private fun ForecastDto.toDomain(): Forecast = Forecast(
    city = city.toCity(),
    currentWeather = CurrentWeather(
        temperatureCelsius = current.temperatureCelsius,
        // Bilinmeyen ad bilerek yedeklenmez: fırlatır; repository
        // giriş-başı `runCatching` ile yalnız o girişi düşürür.
        condition = WeatherCondition.valueOf(current.condition),
        windSpeedKmh = current.windSpeedKmh,
        humidityPercent = current.humidityPercent,
        isDay = current.isDay
    ),
    hourlyForecast = hourly.map { it.toDomain() },
    dailyForecast = daily.map { it.toDomain() }
)

private fun HourlyForecast.toDto(): HourlyForecastDto = HourlyForecastDto(
    hourOfDay = hourOfDay,
    temperatureCelsius = temperatureCelsius,
    condition = condition.name,
    precipitationProbabilityPercent = precipitationProbabilityPercent,
    isDay = isDay
)

private fun HourlyForecastDto.toDomain(): HourlyForecast = HourlyForecast(
    hourOfDay = hourOfDay,
    temperatureCelsius = temperatureCelsius,
    condition = WeatherCondition.valueOf(condition),
    precipitationProbabilityPercent = precipitationProbabilityPercent,
    isDay = isDay
)

private fun DailyForecast.toDto(): DailyForecastDto = DailyForecastDto(
    dayOffset = dayOffset,
    minTemperatureCelsius = minTemperatureCelsius,
    maxTemperatureCelsius = maxTemperatureCelsius,
    condition = condition.name
)

private fun DailyForecastDto.toDomain(): DailyForecast = DailyForecast(
    dayOffset = dayOffset,
    minTemperatureCelsius = minTemperatureCelsius,
    maxTemperatureCelsius = maxTemperatureCelsius,
    condition = WeatherCondition.valueOf(condition)
)
