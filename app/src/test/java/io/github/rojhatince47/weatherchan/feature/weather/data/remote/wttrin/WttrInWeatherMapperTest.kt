package io.github.rojhatince47.weatherchan.feature.weather.data.remote.wttrin

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
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
}
