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
 * Konum geri ekleme satırı.
 *
 * Yalnız listenin **boş** olduğu durumda Ev'in boş ekranında görünür
 * ("Konumumu ekle" / "Tekrar dene"). Liste doluysa eylemin yeri Ayarlar
 * ekranının en üstündeki Konum bölümüdür; `Loading` göstergesi de oradadır.
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
