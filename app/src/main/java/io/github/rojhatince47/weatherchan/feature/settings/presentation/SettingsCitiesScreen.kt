package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.location.LocationProblem
import io.github.rojhatince47.weatherchan.core.ui.component.AddLocationRow
import io.github.rojhatince47.weatherchan.core.ui.component.rememberLocationProblem
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.EvSayfasiViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.rememberLocationAction
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState

/** Şehir listesi ve konum ekleme; Ayarlar'ın "Şehirler ve konum" alt sayfası. */
@Composable
fun SettingsCitiesScreen(
    locationState: LocationState,
    locationAdded: Boolean,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    onOpenCityList: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    locationProblem: LocationProblem = LocationProblem.NONE
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_row_cities),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsNavRow(
            title = stringResource(R.string.settings_open_city_list),
            subtitle = null,
            onClick = onOpenCityList,
            testTag = "settings_row_city_list"
        )
        if (!locationAdded && locationState !is LocationState.Resolved) {
            SectionTitle(stringResource(R.string.settings_location_section))
            when (locationState) {
                LocationState.Loading -> Text(
                    text = stringResource(R.string.location_loading),
                    modifier = Modifier.testTag("location_loading")
                )
                else -> AddLocationRow(
                    locationState = locationState,
                    onAddLocation = onAddLocation,
                    onRetryLocation = onRetryLocation,
                    locationProblem = locationProblem
                )
            }
        }
    }
}

@Composable
fun SettingsCitiesRoute(onBack: () -> Unit, onOpenCityList: () -> Unit, evViewModel: EvSayfasiViewModel, modifier: Modifier = Modifier) {
    val uiState by evViewModel.uiState.collectAsStateWithLifecycle()
    val onLocationAction = rememberLocationAction(
        permissionState = uiState.permissionState,
        onPermissionResult = evViewModel::onPermissionResult,
        onReadLocation = evViewModel::refreshLocation
    )

    SettingsCitiesScreen(
        locationState = uiState.locationState,
        locationAdded = uiState.pages.any { it.isLocation },
        onAddLocation = onLocationAction,
        onRetryLocation = onLocationAction,
        onOpenCityList = onOpenCityList,
        onBack = onBack,
        modifier = modifier,
        locationProblem = rememberLocationProblem()
    )
}
