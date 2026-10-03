package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WttrInWeatherMapperTest {

    @Test
    fun `yedek kaynak gunduz varsayar`() {
        // Yedek yolda saat-türetme yoktur (bilinçli kapsam kesiti); veri doğru,
        // sunum gündüz varsayımıyla çizilir.
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val mapper = WttrInWeatherMapper()
        assertTrue(mapper.toCityWeather(city, WttrInDto()).currentWeather.isDay)
    }

    @Test
    fun `hafif ciselti yumusak kategoriye duser`() {
        // WMO 51'in karşılığı: 266 hafif çisenti.
        val city = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val mapper = WttrInWeatherMapper()
        val dto = WttrInDto(
            currentCondition = listOf(WttrInCurrentDto(weatherCode = "266"))
        )
        assertEquals(
            WeatherCondition.LIGHT_SHOWERS,
            mapper.toCityWeather(city, dto).currentWeather.condition
        )
    }
}
