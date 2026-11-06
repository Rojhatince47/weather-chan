package io.github.rojhatince47.weatherchan.feature.notifications.domain

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit

/**
 * Özet bildirimindeki sıcaklık metni. Ana ekranla aynı tam sayı formülü
 * kullanılır ki iki yerde farklı sayı görünmesin.
 */
object SummaryTemperatureFormatter {
    fun temperature(celsius: Int, unit: TemperatureUnit): String = when (unit) {
        TemperatureUnit.CELSIUS -> "$celsius°C"
        TemperatureUnit.FAHRENHEIT -> "${celsius * 9 / 5 + 32}°F"
    }
}
