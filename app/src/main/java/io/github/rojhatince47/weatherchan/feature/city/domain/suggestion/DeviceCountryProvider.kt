package io.github.rojhatince47.weatherchan.feature.city.domain.suggestion

/**
 * Cihazın bulunduğu ülke (ISO-2). İzin gerektirmez.
 *
 * Konum okunamadığında öneri dalını seçer; bu yüzden **null** "bilinmiyor"
 * demektir ve çağıran taraf dünya yedeğine düşer.
 */
fun interface DeviceCountryProvider {
    fun countryCode(): String?
}
