package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import io.github.rojhatince47.weatherchan.core.common.result.AppResult
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.domain.repository.CityRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CachedWeathers
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CityWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.CurrentWeather
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Forecast
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.domain.repository.WeatherRepository
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCachedWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.domain.usecase.GetCityWeathersUseCase
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherUiMapper
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
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
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    @Test
    fun deletingCityViaRouteRestoresItAfterSnackbarUndo() {
        val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val fakeCities = RouteCityRepository(listOf(istanbul, City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))))
        val weather = RouteWeatherRepository()
        val unitRepository = RouteUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(weather),
            getCachedWeathers = GetCachedWeathersUseCase(weather),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = fakeCities
        )
        val settingsViewModel = SettingsViewModel(unitRepository, RouteThemeRepository(), RouteAppLanguageApplier())

        composeRule.setContent {
            HavaTheme {
                MainRoute(
                    onSelectCity = {},
                    onBack = {},
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
        assertTrue(fakeCities.current().none { it.name == "İstanbul" })

        composeRule.onNodeWithText("Geri al").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("İstanbul").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(fakeCities.current().any { it.name == "İstanbul" })
    }

    /**
     * Üst üste silmede ekranda yalnız **son** silmenin "Geri al" düğmesi olur ve
     * düğme yalnız o silmeyi geri alır. Kullanıcının seçtiği politika budur:
     * "Geri al" görünümü her zaman aynı olduğu için hangi silmeye ait olduğunu
     * ayırt etmek mümkün değildir; bu yüzden eski silme ekrandan kalkar ve
     * geri alınamaz olur.
     *
     * Bu test **politikayı** güvenceye alır (ekranda hangi silme var, hangi şehir
     * dönüyor). Yanlış şehri geri gelmeye karşı asıl kilit `MainViewModel`'de
     * ve birim testiyle kanıtlanmıştır
     * (`ust uste silmede eski geri al aksiyonu hicbir seyi geri almaz`).
     */
    @Test
    fun deletingTwoCitiesInARowOffersOnlyTheLatestUndo() {
        val istanbul = City("İstanbul", "Marmara", "Türkiye", Coordinates(41.01, 28.98))
        val ankara = City("Ankara", "İç Anadolu", "Türkiye", Coordinates(39.92, 32.85))
        val izmir = City("İzmir", "Ege", "Türkiye", Coordinates(38.42, 27.14))
        val fakeCities = RouteCityRepository(listOf(istanbul, ankara, izmir))
        val weather = RouteWeatherRepository()
        val unitRepository = RouteUnitRepository()
        val viewModel = MainViewModel(
            getCityWeathers = GetCityWeathersUseCase(weather),
            getCachedWeathers = GetCachedWeathersUseCase(weather),
            uiMapper = WeatherUiMapper(),
            unitRepository = unitRepository,
            cityRepository = fakeCities
        )
        val settingsViewModel = SettingsViewModel(unitRepository, RouteThemeRepository(), RouteAppLanguageApplier())

        composeRule.setContent {
            HavaTheme {
                MainRoute(
                    onSelectCity = {},
                    onBack = {},
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

        composeRule.onNodeWithText("Ankara").performTouchInput { longClick() }
        composeRule.onNodeWithText("Sil").performClick()
        // Geçen silmenin snackbar'ı **hemen** kalkmalı. Kuyruk kalsaydı ilk
        // snackbar kendiliğinden kapanana dek (~4 saniye) ekranda kalır ve o
        // boyunca yanlış şehri geri getiren bir "Geri al" düğmesi görünür.
        // Ölçüm: düzeltme olmadan ikinci snackbar 2 saniyede belirmiyor.
        composeRule.waitUntil(timeoutMillis = 2_000) {
            composeRule.onAllNodesWithText("İstanbul silindi").fetchSemanticsNodes().isEmpty()
        }

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ankara silindi").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(
            "Ekranda yalnizca son silmenin snackbar'i olmali",
            composeRule.onAllNodesWithText("İstanbul silindi").fetchSemanticsNodes().isEmpty()
        )

        composeRule.onNodeWithText("Geri al").performClick()

        composeRule.waitUntil(timeoutMillis = 5_000) {
            composeRule.onAllNodesWithText("Ankara").fetchSemanticsNodes().isNotEmpty() &&
                composeRule.onAllNodesWithText("İstanbul").fetchSemanticsNodes().isEmpty()
        }
        assertTrue("Yalnizca son silinen sehir geri alinmali", fakeCities.current().any { it.id == ankara.id })
        assertTrue("Onceki silinen sehir geri gelmemeli", fakeCities.current().none { it.id == istanbul.id })
    }

    /** Dil akışı bu testlerde kapsanmaz; seçim etkisiz. */
    private class RouteAppLanguageApplier : AppLanguageApplier {
        override val current: AppLanguage = AppLanguage.SYSTEM
        override fun apply(language: AppLanguage) {}
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
        override val cities: Flow<List<City>> = state.asStateFlow()

        /** Yalnız test doğrulaması için: depodaki güncel liste. */
        fun current(): List<City> = state.value

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

        override suspend fun restore(city: City, index: Int): List<City> {
            val current = state.value
            val updated = if (current.any { it.id == city.id }) {
                current
            } else {
                current.toMutableList().apply { add(index.coerceIn(0, current.size), city) }
            }
            state.value = updated
            return updated
        }

        override suspend fun addAsLocation(city: City): List<City> {
            val location = city.copy(isLocation = true)
            val updated = listOf(location) + state.value.filterNot { it.id == location.id }
            state.value = updated
            return updated
        }
    }

    private class RouteWeatherRepository : WeatherRepository {
        override fun cachedWeathers(): Flow<CachedWeathers?> = emptyFlow()

        override fun cachedLocationCity(): Flow<City?> = flowOf(null)

        override suspend fun saveLocationCity(city: City) {}

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
