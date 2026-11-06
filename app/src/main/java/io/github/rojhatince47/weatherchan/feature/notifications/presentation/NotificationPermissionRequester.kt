package io.github.rojhatince47.weatherchan.feature.notifications.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
    // Yapılandırma değişiminde tekrar sormamak için oturumlar arası saklanır.
    var requested by rememberSaveable { mutableStateOf(false) }
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
            requested = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
