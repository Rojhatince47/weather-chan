package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

/**
 * Dokunuş sesinin çalınıp çalınmayacağına karar verir.
 *
 * [enabled] kullanıcının Ayarlar tercihidir; [silentMode] cihaz sessiz veya
 * titreşim modundaysa `true` gelir. Saf fonksiyon: Android'e bağlı değildir,
 * birim testiyle doğrulanır.
 */
internal fun shouldPlayCompanionSound(enabled: Boolean, silentMode: Boolean): Boolean = enabled && !silentMode
