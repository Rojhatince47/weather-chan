package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import android.content.res.Resources
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
fun MainRoute(
    onSelectCity: (Long) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    // NavHost üstünde alınan ortak örnek; giriş başına ayrı örnek oluşmaz.
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    LaunchedEffect(viewModel) {
        // Yalnız **en son** silmenin "Geri al" düğmesi ekranda olur. Snackbar
        // kuyruğu kullanılırsa eski düğme, kullanıcı ondan ne istediğini
        // karıştırarak basabilir ("Geri al" görünümü hep aynıdır) ve beklediği
        // şehri değil başka bir şehri geri getirir. Bu yüzden yeni silme
        // ekrandakini hemen kaldırır; üst üste silmede önceki silmeler geri
        // alınamaz olur, ama yanlış şehir hiç dönmez.
        var currentSnackbar: Job? = null
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.ShowSnackbar -> {
                    currentSnackbar?.cancel()
                    currentSnackbar = launch {
                        val result = snackbarHostState.showSnackbar(
                            message = event.message.resolve(resources),
                            actionLabel = event.actionLabel?.resolve(resources),
                            duration = SnackbarDuration.Short
                        )
                        // Aksiyon, snackbar action etiketine tıklanınca tetiklenir
                        // (geri al gibi anlık işlemler); otomatik kapanmada çalışmaz.
                        if (result == SnackbarResult.ActionPerformed) {
                            event.action?.invoke()
                        }
                    }
                }
            }
        }
    }

    MainScreen(
        uiState = uiState,
        themeMode = themeMode,
        onCityClick = onSelectCity,
        onBack = onBack,
        onDeleteCity = viewModel::onDeleteCity,
        onRetryCity = viewModel::onRetryCity,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

/** UiText'i suspend/launch bağlamında (LaunchedEffect) çözümlemek için. */
private fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> resources.getString(resId, *args)
}
