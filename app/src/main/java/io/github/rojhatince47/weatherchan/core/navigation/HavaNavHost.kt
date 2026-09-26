package io.github.rojhatince47.weatherchan.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
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
                onCityClick = { city ->
                    navController.navigate(
                        ForecastDestination(
                            cityId = city.id,
                            name = city.name,
                            region = city.region,
                            country = city.country,
                            latitude = city.coordinates.latitude,
                            longitude = city.coordinates.longitude
                        )
                    )
                },
                settingsViewModel = settingsViewModel
            )
        }
        composable<ForecastDestination> {
            DetailRoute(
                onBack = { navController.popBackStack() },
                settingsViewModel = settingsViewModel
            )
        }
    }
}
