package io.github.rojhatince47.weatherchan.core.ui.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.location.LocationProblem
import io.github.rojhatince47.weatherchan.core.common.location.hasLocationPermission
import io.github.rojhatince47.weatherchan.core.common.location.isLocationEnabled
import io.github.rojhatince47.weatherchan.core.common.location.locationProblemFor
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState

/**
 * Konum geri ekleme satırı.
 *
 * Boş listenin gösterildiği ekranlarda (Ana ekran, Şehirlerim) görünür:
 * "Konumumu ekle" / "Tekrar dene". Liste doluysa erişim yolu Ayarlar
 * ekranının en üstündeki Konum bölümüdür; `Loading` göstergesi de oradadır.
 *
 * Konum alınamadığında kullanıcıya **nedeni** de söylenir; sessizce
 * başarısız olan bir düğme olmaz. [locationProblem] testlerde sabitlenebilir;
 * routes gerçek durumu [rememberLocationProblem] ile verir.
 */
@Composable
fun AddLocationRow(
    locationState: LocationState,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    modifier: Modifier = Modifier,
    locationProblem: LocationProblem = LocationProblem.NONE
) {
    val retry = locationState is LocationState.Unavailable
    Column(modifier = modifier.fillMaxWidth()) {
        if (retry && locationProblem != LocationProblem.NONE) {
            Text(
                text = locationProblemLabel(locationProblem),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier
                    .padding(bottom = 4.dp)
                    .testTag("location_problem")
            )
        }
        Button(
            onClick = if (retry) onRetryLocation else onAddLocation,
            modifier = Modifier
                .fillMaxWidth()
                // Etiketli: "Tekrar dene" metni hem bu satırda hem de
                // havası alınamayan kartların kendi düğmesinde görünür; testler
                // satırı metinden değil etiketten ayırt etmeli.
                .testTag("add_location_row")
        ) {
            Text(
                text = if (retry) {
                    locationRetryLabel(locationProblem)
                } else {
                    stringResource(R.string.location_add_action)
                }
            )
        }
    }
}

/** Route'ların gerçek durumu satıra taşıması için hazır bakış. */
@Composable
fun rememberLocationProblem(): LocationProblem {
    val context = LocalContext.current
    return locationProblemFor(
        hasPermission = context.hasLocationPermission(),
        servicesEnabled = context.isLocationEnabled()
    )
}

@Composable
private fun locationProblemLabel(problem: LocationProblem): String = when (problem) {
    LocationProblem.PERMISSION_DENIED -> stringResource(R.string.location_problem_permission)
    LocationProblem.SERVICES_DISABLED -> stringResource(R.string.location_problem_services)
    LocationProblem.NONE,
    LocationProblem.UNKNOWN -> stringResource(R.string.location_problem_unknown)
}

@Composable
private fun locationRetryLabel(problem: LocationProblem): String = when (problem) {
    LocationProblem.PERMISSION_DENIED -> stringResource(R.string.location_action_settings)
    LocationProblem.SERVICES_DISABLED -> stringResource(R.string.location_action_location_settings)
    LocationProblem.NONE,
    LocationProblem.UNKNOWN -> stringResource(R.string.location_retry)
}
