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
