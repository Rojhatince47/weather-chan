package io.github.rojhatince47.weatherchan.feature.notifications.domain.repository

/** Bildirim izni/gösterim durumu. Testlerde sahtelenebilir bir port. */
fun interface NotificationPermissionChecker {
    fun canNotify(): Boolean
}
