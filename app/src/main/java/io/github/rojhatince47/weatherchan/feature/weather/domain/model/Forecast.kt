package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class Forecast(
    val city: City,
    val currentWeather: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>
)
