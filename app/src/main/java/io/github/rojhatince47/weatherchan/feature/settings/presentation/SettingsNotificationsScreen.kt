package io.github.rojhatince47.weatherchan.feature.settings.presentation

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

/** Bildirim türleri, ana anahtar ve özet saati; Ayarlar'ın alt sayfası. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsNotificationsScreen(
    settings: NotificationSettings,
    permissionGranted: Boolean,
    onMasterChange: (Boolean) -> Unit,
    onTypeChange: (NotificationType, Boolean) -> Unit,
    onSummaryTimeChange: (LocalTime) -> Unit,
    onOpenNotificationSettings: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_notifications),
        onBack = onBack,
        modifier = modifier
    ) {
        if (!permissionGranted) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = HavaSpacing.xs),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.settings_notif_permission_missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.weight(1f)
                )
                TextButton(
                    onClick = onOpenNotificationSettings,
                    modifier = Modifier.testTag("settings_notif_permission")
                ) {
                    Text(stringResource(R.string.settings_notif_permission_action))
                }
            }
        }
        NotificationToggleRow(
            label = stringResource(R.string.settings_notif_master),
            checked = settings.masterEnabled,
            enabled = true,
            testTag = "settings_notif_master",
            onCheckedChange = onMasterChange
        )
        notificationTypes.forEach { (type, labelRes) ->
            NotificationToggleRow(
                label = stringResource(labelRes),
                checked = settings.typeEnabled.getValue(type),
                enabled = settings.masterEnabled,
                testTag = "settings_notif_${type.name.lowercase(Locale.ROOT)}",
                onCheckedChange = { onTypeChange(type, it) }
            )
        }
        SummaryTimeRow(
            time = settings.summaryTime,
            enabled = settings.masterEnabled,
            onChange = onSummaryTimeChange
        )
    }
}

@Composable
fun SettingsNotificationsRoute(onBack: () -> Unit, modifier: Modifier = Modifier, settingsViewModel: SettingsViewModel) {
    val settings by settingsViewModel.notificationSettings.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(isNotificationPermissionGranted(context))
    }
    // Sistem ayarlarından dönüşte durum tazelenir.
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        permissionGranted = isNotificationPermissionGranted(context)
    }

    SettingsNotificationsScreen(
        settings = settings,
        permissionGranted = permissionGranted,
        onMasterChange = settingsViewModel::onNotificationMasterChanged,
        onTypeChange = settingsViewModel::onNotificationTypeChanged,
        onSummaryTimeChange = settingsViewModel::onNotificationSummaryTimeChanged,
        onOpenNotificationSettings = {
            context.startActivity(
                Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                    .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            )
        },
        onBack = onBack,
        modifier = modifier
    )
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
            Text(time.format(summaryTimeFormat))
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

private fun isNotificationPermissionGranted(context: Context): Boolean = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
    ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
    ) == PackageManager.PERMISSION_GRANTED

private val summaryTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
