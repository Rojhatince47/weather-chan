package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.component.AddLocationRow
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.EvSayfasiViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.rememberLocationAction
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import java.time.LocalTime
import java.time.format.DateTimeFormatter

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
    appLanguage: AppLanguage,
    locationState: LocationState,
    locationAdded: Boolean,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    onUnitSelected: (TemperatureUnit) -> Unit,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onOpenCityList: () -> Unit,
    onBack: () -> Unit,
    soundEffectsEnabled: Boolean = true,
    onSoundEffectsEnabledChange: (Boolean) -> Unit = {},
    notificationSettings: NotificationSettings = NotificationSettings(),
    onNotificationMasterChange: (Boolean) -> Unit = {},
    onNotificationTypeChange: (NotificationType, Boolean) -> Unit = { _, _ -> },
    onNotificationSummaryTimeChange: (LocalTime) -> Unit = {},
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
                .verticalScroll(rememberScrollState())
                .padding(horizontal = HavaSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
        ) {
            // İçeriğin en üstü; konum/kart durumundan bağımsız görünür. Sabit
            // (sticky) değildir: kaydırma ile en üste gelince görünür. Buton,
            // ana/liste akışındaki "Şehirlerim" dilinin Ayarlar karşılığıdır.
            Button(onClick = onOpenCityList) {
                Text(stringResource(R.string.settings_open_city_list))
            }
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
                        onRetryLocation = onRetryLocation
                    )
                }
            }
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
            SectionTitle(
                text = stringResource(R.string.settings_language_section),
                modifier = Modifier.padding(top = HavaSpacing.md)
            )
            Column {
                AppLanguage.entries.forEach { option ->
                    FilterChip(
                        selected = option == appLanguage,
                        onClick = { onAppLanguageSelected(option) },
                        label = {
                            Text(
                                when (option) {
                                    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
                                    AppLanguage.TURKISH -> stringResource(R.string.language_turkish)
                                    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
                                }
                            )
                        },
                        modifier = Modifier.padding(vertical = HavaSpacing.xs)
                    )
                }
            }
            SectionTitle(
                text = stringResource(R.string.settings_sound_section),
                modifier = Modifier.padding(top = HavaSpacing.md)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .toggleable(
                        value = soundEffectsEnabled,
                        onValueChange = onSoundEffectsEnabledChange,
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
                    checked = soundEffectsEnabled,
                    onCheckedChange = null
                )
            }
            SectionTitle(
                text = stringResource(R.string.settings_notifications),
                modifier = Modifier.padding(top = HavaSpacing.md)
            )
            NotificationToggleRow(
                label = stringResource(R.string.settings_notif_master),
                checked = notificationSettings.masterEnabled,
                enabled = true,
                testTag = "settings_notif_master",
                onCheckedChange = onNotificationMasterChange
            )
            notificationTypes.forEach { (type, labelRes) ->
                NotificationToggleRow(
                    label = stringResource(labelRes),
                    checked = notificationSettings.typeEnabled.getValue(type),
                    enabled = notificationSettings.masterEnabled,
                    testTag = "settings_notif_${type.name.lowercase()}",
                    onCheckedChange = { onNotificationTypeChange(type, it) }
                )
            }
            SummaryTimeRow(
                time = notificationSettings.summaryTime,
                enabled = notificationSettings.masterEnabled,
                onChange = onNotificationSummaryTimeChange
            )
        }
    }
}

/** Tür sırası arayüzdeki sıradır; ayarlar ekranı bu listeyi gezer. */
private val notificationTypes = listOf(
    NotificationType.SEVERE_WEATHER to R.string.settings_notif_severe,
    NotificationType.RAIN_SOON to R.string.settings_notif_rain,
    NotificationType.DAILY_SUMMARY to R.string.settings_notif_summary,
    NotificationType.COMPANION to R.string.settings_notif_companion
)

@Composable
private fun NotificationToggleRow(label: String, checked: Boolean, enabled: Boolean, testTag: String, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Switch,
                onValueChange = onCheckedChange
            )
            .testTag(testTag)
            .padding(vertical = HavaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        Switch(
            checked = checked,
            onCheckedChange = null,
            enabled = enabled
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SummaryTimeRow(time: LocalTime, enabled: Boolean, onChange: (LocalTime) -> Unit) {
    var showDialog by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = HavaSpacing.xs),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.settings_notif_summary_time),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = { showDialog = true },
            enabled = enabled,
            modifier = Modifier.testTag("settings_notif_summary_time")
        ) {
            Text(time.format(timeFormat))
        }
    }
    if (showDialog) {
        val pickerState = rememberTimePickerState(
            initialHour = time.hour,
            initialMinute = time.minute,
            is24Hour = true
        )
        AlertDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        onChange(LocalTime.of(pickerState.hour, pickerState.minute))
                        showDialog = false
                    }
                ) {
                    Text(stringResource(android.R.string.ok))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            },
            text = { TimePicker(state = pickerState) }
        )
    }
}

private val timeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

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
fun SettingsRoute(
    onBack: () -> Unit,
    onOpenCityList: () -> Unit,
    evViewModel: EvSayfasiViewModel,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = hiltViewModel()
) {
    val unitState by settingsViewModel.uiState.collectAsStateWithLifecycle()
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by settingsViewModel.appLanguage.collectAsStateWithLifecycle()
    val soundEffectsEnabled by settingsViewModel.soundEffectsEnabled.collectAsStateWithLifecycle()
    val notificationSettings by settingsViewModel.notificationSettings.collectAsStateWithLifecycle()
    val evUiState by evViewModel.uiState.collectAsStateWithLifecycle()
    // Ekran her açılışında seçim dış kaynakla (sistem ayarları) tazelenir.
    LaunchedEffect(Unit) { settingsViewModel.refreshAppLanguage() }
    // Konum eylemi Ev route'uyla aynı state machine'e bağlıdır (tek örnek).
    val onLocationAction = rememberLocationAction(
        permissionState = evUiState.permissionState,
        onPermissionResult = evViewModel::onPermissionResult,
        onReadLocation = evViewModel::refreshLocation
    )
    // `UiState` yalnız yükleme sırasında farklı olabilir; ölümcül değil, bu
    // ekranda güvenli varsayılan gösterilir (MainRoute ile aynı desen).
    val unit = (unitState as? UiState.Success)?.data ?: TemperatureUnit.CELSIUS.toUiModel()
    SettingsScreen(
        unit = unit,
        themeMode = themeMode,
        appLanguage = appLanguage,
        locationState = evUiState.locationState,
        locationAdded = evUiState.pages.any { it.isLocation },
        onAddLocation = onLocationAction,
        onRetryLocation = onLocationAction,
        onUnitSelected = settingsViewModel::onUnitSelected,
        onThemeModeSelected = settingsViewModel::onThemeModeSelected,
        onAppLanguageSelected = settingsViewModel::onAppLanguageSelected,
        onOpenCityList = onOpenCityList,
        onBack = onBack,
        soundEffectsEnabled = soundEffectsEnabled,
        onSoundEffectsEnabledChange = settingsViewModel::onSoundEffectsEnabledChanged,
        notificationSettings = notificationSettings,
        onNotificationMasterChange = settingsViewModel::onNotificationMasterChanged,
        onNotificationTypeChange = settingsViewModel::onNotificationTypeChanged,
        onNotificationSummaryTimeChange = settingsViewModel::onNotificationSummaryTimeChanged,
        modifier = modifier
    )
}
