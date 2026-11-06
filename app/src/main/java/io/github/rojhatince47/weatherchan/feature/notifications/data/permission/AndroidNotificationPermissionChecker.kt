package io.github.rojhatince47.weatherchan.feature.notifications.data.permission

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPermissionChecker
import javax.inject.Inject

/**
 * Android 13+ çalışma zamanı izni; öncesinde uygulama düzeyi bildirim
 * kapalı mı diye bakılır. İkisinde de tek cevap: gönderebilir miyim?
 */
class AndroidNotificationPermissionChecker @Inject constructor(
    @ApplicationContext private val context: Context
) : NotificationPermissionChecker {

    override fun canNotify(): Boolean = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        NotificationManagerCompat.from(context).areNotificationsEnabled()
    }
}
