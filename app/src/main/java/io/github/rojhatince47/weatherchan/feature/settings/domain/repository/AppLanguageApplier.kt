package io.github.rojhatince47.weatherchan.feature.settings.domain.repository

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage

/**
 * Uygulama dilini okur/uygular. Kalıcılığı AppCompat'in per-app language
 * mekanizması üstlenir (API<33 SharedPreferences, 33+ sistem ayarı);
 * uygulamada ikinci bir kopya (DataStore) tutulmaz (spec §5).
 */
interface AppLanguageApplier {
    val current: AppLanguage

    fun apply(language: AppLanguage)
}
