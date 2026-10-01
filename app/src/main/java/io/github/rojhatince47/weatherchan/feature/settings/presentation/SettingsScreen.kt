package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel

/**
 * Birim ve tema ayarları.
 *
 * Ana ekrandaki iki düğme yerine burada açık seçim: seçenek sayısı sabit ve
 * az olduğu için "döngüsel değiştir" gizli bir durum yaratıyordu (kullanıcı
 * hangi seçeneğe dokunacağını göremiyordu). Seçim anında uygulanır.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    unit: TemperatureUnitUiModel,
    themeMode: ThemeMode,
    onUnitSelected: (TemperatureUnit) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("action_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = HavaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
        ) {
            SectionTitle(stringResource(R.string.settings_unit_section))
            Row(horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)) {
                TemperatureUnit.entries.forEach { option ->
                    FilterChip(
                        selected = option == unit.unit,
                        onClick = { onUnitSelected(option) },
                        label = {
                            Text(
                                when (option) {
                                    TemperatureUnit.CELSIUS -> stringResource(R.string.unit_celsius)
                                    TemperatureUnit.FAHRENHEIT -> stringResource(R.string.unit_fahrenheit)
                                }
                            )
                        }
                    )
                }
            }
            SectionTitle(
                text = stringResource(R.string.settings_theme_section),
                modifier = Modifier.padding(top = HavaSpacing.md)
            )
            Column {
                ThemeMode.entries.forEach { option ->
                    FilterChip(
                        selected = option == themeMode,
                        onClick = { onThemeModeSelected(option) },
                        label = {
                            Text(
                                when (option) {
                                    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
                                    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
                                    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
                                }
                            )
                        },
                        modifier = Modifier.padding(vertical = HavaSpacing.xs)
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
            .padding(top = HavaSpacing.md)
            .semantics { heading() }
    )
}

@Composable
fun SettingsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, settingsViewModel: SettingsViewModel = hiltViewModel()) {
    val unitState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    // `UiState` yalnız yükleme sırasında farklı olabilir; ölümcül değil, bu
    // ekranda güvenli varsayılan gösterilir (MainRoute ile aynı desen).
    val unit = (unitState as? UiState.Success)?.data ?: TemperatureUnit.CELSIUS.toUiModel()
    SettingsScreen(
        unit = unit,
        themeMode = themeMode,
        onUnitSelected = settingsViewModel::onUnitSelected,
        onThemeModeSelected = settingsViewModel::onThemeModeSelected,
        onBack = onBack,
        modifier = modifier
    )
}
