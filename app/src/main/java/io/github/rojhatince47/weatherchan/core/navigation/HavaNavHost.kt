package io.github.rojhatince47.weatherchan.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsRoute
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity.AddCityRoute
import io.github.rojhatince47.weatherchan.feature.weather.presentation.detail.DetailRoute
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.MainRoute

@Composable
fun HavaNavHost(navController: NavHostController, settingsViewModel: SettingsViewModel, modifier: Modifier = Modifier) {
    NavHost(
        navController = navController,
        startDestination = ListDestination,
        modifier = modifier
    ) {
        composable<ListDestination> {
            MainRoute(
                onCityClick = { city -> navController.navigate(city.toForecastDestination()) },
                onAddCityClick = { navController.navigate(AddCityDestination) },
                onSettingsClick = { navController.navigate(SettingsDestination) },
                settingsViewModel = settingsViewModel
            )
        }
        composable<ForecastDestination> {
            DetailRoute(
                onBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel
            )
        }
        composable<AddCityDestination> {
            AddCityRoute(
                onCityClick = { city -> navController.navigate(city.toForecastDestination()) },
                onBack = { navController.popBackStack() }
            )
        }
        composable<SettingsDestination> {
            // Paylaşılan örnek: NavHost üstünde alınan SettingsViewModel
            // (MainRoute/DetailRoute ile aynı); giriş başına ayrı örnek
            // oluşmaz ve tema değişikliği anında tüm ekranlara yayılır.
            SettingsRoute(
                onBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/** Detay ekranının destination bağımsız değişkenlerini tek yerde kurar. */
private fun City.toForecastDestination(): ForecastDestination = ForecastDestination(
    cityId = id,
    name = name,
    region = region,
    country = country,
    latitude = coordinates.latitude,
    longitude = coordinates.longitude
)
