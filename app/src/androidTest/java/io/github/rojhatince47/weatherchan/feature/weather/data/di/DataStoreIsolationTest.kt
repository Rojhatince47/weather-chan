package io.github.rojhatince47.weatherchan.feature.weather.data.di

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import io.github.rojhatince47.weatherchan.feature.city.data.di.CityDataModule
import org.junit.Assert.assertNotSame
import org.junit.Test

/**
 * Regresyon kilidi: önbellek yazmaları şehir akışını tetiklememelidir.
 *
 * İki preferences aynı DataStore dosyasını paylaşırsa her `cache.write()`
 * `cities()`'i yeniden yayımlatır → yeni tur → yeni yazma → SONSUZ DÖNGÜ
 * (cihazda ~8/sn istek gözlendi, 900+ istek ve artıyordu). Ayrı dosyalar bu
 * geri-beslemeyi yapısal olarak imkânsız kılar. Bu test veri yazmaz.
 */
class DataStoreIsolationTest {

    @Test
    fun cacheUsesSeparateStoreFile() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cityStore = CityDataModule.provideCityDataStore(context)
        val cacheStore = WeatherDataModule.provideWeatherCacheStore(context)
        assertNotSame(
            "Önbellek şehir dosyasını paylaşmamalı (fetch döngüsü yapar)",
            cityStore,
            cacheStore
        )
    }
}
