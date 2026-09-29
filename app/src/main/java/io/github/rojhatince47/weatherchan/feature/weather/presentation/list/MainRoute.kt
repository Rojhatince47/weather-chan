package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import android.Manifest
import android.content.res.Resources
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

@Composable
fun MainRoute(
    onCityClick: (City) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MainViewModel = hiltViewModel(),
    // NavHost üstünde alınan ortak örnek; giriş başına ayrı örnek oluşmaz.
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settingsState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val unit = (settingsState as? UiState.Success)?.data
        ?: TemperatureUnit.CELSIUS.toUiModel()
    val snackbarHostState = remember { SnackbarHostState() }
    val resources = LocalResources.current

    // Sistem izin diyaloğu. Cevap ViewModel'e bırakılır; kalıcı bayrak orada
    // yazılır ve bu launcher bir daha tetiklenmez.
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { result ->
        // Kaba konum izni tek başına da yeterlidir; kullanıcı "yaklaşık"
        // seçtiyse akış yine çalışır.
        val granted = result[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            result[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        viewModel.onPermissionResult(granted = granted)
    }

    // Diyaloğu yalnız **ilk** açılışta göster. `askedKey` bu composable'ın
    // ömrü boyunca "gösterildi" işaretini tutar; kalıcı karar ViewModel'deki
    // bayraktadır. Etkinlik yeniden oluşsa bile diyalog ikinci kez açılmaz.
    var askedKey by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(uiState.permissionState) {
        if (!askedKey && uiState.permissionState == LocationPermissionState.NotAsked) {
            askedKey = true
            permissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        }
    }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is MainEvent.ShowSnackbar -> {
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

    MainScreen(
        uiState = uiState,
        unit = unit,
        themeMode = themeMode,
        onQueryChange = viewModel::onQueryChange,
        onCityClick = { cityId -> viewModel.findCity(cityId)?.let(onCityClick) },
        onAddCity = viewModel::onAddCity,
        onOpenCity = onCityClick,
        onDeleteCity = viewModel::onDeleteCity,
        onRetryCity = viewModel::onRetryCity,
        onRetry = viewModel::onRetry,
        onRefresh = viewModel::onRefresh,
        onToggleUnit = settingsViewModel::onToggleUnit,
        onToggleThemeMode = settingsViewModel::onCycleThemeMode,
        onAddLocation = viewModel::onAddLocation,
        onRetryLocation = viewModel::refreshLocation,
        modifier = modifier,
        snackbarHostState = snackbarHostState
    )
}

/** UiText'i suspend/launch bağlamında (LaunchedEffect) çözümlemek için. */
private fun UiText.resolve(resources: Resources): String = when (this) {
    is UiText.Dynamic -> value
    is UiText.Resource -> resources.getString(resId, *args)
}
