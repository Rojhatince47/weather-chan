package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import org.junit.Assert.assertEquals
import org.junit.Test

class LocationActionTest {

    /**
     * Regresyon (cihazda gözlendi): izin reddedildikten sonra "Tekrar dene"
     * hiçbir şey yapmıyordu. Düğme yalnız konumu yeniden okuyordu; izin yokken
     * bu okuma tanımı gereği başarısız olur (AndroidLocationProvider izni
     * yoksa null döner) ve izin diyaloğu bir daha gösterilmezdi — ilk soruşta
     * kalıcı bayrak yazılır ve durum `NotAsked`'a bir daha dönmez.
     */
    @Test
    fun `izin varsa konum yeniden okunur`() {
        assertEquals(
            LocationAction.ReadLocation,
            locationActionFor(hasPermission = true, canShowRationale = false)
        )
        assertEquals(
            LocationAction.ReadLocation,
            locationActionFor(hasPermission = true, canShowRationale = true)
        )
    }

    /**
     * Kullanıcı izni bir kez reddetti ama sistem henüz kalıcı olarak
     * kapatmadı: diyalog tekrar gösterilebilir. Bu, bildirilen hatanın ana
     * senaryosudur — "Tekrar dene" burada izin diyaloğunu yeniden açmalı.
     */
    @Test
    fun `izin yok ve diyalog hala gosterilebilirse izin yeniden istenir`() {
        assertEquals(
            LocationAction.RequestPermission,
            locationActionFor(hasPermission = false, canShowRationale = true)
        )
    }

    /**
     * Kalıcı redd: sistem diyaloğu artık gösterilmez (ikinci redd ya da
     * "bir daha sorma"). `requestPermissions` sessizce anında reddedilir;
     * düğmeye basmak yine hiçbir şey yapmaz. Tek yol uygulama ayarlarıdır.
     */
    @Test
    fun `izin yok ve diyalog kapaliysa uygulama ayarlari acilir`() {
        assertEquals(
            LocationAction.OpenAppSettings,
            locationActionFor(hasPermission = false, canShowRationale = false)
        )
    }
}
