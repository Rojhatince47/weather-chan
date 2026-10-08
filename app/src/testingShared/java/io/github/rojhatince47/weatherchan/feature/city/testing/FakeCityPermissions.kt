package io.github.rojhatince47.weatherchan.feature.city.testing

import io.github.rojhatince47.weatherchan.feature.city.domain.permission.CityPermissions

/** Kalıcı "diyalog gösterildi" bayrağının sahtesi. */
class FakeCityPermissions(private var asked: Boolean = false) : CityPermissions {
    override suspend fun wasAsked(): Boolean = asked

    override suspend fun markAsked() {
        asked = true
    }
}
