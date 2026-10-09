package io.github.rojhatince47.weatherchan.feature.weather.presentation.model

import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import java.time.DayOfWeek
import java.time.LocalDate
import javax.inject.Inject

class WeatherUiMapper @Inject constructor() {

    fun toCityWeatherUiModel(cityWeather: CityWeather, unit: TemperatureUnit = TemperatureUnit.CELSIUS): CityWeatherUiModel =
        CityWeatherUiModel(
            cityId = cityWeather.city.id,
            name = displayTitle(cityWeather.city.name, cityWeather.city.region, cityWeather.city.isLocation),
            region = cityWeather.city.region,
            country = cityWeather.city.country,
            weather = CardWeather.Loaded(
                temperatureText = formatTemperature(cityWeather.currentWeather.temperatureCelsius, unit),
                condition = cityWeather.currentWeather.condition,
                conditionEmoji = emojiFor(cityWeather.currentWeather.condition, cityWeather.currentWeather.isDay),
                conditionLabel = labelFor(cityWeather.currentWeather.condition),
                isDay = cityWeather.currentWeather.isDay
            ),
            isLocation = cityWeather.city.isLocation
        )

    /**
     * Hava durumu alınamayan şehir için kart üretir. Kayıt listede kaldığı
     * sürece bu kart görünür; [CardWeather.Unavailable] dalı nedeni ve eylemi
     * çizer.
     */
    fun toUnavailable(city: City): CityWeatherUiModel = CityWeatherUiModel(
        cityId = city.id,
        name = displayTitle(city.name, city.region, city.isLocation),
        region = city.region,
        country = city.country,
        weather = CardWeather.Unavailable,
        isLocation = city.isLocation
    )
    fun toForecastDetailUiModel(
        forecast: Forecast,
        unit: TemperatureUnit = TemperatureUnit.CELSIUS,
        today: LocalDate = LocalDate.now()
    ): ForecastDetailUiModel = ForecastDetailUiModel(
        city = CityUiModel(
            id = forecast.city.id,
            name = displayTitle(forecast.city.name, forecast.city.region, forecast.city.isLocation),
            region = forecast.city.region,
            country = forecast.city.country,
            latitude = forecast.city.coordinates.latitude,
            longitude = forecast.city.coordinates.longitude
        ),
        currentWeather = CurrentWeatherUiModel(
            temperatureText = formatTemperature(forecast.currentWeather.temperatureCelsius, unit),
            conditionEmoji = emojiFor(forecast.currentWeather.condition, forecast.currentWeather.isDay),
            conditionLabel = labelFor(forecast.currentWeather.condition),
            windSpeedText = "${forecast.currentWeather.windSpeedKmh} km/s",
            humidityText = "%${forecast.currentWeather.humidityPercent}",
            condition = forecast.currentWeather.condition,
            isDay = forecast.currentWeather.isDay
        ),
        hourlyForecast = forecast.hourlyForecast.map { hourly ->
            HourlyForecastUiModel(
                timeText = formatHour(hourly.hourOfDay),
                temperatureText = formatTemperature(hourly.temperatureCelsius, unit),
                temperatureValue = temperatureValue(hourly.temperatureCelsius, unit),
                conditionEmoji = emojiFor(hourly.condition),
                precipitationProbabilityText = "%${hourly.precipitationProbabilityPercent}"
            )
        },
        dailyForecast = forecast.dailyForecast.map { daily ->
            DailyForecastUiModel(
                dayOffset = daily.dayOffset,
                dateText = if (daily.dayOffset == 0) {
                    UiText.Resource(R.string.forecast_today)
                } else {
                    UiText.Resource(dayNameResId(today.plusDays(daily.dayOffset.toLong()).dayOfWeek))
                },
                minTemperatureText = formatTemperature(daily.minTemperatureCelsius, unit),
                maxTemperatureText = formatTemperature(daily.maxTemperatureCelsius, unit),
                conditionEmoji = emojiFor(daily.condition),
                condition = daily.condition
            )
        }
    )

    private fun temperatureValue(celsius: Int, unit: TemperatureUnit): Int = when (unit) {
        TemperatureUnit.CELSIUS -> celsius
        TemperatureUnit.FAHRENHEIT -> celsius * 9 / 5 + 32
    }

    private fun formatTemperature(celsius: Int, unit: TemperatureUnit): String = when (unit) {
        TemperatureUnit.CELSIUS -> "${temperatureValue(celsius, unit)}°C"
        TemperatureUnit.FAHRENHEIT -> "${temperatureValue(celsius, unit)}°F"
    }

    private fun formatHour(hour: Int): String = "%02d:00".format(hour)

    private fun dayNameResId(dayOfWeek: DayOfWeek): Int = when (dayOfWeek) {
        DayOfWeek.MONDAY -> R.string.day_monday
        DayOfWeek.TUESDAY -> R.string.day_tuesday
        DayOfWeek.WEDNESDAY -> R.string.day_wednesday
        DayOfWeek.THURSDAY -> R.string.day_thursday
        DayOfWeek.FRIDAY -> R.string.day_friday
        DayOfWeek.SATURDAY -> R.string.day_saturday
        DayOfWeek.SUNDAY -> R.string.day_sunday
    }

    /**
     * Koşul + gün bilgisine göre simge. `isDay` default'u kapsam-dışı (detay:
     * tahmin/saatlik/günlük) çağrılar içindir; liste yolu gerçek değeri açıkça geçirir.
     */
    private fun emojiFor(condition: WeatherCondition, isDay: Boolean = true): String = when (condition) {
        WeatherCondition.CLEAR -> if (isDay) "☀️" else "🌙"
        WeatherCondition.PARTLY_CLOUDY -> if (isDay) "🌤️" else "🌙☁️"
        WeatherCondition.LIGHT_SHOWERS -> if (isDay) "🌦️" else "🌙🌧️"
        WeatherCondition.CLOUDY -> "☁️"
        WeatherCondition.RAIN -> "🌧️"
        WeatherCondition.SNOW -> "❄️"
        WeatherCondition.THUNDERSTORM -> "⛈️"
    }

    private fun labelFor(condition: WeatherCondition): UiText = when (condition) {
        WeatherCondition.CLEAR -> UiText.Resource(R.string.condition_clear)
        WeatherCondition.PARTLY_CLOUDY -> UiText.Resource(R.string.condition_partly_cloudy)
        WeatherCondition.LIGHT_SHOWERS -> UiText.Resource(R.string.condition_light_showers)
        WeatherCondition.CLOUDY -> UiText.Resource(R.string.condition_cloudy)
        WeatherCondition.RAIN -> UiText.Resource(R.string.condition_rain)
        WeatherCondition.SNOW -> UiText.Resource(R.string.condition_snow)
        WeatherCondition.THUNDERSTORM -> UiText.Resource(R.string.condition_thunderstorm)
    }
}
