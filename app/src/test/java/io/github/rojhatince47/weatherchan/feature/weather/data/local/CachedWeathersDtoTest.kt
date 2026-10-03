package io.github.rojhatince47.weatherchan.feature.weather.data.local

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class CachedWeathersDtoTest {

    private val json = Json { ignoreUnknownKeys = true }
    private val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))
    private val weather = CityWeather(
        city = ankara,
        currentWeather = CurrentWeather(
            temperatureCelsius = 14,
            condition = WeatherCondition.CLOUDY,
            windSpeedKmh = 6,
            humidityPercent = 45
        )
    )

    @Test
    fun `gidis donus kayipsizdir`() {
        val dto = listOf(weather).toCachedDto(fetchedAtEpochMillis = 1_750_000_000_000L)
        val raw = json.encodeToString(CachedWeathersDto.serializer(), dto)
        val back = json.decodeFromString<CachedWeathersDto>(raw).toDomain()
        assertEquals(1_750_000_000_000L, back.fetchedAt.toEpochMilli())
        assertEquals(listOf(weather), back.weathers)
    }

    @Test
    fun `bilinmeyen kosul cozumu patlatir`() {
        // Alan-düzeyi yedekleme YOKTUR: bilinmeyen enum adı tüm blob'u
        // geçersiz kılar; `WeatherCachePreferences` bunu yakalayıp
        // önbellek-yok gibi davranır (bozuk → yok felsefesi).
        val raw = """{"fetchedAtEpochMillis":1,"weathers":[""" +
            """{"city":{"name":"A","latitude":1.0,"longitude":2.0},"current":{"temperatureCelsius":1,"condition":"FIRTINA","windSpeedKmh":1,"humidityPercent":1,"isDay":true}}]}"""
        var threw = false
        try {
            json.decodeFromString<CachedWeathersDto>(raw).toDomain()
        } catch (e: IllegalArgumentException) {
            threw = true
        }
        assertEquals(true, threw)
    }

    @Test
    fun `gece bayragi gidis donuste korunur`() {
        val dto = listOf(weather.copy(currentWeather = weather.currentWeather.copy(isDay = false)))
            .toCachedDto(fetchedAtEpochMillis = 1L)
        val raw = json.encodeToString(CachedWeathersDto.serializer(), dto)
        val back = json.decodeFromString<CachedWeathersDto>(raw).toDomain()
        assertEquals(false, back.weathers.single().currentWeather.isDay)
    }

    @Test
    fun `isDay alani olmayan eski blob reddedilir`() {
        // Alan defaultsuzdur: eski sürüm kaydı decode'da elenir → önbellek-yok
        // sayılır → tek seferlik yeniden çekme ile kendini iyileştirir.
        val raw = """{"fetchedAtEpochMillis":1,"weathers":[""" +
            """{"city":{"name":"A","latitude":1.0,"longitude":2.0},"current":{"temperatureCelsius":1,"condition":"CLEAR","windSpeedKmh":1,"humidityPercent":1}}]}"""
        var threw = false
        try {
            json.decodeFromString<CachedWeathersDto>(raw)
        } catch (e: SerializationException) {
            threw = true
        }
        assertEquals(true, threw)
    }
}
