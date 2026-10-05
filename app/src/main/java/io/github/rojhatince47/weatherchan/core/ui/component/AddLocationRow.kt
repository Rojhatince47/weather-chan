package io.github.rojhatince47.weatherchan.core.ui.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState

/**
 * Liste doluyken gösterilen konum geri ekleme satırı.
 *
 * Liste boşken eklenen eylem burada da görünür olmalıdır: aksi halde kullanıcı
 * konumu yalnız listeyi boşaltarak geri getirebilirdi. Ana ekranda pager'ın
 * üstünde, ince listede listenin sonunda sabit durur.
 */
@Composable
fun AddLocationRow(locationState: LocationState, onAddLocation: () -> Unit, onRetryLocation: () -> Unit, modifier: Modifier = Modifier) {
    val retry = locationState is LocationState.Unavailable
    Button(
        onClick = if (retry) onRetryLocation else onAddLocation,
        modifier = modifier
            .fillMaxWidth()
            // Etiketli: "Tekrar dene" metni hem bu satırda hem de
            // havası alınamayan kartların kendi düğmesinde görünür; testler
            // satırı metinden değil etiketten ayırt etmeli.
            .testTag("add_location_row")
    ) {
        Text(
            text = if (retry) {
                stringResource(R.string.location_retry)
            } else {
                stringResource(R.string.location_add_action)
            }
        )
    }
}
