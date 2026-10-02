package io.github.rojhatince47.weatherchan.core.di

import javax.inject.Qualifier

/**
 * Şehir listesinin DataStore'u. Ayar DataStore'u ile aynı tipte oldukları için
 * Dagger ayırıcı bir nitelik gerektirir.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class CityDataStore

/** Tema ve sıcaklık biriminin DataStore'u. */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class SettingsDataStore

/**
 * Hava durumu önbelleğinin DataStore'u.
 *
 * Şehir dosyasından **ayrı dosya** kullanır: aynı dosya paylaşılsaydı her
 * önbellek yazma `cities()` akışını yeniden yayımlatır ve tur-yaz-tur
 * sonsuz döngüsü oluşurdu.
 */
@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class WeatherCacheDataStore
