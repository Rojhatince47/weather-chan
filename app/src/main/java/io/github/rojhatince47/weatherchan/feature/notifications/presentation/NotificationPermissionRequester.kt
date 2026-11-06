package io.github.rojhatince47.weatherchan.feature.notifications.presentation

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/**
 * Android 13+ bildirim iznini ilk açılışta bir kez ister.
 *
 * İzin reddedilirse hiçbir şey olmaz: işçi bildirim göndermeden çıkar,
 * kullanıcı izni sistem ayarlarından sonradan verebilir.
 */
@Composable
fun NotificationPermissionRequester() {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return

    val context = LocalContext.current
    val preferences = remember {
        context.getSharedPreferences(PERMISSION_PREFERENCES, Context.MODE_PRIVATE)
    }
    // Kalıcı "bir kez soruldu" bilgisi: reddeden kullanıcı her açılışta
    // yeniden sorulmaz; izin sonradan sistem ayarlarından verilebilir.
    var requested by rememberSaveable {
        mutableStateOf(preferences.getBoolean(KEY_PERMISSION_ASKED, false))
    }
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
        onResult = { /* Sonuç işçi tarafında sorgulanır; burada tepki yok. */ }
    )

    LaunchedEffect(Unit) {
        val granted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
        if (!granted && !requested) {
            preferences.edit().putBoolean(KEY_PERMISSION_ASKED, true).apply()
            requested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}

private const val PERMISSION_PREFERENCES = "notifications_permission"
private const val KEY_PERMISSION_ASKED = "asked"
