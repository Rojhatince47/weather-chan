package io.github.rojhatince47.weatherchan.core.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsRoute
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity.AddCityRoute
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.EvSayfasiRoute
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.EvSayfasiViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.MainRoute

@Composable
fun HavaNavHost(
    navController: NavHostController,
    settingsViewModel: SettingsViewModel,
    evViewModel: EvSayfasiViewModel,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = EvSayfasiDestination,
        modifier = modifier
    ) {
        composable<EvSayfasiDestination> { entry ->
            EvSayfasiRoute(
                onAddCityClick = { navController.navigate(AddCityDestination) },
                onSettingsClick = { navController.navigate(SettingsDestination) },
                onOpenList = { navController.navigate(ListDestination) },
                viewModel = evViewModel,
                settingsViewModel = settingsViewModel,
                savedStateHandle = entry.savedStateHandle
            )
        }
        composable<ListDestination> {
            MainRoute(
                // Seçim, pager'a SavedStateHandle üzerinden taşınır ve liste
                // kapanır; pager kendisi ağ turunu pencereyle sınırlandırır.
                onSelectCity = { cityId ->
                    navController.previousBackStackEntry?.savedStateHandle?.set(SELECTED_CITY_ID, cityId)
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() },
                evViewModel = evViewModel,
                onAddCityClick = { navController.navigate(AddCityDestination) },
                settingsViewModel = settingsViewModel
            )
        }
        composable<AddCityDestination> {
            AddCityRoute(
                // Eklenen şehir detay ekranında değil doğrudan listeye açılır:
                // seçim kimliği Ev'in SavedStateHandle'ına yazılır; kullanıcı
                // önceki ekrana (Ana veya Şehirlerim) döner, eve dönüşte pager'a
                // aktarılır.
                onCityClick = { city ->
                    navController.getBackStackEntry(EvSayfasiDestination)
                        .savedStateHandle[SELECTED_CITY_ID] = city.id
                    navController.popBackStack()
                },
                onBack = { navController.popBackStack() }
            )
        }
        composable<SettingsDestination> {
            // Paylaşılan örnek: NavHost üstünde alınan SettingsViewModel
            // (EvSayfasiRoute ile aynı); giriş başına ayrı örnek oluşmaz ve
            // tema değişikliği anında tüm ekranlara yayılır.
            SettingsRoute(
                onBack = { navController.popBackStack() },
                // Şehirlerim'e geçiş; launchSingleTop çift dokunuşta ikinci
                // Liste yığını açtırmaz (AC3).
                onOpenCityList = {
                    navController.navigate(ListDestination) { launchSingleTop = true }
                },
                evViewModel = evViewModel,
                settingsViewModel = settingsViewModel
            )
        }
    }
}

/** Liste/ekleme ekranının pager'a taşıdığı seçili şehir kimliğinin anahtarı. */
internal const val SELECTED_CITY_ID = "selectCityId"
