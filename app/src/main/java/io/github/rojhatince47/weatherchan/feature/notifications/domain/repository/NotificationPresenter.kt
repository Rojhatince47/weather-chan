package io.github.rojhatince47.weatherchan.feature.notifications.domain.repository

import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.PlannedNotification
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

/** Planlanan bildirimi kullanıcıya ulaştıran port; Android tarafı poster. */
interface NotificationPresenter {
    fun post(notification: PlannedNotification, city: City)
}
