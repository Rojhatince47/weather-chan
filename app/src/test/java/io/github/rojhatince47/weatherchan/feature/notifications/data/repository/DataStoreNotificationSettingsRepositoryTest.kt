package io.github.rojhatince47.weatherchan.feature.notifications.data.repository

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import io.github.rojhatince47.weatherchan.feature.notifications.data.local.NotificationPreferences
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import java.time.LocalTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class DataStoreNotificationSettingsRepositoryTest {

    @get:Rule
    val temporaryFolder = TemporaryFolder()

    private fun repository(): DataStoreNotificationSettingsRepository {
        val dispatcher = UnconfinedTestDispatcher()
        val dataStore = PreferenceDataStoreFactory.create(
            scope = CoroutineScope(dispatcher + Job()),
            produceFile = { temporaryFolder.newFile("notifications.preferences_pb") }
        )
        return DataStoreNotificationSettingsRepository(
            preferences = NotificationPreferences(dataStore),
            ioDispatcher = dispatcher
        )
    }

    @Test
    fun `varsayilanlar hepsi acik ve 08 00`() = runTest {
        val settings = repository().settings.first()

        assertTrue(settings.masterEnabled)
        NotificationType.entries.forEach { type ->
            assertTrue("$type acik olmali", settings.typeEnabled.getValue(type))
        }
        assertEquals(LocalTime.of(8, 0), settings.summaryTime)
    }

    @Test
    fun `yazilan ayarlar akisa yansir`() = runTest {
        val repository = repository()

        repository.setMasterEnabled(false)
        repository.setTypeEnabled(NotificationType.RAIN_SOON, false)
        repository.setSummaryTime(LocalTime.of(7, 30))

        val updated = repository.settings.first { !it.masterEnabled }
        assertFalse(updated.typeEnabled.getValue(NotificationType.RAIN_SOON))
        assertTrue(updated.typeEnabled.getValue(NotificationType.SEVERE_WEATHER))
        assertEquals(LocalTime.of(7, 30), updated.summaryTime)
    }
}
