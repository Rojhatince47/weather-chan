package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import io.github.rojhatince47.weatherchan.core.common.location.LocationProblem
import io.github.rojhatince47.weatherchan.core.common.location.locationProblemFor
import org.junit.Assert.assertEquals
import org.junit.Test

class LocationActionTest {

    @Test
    fun `izin varsa konum okunur`() {
        assertEquals(
            LocationAction.ReadLocation,
            locationActionFor(hasPermission = true, canShowRationale = false, servicesEnabled = true)
        )
    }

    @Test
    fun `izin var ama konum servisi kapaliyken konum ayarlari acilir`() {
        // Regresyon: eski karar "oku" derdi; okuma sessizce basarisiz olur,
        // kullanicinin gorur bir geri donusu olmazdi.
        assertEquals(
            LocationAction.OpenLocationSettings,
            locationActionFor(hasPermission = true, canShowRationale = false, servicesEnabled = false)
        )
    }

    @Test
    fun `izin yok ve diyalog gosterilebilirse izin istenir`() {
        assertEquals(
            LocationAction.RequestPermission,
            locationActionFor(hasPermission = false, canShowRationale = true, servicesEnabled = true)
        )
        // Konum servisi kapaliyken de once izin gerekir; diyalog gosterilir.
        assertEquals(
            LocationAction.RequestPermission,
            locationActionFor(hasPermission = false, canShowRationale = true, servicesEnabled = false)
        )
    }

    @Test
    fun `kalici redde uygulama ayarlari acilir`() {
        assertEquals(
            LocationAction.OpenAppSettings,
            locationActionFor(hasPermission = false, canShowRationale = false, servicesEnabled = true)
        )
        assertEquals(
            LocationAction.OpenAppSettings,
            locationActionFor(hasPermission = false, canShowRationale = false, servicesEnabled = false)
        )
    }

    @Test
    fun `konum problemi izin ve servis durumundan uretilir`() {
        assertEquals(
            LocationProblem.PERMISSION_DENIED,
            locationProblemFor(hasPermission = false, servicesEnabled = true)
        )
        assertEquals(
            LocationProblem.PERMISSION_DENIED,
            locationProblemFor(hasPermission = false, servicesEnabled = false)
        )
        assertEquals(
            LocationProblem.SERVICES_DISABLED,
            locationProblemFor(hasPermission = true, servicesEnabled = false)
        )
        assertEquals(
            LocationProblem.UNKNOWN,
            locationProblemFor(hasPermission = true, servicesEnabled = true)
        )
    }
}
