package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.DailyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.HourlyForecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherUiMapperTest {
    private val mapper = WeatherUiMapper()

    private val istanbul = City(
        name = "İstanbul",
        region = "Marmara",
        country = "Türkiye",
        coordinates = Coordinates(latitude = 41.01, longitude = 28.98)
    )

    private fun cityWeather() = CityWeather(
        city = istanbul,
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        )
    )

    private fun forecast() = Forecast(
        city = istanbul,
        currentWeather = CurrentWeather(
            temperatureCelsius = 18,
            condition = WeatherCondition.CLEAR,
            windSpeedKmh = 8,
            humidityPercent = 50
        ),
        hourlyForecast = List(24) { hour ->
            HourlyForecast(
                hourOfDay = hour,
                temperatureCelsius = 15 + hour % 10,
                condition = WeatherCondition.PARTLY_CLOUDY,
                precipitationProbabilityPercent = hour * 3 % 100
            )
        },
        dailyForecast = List(7) { day ->
            DailyForecast(
                dayOffset = day,
                minTemperatureCelsius = 10 + day % 5,
                maxTemperatureCelsius = 20 + day % 8,
                condition = WeatherCondition.CLOUDY
            )
        }
    )

    @Test
    fun `maps city weather to ui model`() {
        val cityWeather = cityWeather()
        val uiModel = mapper.toCityWeatherUiModel(cityWeather)
        val weather = uiModel.weather as CardWeather.Loaded

        assertEquals(cityWeather.city.id, uiModel.cityId)
        assertEquals(cityWeather.city.name, uiModel.name)
        assertTrue(weather.temperatureText.endsWith("°C"))
        assertEquals(cityWeather.currentWeather.condition, weather.condition)
        assertEquals(
            UiText.Resource(R.string.condition_clear),
            weather.conditionLabel
        )
    }

    /**
     * Hava durumu alınamayan şehir de listede kalır; kart sıcaklık yerine
     * nedeni ve eylemi gösterir. Yüksüz kartın varlığı bu eşlemeye bağlıdır.
     */
    @Test
    fun `maps city without weather to unavailable card`() {
        val uiModel = mapper.toUnavailable(istanbul)

        assertEquals(istanbul.id, uiModel.cityId)
        assertEquals("İstanbul", uiModel.name)
        assertEquals("Marmara", uiModel.region)
        assertEquals("Türkiye", uiModel.country)
        assertTrue(uiModel.weather is CardWeather.Unavailable)
    }

    @Test
    fun `maps forecast to detail ui model`() {
        val forecast = forecast()
        val uiModel = mapper.toForecastDetailUiModel(forecast)

        assertEquals(24, uiModel.hourlyForecast.size)
        assertEquals(7, uiModel.dailyForecast.size)
        assertTrue(uiModel.currentWeather.temperatureText.endsWith("°C"))
        assertEquals(
            UiText.Resource(R.string.forecast_today),
            uiModel.dailyForecast.first().dateText
        )
        assertEquals(0, uiModel.dailyForecast.first().dayOffset)
    }

    @Test
    fun `gun adlari cekim gunune gore uretilir ve hafta kirpimini asar`() {
        val today = LocalDate.of(2026, 10, 8) // Perşembe

        val uiModel = mapper.toForecastDetailUiModel(forecast(), today = today)

        val expected = listOf(
            R.string.forecast_today,
            R.string.day_friday,
            R.string.day_saturday,
            R.string.day_sunday,
            R.string.day_monday,
            R.string.day_tuesday,
            R.string.day_wednesday
        )
        expected.forEachIndexed { index, resId ->
            assertEquals(UiText.Resource(resId), uiModel.dailyForecast[index].dateText)
        }
    }

    /**
     * Konum kartı görsel olarak ayrışmalı: kart "Konumunuz" etiketi taşır.
     * Bu bir görüntüleme kaygısı olduğu için bayrak UI modeline taşınır —
     * sıralama kaygısı değildir (o, depodaki City üzerinde yapılır).
     */
    @Test
    fun `konum karti ui modelinde isaretlenir`() {
        val locationWeather = cityWeather().copy(city = istanbul.copy(isLocation = true))

        assertTrue(mapper.toCityWeatherUiModel(locationWeather).isLocation)
        assertTrue(mapper.toUnavailable(istanbul.copy(isLocation = true)).isLocation)

        assertFalse(mapper.toCityWeatherUiModel(cityWeather()).isLocation)
        assertFalse(mapper.toUnavailable(istanbul).isLocation)
    }

    @Test
    fun `konum karti birlesik tam ad tasir`() {
        val derik = City("Derik", "Mardin", "Türkiye", Coordinates(37.36, 40.26), isLocation = true)

        val ui = mapper.toCityWeatherUiModel(cityWeather().copy(city = derik))

        assertEquals("Derik, Mardin", ui.name)
        assertEquals("Türkiye", ui.country)
        assertTrue(ui.isLocation)
    }

    @Test
    fun `converts temperatures to fahrenheit`() {
        val cityWeather = cityWeather()
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather,
            unit = TemperatureUnit.FAHRENHEIT
        )
        val expected = cityWeather.currentWeather.temperatureCelsius * 9 / 5 + 32
        val weather = uiModel.weather as CardWeather.Loaded

        assertEquals("$expected°F", weather.temperatureText)
    }

    @Test
    fun `gece acik ay simgesiyle gosterilir`() {
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather = cityWeather().copy(
                currentWeather = cityWeather().currentWeather.copy(condition = WeatherCondition.CLEAR, isDay = false)
            )
        )
        val weather = uiModel.weather as CardWeather.Loaded
        assertEquals("\uD83C\uDF19", weather.conditionEmoji)
    }

    @Test
    fun `gece parcasi bulutlu ay-bulut simgesiyle gosterilir`() {
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather = cityWeather().copy(
                currentWeather = cityWeather().currentWeather.copy(condition = WeatherCondition.PARTLY_CLOUDY, isDay = false)
            )
        )
        val weather = uiModel.weather as CardWeather.Loaded
        assertEquals("\uD83C\uDF19\u2601\uFE0F", weather.conditionEmoji)
    }

    @Test
    fun `gece diger kosullar degismez`() {
        listOf(
            WeatherCondition.CLOUDY,
            WeatherCondition.RAIN,
            WeatherCondition.SNOW,
            WeatherCondition.THUNDERSTORM
        ).forEach { condition ->
            val night = mapper.toCityWeatherUiModel(
                cityWeather = cityWeather().copy(
                    currentWeather = cityWeather().currentWeather.copy(condition = condition, isDay = false)
                )
            )
            val day = mapper.toCityWeatherUiModel(
                cityWeather = cityWeather().copy(
                    currentWeather = cityWeather().currentWeather.copy(condition = condition, isDay = true)
                )
            )
            assertEquals(
                (day.weather as CardWeather.Loaded).conditionEmoji,
                (night.weather as CardWeather.Loaded).conditionEmoji
            )
        }
    }

    @Test
    fun `hafif saganak gunduz yumusak simge ve etiketle gosterilir`() {
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather = cityWeather().copy(
                currentWeather = cityWeather().currentWeather.copy(condition = WeatherCondition.LIGHT_SHOWERS, isDay = true)
            )
        )
        val weather = uiModel.weather as CardWeather.Loaded
        assertEquals("🌦️", weather.conditionEmoji)
        assertEquals(UiText.Resource(R.string.condition_light_showers), weather.conditionLabel)
    }

    @Test
    fun `hafif saganak gece ay-yagmur simgesiyle gosterilir`() {
        val uiModel = mapper.toCityWeatherUiModel(
            cityWeather = cityWeather().copy(
                currentWeather = cityWeather().currentWeather.copy(condition = WeatherCondition.LIGHT_SHOWERS, isDay = false)
            )
        )
        val weather = uiModel.weather as CardWeather.Loaded
        assertEquals("🌙🌧️", weather.conditionEmoji)
    }
}
