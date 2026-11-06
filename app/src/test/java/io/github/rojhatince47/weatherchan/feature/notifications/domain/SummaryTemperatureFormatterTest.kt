package io.github.rojhatince47.weatherchan.feature.notifications.domain

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class SummaryTemperatureFormatterTest {

    @Test
    fun `santigrat aynen yazilir`() {
        assertEquals("22°C", SummaryTemperatureFormatter.temperature(22, TemperatureUnit.CELSIUS))
        assertEquals("-3°C", SummaryTemperatureFormatter.temperature(-3, TemperatureUnit.CELSIUS))
    }

    @Test
    fun `fahrenheit cevrilir`() {
        // WeatherUiMapper ile aynı tam sayı formülü: C * 9 / 5 + 32.
        assertEquals("71°F", SummaryTemperatureFormatter.temperature(22, TemperatureUnit.FAHRENHEIT))
        assertEquals("68°F", SummaryTemperatureFormatter.temperature(20, TemperatureUnit.FAHRENHEIT))
    }
}
