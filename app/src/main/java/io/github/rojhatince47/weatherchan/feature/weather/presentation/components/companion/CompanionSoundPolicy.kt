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
 * `loadComplete` de örnek kimliğini verir. Bu yüzden istenen klip, kaynak →
 * örnek eşlemesi ve yüklü örnek kümesi üzerinden çözülür.
 */
internal fun sampleIdForClip(clips: List<Int>, clipIndex: Int, sampleIdByResId: Map<Int, Int>, loadedSampleIds: Set<Int>): Int? {
    val resId = clips.getOrNull(clipIndex) ?: return null
    val sampleId = sampleIdByResId[resId] ?: return null
    return sampleId.takeIf { it in loadedSampleIds }
}
