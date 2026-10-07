package io.github.rojhatince47.weatherchan.feature.weather.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedWeathers
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
internal data class CachedWeathersDto(
    @SerialName("fetchedAtEpochMillis") val fetchedAtEpochMillis: Long,
    @SerialName("weathers") val weathers: List<CityWeatherDto> = emptyList()
)

@Serializable
internal data class CityWeatherDto(
    @SerialName("city") val city: CityDto,
    @SerialName("current") val current: CurrentWeatherDto
)

/** `CityPreferences.CityDto` `private` olduğu için aynı şekil buraya kopyalanır. */
@Serializable
internal data class CityDto(
    @SerialName("name") val name: String,
    @SerialName("region") val region: String? = null,
    @SerialName("country") val country: String? = null,
    @SerialName("latitude") val latitude: Double,
    @SerialName("longitude") val longitude: Double,
    @SerialName("isLocation") val isLocation: Boolean = false
)

@Serializable
internal data class CurrentWeatherDto(
    @SerialName("temperatureCelsius") val temperatureCelsius: Int,
    @SerialName("condition") val condition: String,
    @SerialName("windSpeedKmh") val windSpeedKmh: Int,
    @SerialName("humidityPercent") val humidityPercent: Int,
    @SerialName("isDay") val isDay: Boolean
)

internal fun List<CityWeather>.toCachedDto(fetchedAtEpochMillis: Long): CachedWeathersDto = CachedWeathersDto(
    fetchedAtEpochMillis = fetchedAtEpochMillis,
    weathers = map { it.toDto() }
)

internal fun CityWeather.toDto(): CityWeatherDto = CityWeatherDto(
    city = city.toDto(),
    current = currentWeather.toDto()
)

internal fun CachedWeathersDto.toDomain(): CachedWeathers = CachedWeathers(
    fetchedAt = Instant.ofEpochMilli(fetchedAtEpochMillis),
    weathers = weathers.map { it.toDomain() }
)

internal fun City.toDto(): CityDto = CityDto(
    name = name,
    region = region,
    country = country,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude,
    isLocation = isLocation
)

internal fun CityDto.toCity(): City = City(
    name = name,
    region = region,
    country = country,
    coordinates = Coordinates(latitude = latitude, longitude = longitude),
    isLocation = isLocation
)

internal fun CurrentWeather.toDto(): CurrentWeatherDto = CurrentWeatherDto(
    temperatureCelsius = temperatureCelsius,
    condition = condition.name,
    windSpeedKmh = windSpeedKmh,
    humidityPercent = humidityPercent,
    isDay = isDay
)

private fun CityWeatherDto.toDomain(): CityWeather = CityWeather(
    city = city.toCity(),
    currentWeather = CurrentWeather(
        temperatureCelsius = current.temperatureCelsius,
        // Bilinmeyen ad bilerek yedeklenmez: fırlatır, blob-düzeyinde
        // `runCatching` yakalar, tüm önbellek yok sayılır.
        condition = WeatherCondition.valueOf(current.condition),
        windSpeedKmh = current.windSpeedKmh,
        humidityPercent = current.humidityPercent,
        isDay = current.isDay
    )
)
