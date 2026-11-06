package io.github.rojhatince47.weatherchan.feature.notifications.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.github.rojhatince47.weatherchan.feature.notifications.data.NotificationPoster
import io.github.rojhatince47.weatherchan.feature.notifications.data.local.DataStoreNotificationHistoryStore
import io.github.rojhatince47.weatherchan.feature.notifications.data.permission.AndroidNotificationPermissionChecker
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationHistoryStore
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPermissionChecker
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationPresenter
import io.github.rojhatince47.weatherchan.feature.notifications.worker.NotificationScheduler
import io.github.rojhatince47.weatherchan.feature.notifications.worker.WorkNotificationScheduler

@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationsModule {

    @Binds
    abstract fun bindNotificationHistoryStore(impl: DataStoreNotificationHistoryStore): NotificationHistoryStore

    @Binds
    abstract fun bindNotificationPresenter(impl: NotificationPoster): NotificationPresenter

    @Binds
    abstract fun bindNotificationPermissionChecker(impl: AndroidNotificationPermissionChecker): NotificationPermissionChecker

    @Binds
    abstract fun bindNotificationScheduler(impl: WorkNotificationScheduler): NotificationScheduler
}
