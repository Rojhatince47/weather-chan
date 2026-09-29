package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.SearchCitiesUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * MainRoute üzerinden uçtan uca akış: uzun basma → bottom sheet → Sil →
 * snackbar "Geri al" görünür → tıklayınca şehir geri gelir. MainScreen'in
 * aksine MainRoute event'leri topladığı için snackbar davranışı gerçek
 * ViewModel bağlantısıyla doğrulanır.
 */
class MainRouteTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun deletingCityViaRouteRestoresItAfterSnackbarUndo() {
        val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val fakeCities = RouteCityRepository(listOf(istanbul, City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))))
        val weather = RouteWeatherRepository()
        val unitRepository = RouteUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(weather),
            searchCities = SearchCitiesUseCase(weather),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = fakeCities
        )
        val settingsViewModel = SettingsViewModel(unitRepository, RouteThemeRepository())

        composeRule.setContent {
            HavaTheme {
                MainRoute(
                    onCityClick = {},
                    viewModel = viewModel,
                    settingsViewModel = settingsViewModel
                )
            }
        }

        composeRule.onNodeWithText("İstanbul").performTouchInput { longClick() }
        composeRule.onNodeWithText("Sil").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Geri al").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(fakeCities.cities.value.none { it.name == "İstanbul" })

        composeRule.onNodeWithText("Geri al").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("İstanbul").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(fakeCities.cities.value.any { it.name == "İstanbul" })
    }

    private class RouteUnitRepository : TemperatureUnitRepository {
        private val state = MutableStateFlow(TemperatureUnit.CELSIUS)
        override val unit: StateFlow<TemperatureUnit> = state.asStateFlow()

        override suspend fun setUnit(unit: TemperatureUnit) {
            state.value = unit
        }
    }

    private class RouteThemeRepository : ThemeModeRepository {
        private val state = MutableStateFlow(ThemeMode.SYSTEM)
        override val themeMode: StateFlow<ThemeMode> = state.asStateFlow()

        override suspend fun setThemeMode(themeMode: ThemeMode) {
            state.value = themeMode
        }
    }

    private class RouteCityRepository(
        initial: List<City>
    ) : CityRepository {
        private val state = MutableStateFlow(initial)
        override val cities: StateFlow<List<City>> = state.asStateFlow()

        override suspend fun loadCities(): List<City> = state.value

        override suspend fun add(city: City): List<City> {
            // İstenen davranış: listede yoksa ekle.
            val updated = if (state.value.any { it.id == city.id }) state.value else state.value + city
            state.value = updated
            return updated
        }

        override suspend fun remove(cityId: Long): List<City> {
            val updated = state.value.filterNot { it.id == cityId }
            state.value = updated
            return updated
        }
    }

    private class RouteWeatherRepository : WeatherRepository {
        override suspend fun cityWeathers(cities: List<City>): AppResult<List<CityWeather>> = AppResult.Success(
            cities.map { city ->
                CityWeather(
                    city = city,
                    currentWeather = CurrentWeather(
                        temperatureCelsius = 18 + (city.id % 7).toInt(),
                        condition = WeatherCondition.CLEAR,
                        windSpeedKmh = 8 + (city.id % 5).toInt(),
                        humidityPercent = 50 + (city.id % 20).toInt()
                    )
                )
            }
        )

        override suspend fun forecast(city: City): AppResult<Forecast> = AppResult.Success(
            Forecast(
                city = city,
                currentWeather = CurrentWeather(15, WeatherCondition.CLEAR, 5, 40),
                hourlyForecast = emptyList(),
                dailyForecast = emptyList()
            )
        )

        override suspend fun search(query: String): AppResult<List<City>> = AppResult.Success(emptyList())
    }
}
