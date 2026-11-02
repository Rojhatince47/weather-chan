package io.github.rojhatince47.weatherchan.feature.weather.presentation.components.companion

/** Dokunuş sesi portu; gerçek uygulama Task 7'de SoundPool ile gelir. */
interface CompanionAudioPlayer {
    fun playTap()

    fun release()
}

/** Test/preview varsayılanı: hiçbir ses çıkarmaz. */
object NoOpCompanionAudioPlayer : CompanionAudioPlayer {
    override fun playTap() = Unit

    override fun release() = Unit
}
