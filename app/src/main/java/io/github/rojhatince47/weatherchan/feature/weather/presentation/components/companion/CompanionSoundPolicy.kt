package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

/**
 * Dokunuş sesinin çalınıp çalınmayacağına karar verir.
 *
 * [enabled] kullanıcının Ayarlar tercihidir; [silentMode] cihaz sessiz veya
 * titreşim modundaysa `true` gelir. Saf fonksiyon: Android'e bağlı değildir,
 * birim testiyle doğrulanır.
 */
internal fun shouldPlayCompanionSound(enabled: Boolean, silentMode: Boolean): Boolean = enabled && !silentMode

/**
 * SoundPool `load` çağrısı kaynak kimliği değil **örnek kimliği** döndürür;
 * `loadComplete` de örnek kimliğini verir. Bu yüzden yüklü klip listesi
 * kaynak kimliğiyle değil, kaynak → örnek eşlemesi üzerinden seçilir.
 */
internal fun availableTapClips(clips: List<Int>, sampleIdByResId: Map<Int, Int>, loadedSampleIds: Set<Int>): List<Int> =
    clips.filter { resId ->
        sampleIdByResId[resId]?.let { it in loadedSampleIds } == true
    }
