package io.github.rojhatince47.weatherchan.feature.weather.domain.model

data class Forecast(
    val city: City,
    val currentWeather: CurrentWeather,
    val hourlyForecast: List<HourlyForecast>,
    val dailyForecast: List<DailyForecast>,
    /**
     * Şehrin UTC farkı (saniye). Saatlik/günlük damgalar bu dilimde yereldir;
     * cihazın saat diliminden farklı olabilir.
     */
    val utcOffsetSeconds: Int = 0
)
