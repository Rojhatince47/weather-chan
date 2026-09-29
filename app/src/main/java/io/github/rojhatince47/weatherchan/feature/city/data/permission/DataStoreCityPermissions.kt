package io.github.rojhatince47.weatherchan.feature.city.data.permission

import io.github.rojhatince47.weatherchan.feature.city.data.local.CityPreferences
import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions
import javax.inject.Inject
import javax.inject.Singleton

/**
 * [CityPermissions] sözleşmesinin DataStore karşılığı.
 *
 * Uygulama data katmanında durur: domain katmanı DataStore'u bilmemelidir.
 */
@Singleton
class DataStoreCityPermissions @Inject constructor(
    private val preferences: CityPreferences
) : CityPermissions {
    override suspend fun wasAsked(): Boolean = preferences.isLocationPermissionAsked()

    override suspend fun markAsked() = preferences.setLocationPermissionAsked()
}
