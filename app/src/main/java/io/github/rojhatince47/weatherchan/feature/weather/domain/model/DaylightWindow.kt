package io.github.rojhatince47.weatherchan.feature.weather.domain.model

import java.time.LocalTime

/**
 * Gün doğuş-batış penceresi. [Daylight.isDay] saatlik isDay türetmesinin
 * tek kuralıdır: pencere varsa saat pencerede mi, yoksa 06-18 yedeği.
 */
data class DaylightWindow(val sunrise: LocalTime, val sunset: LocalTime) {
    fun isDayAt(hourOfDay: Int): Boolean {
        // Bozuk/sıfır genişlikli pencere (sunrise >= sunset): kurala düş.
        if (sunrise >= sunset) return hourOfDay in 6 until 18
        val t = LocalTime.of(hourOfDay.coerceIn(0, 23), 0)
        return t >= sunrise && t < sunset
    }
}

object Daylight {
    fun isDay(hourOfDay: Int, window: DaylightWindow?): Boolean = window?.isDayAt(hourOfDay) ?: (hourOfDay in 6 until 18)
}
