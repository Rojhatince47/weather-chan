package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import java.time.format.DateTimeFormatter

/**
 * Ayarların kök sayfası: yalnız sık kullanılan kısayollar ve üç alt sayfa.
 *
 * Sıra kullanım sıklığına göredir: şehirler, birim ve ses her zaman görünür;
 * bildirimler ile tema/dil kendi sayfalarına taşınır.
 */
@Composable
fun SettingsScreen(
    unit: TemperatureUnitUiModel,
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    soundEffectsEnabled: Boolean,
    notificationSettings: NotificationSettings,
    onUnitSelected: (TemperatureUnit) -> Unit,
    onSoundEffectsEnabledChange: (Boolean) -> Unit,
    onOpenCities: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAppearance: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_title),
        onBack = onBack,
        modifier = modifier
    ) {
        SettingsNavRow(
            title = stringResource(R.string.settings_row_cities),
            subtitle = stringResource(R.string.settings_open_city_list),
            onClick = onOpenCities,
            testTag = "settings_row_cities"
        )
        HorizontalDivider()
        UnitRow(unit = unit, onUnitSelected = onUnitSelected)
        HorizontalDivider()
        SoundRow(
            enabled = soundEffectsEnabled,
            onEnabledChange = onSoundEffectsEnabledChange
        )
        HorizontalDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_notifications),
            subtitle = notificationSummary(notificationSettings),
            onClick = onOpenNotifications,
            testTag = "settings_row_notifications"
        )
        HorizontalDivider()
        SettingsNavRow(
            title = stringResource(R.string.settings_row_appearance),
            subtitle = themeMode.label() +
                stringResource(R.string.settings_summary_separator) +
                appLanguage.label(),
            onClick = onOpenAppearance,
            testTag = "settings_row_appearance"
        )
    }
}

@Composable
private fun UnitRow(unit: TemperatureUnitUiModel, onUnitSelected: (TemperatureUnit) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HavaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.settings_unit_section),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
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
                },
                modifier = Modifier.testTag(
                    when (option) {
                        TemperatureUnit.CELSIUS -> "settings_unit_celsius"
                        TemperatureUnit.FAHRENHEIT -> "settings_unit_fahrenheit"
                    }
                )
            )
        }
    }
}

@Composable
private fun SoundRow(enabled: Boolean, onEnabledChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = enabled,
                onValueChange = onEnabledChange,
                role = Role.Switch
            )
            .testTag("settings_sound_effects")
            .padding(vertical = HavaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
    ) {
        Text(
            text = stringResource(R.string.settings_sound_effects),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = enabled,
            onCheckedChange = null
        )
    }
}

@Composable
private fun notificationSummary(settings: NotificationSettings): String = if (!settings.masterEnabled) {
    stringResource(R.string.settings_notif_off)
} else {
    stringResource(
        R.string.settings_notif_on,
        settings.summaryTime.format(summaryTimeFormat)
    )
}

@Composable
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenCities: () -> Unit,
    onOpenNotifications: () -> Unit,
    onOpenAppearance: () -> Unit,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val unitState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by settingsViewModel.appLanguage.collectAsStateWithLifecycle()
    val soundEffectsEnabled by settingsViewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
    val notificationSettings by settingsViewModel.notificationSettings.collectAsStateWithLifecycle()

    // Ekran her açılışında seçim dış kaynakla (sistem ayarları) tazelenir.
    LaunchedEffect(Unit) { settingsViewModel.refreshAppLanguage() }

    val unit = (unitState as? UiState.Success)?.data ?: TemperatureUnit.CELSIUS.toUiModel()
    SettingsScreen(
        unit = unit,
        themeMode = themeMode,
        appLanguage = appLanguage,
        soundEffectsEnabled = soundEffectsEnabled,
        notificationSettings = notificationSettings,
        onUnitSelected = settingsViewModel::onUnitSelected,
        onSoundEffectsEnabledChange = settingsViewModel::onSoundEffectsEnabledChanged,
        onOpenCities = onOpenCities,
        onOpenNotifications = onOpenNotifications,
        onOpenAppearance = onOpenAppearance,
        onBack = onBack,
        modifier = modifier
    )
}

private val summaryTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
