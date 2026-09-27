package io.github.rojhatince47.weatherchan.core.ui.theme

/**
 * Animasyon süreleri (ms).
 *
 * F1'de tüketicisi yok; bu bilinçli bir zemin: hareketin kullanılacağı ilk yer
 * F2'deki mascot state machine geçişleri, sonraki yer F3'teki hava durumu kartı.
 * Kullanılana kadar değerlerini değiştirme.
 *
 * Kullanım kuralı: durum geçişleri MEDIUM, küçük geri bildirimler FAST. SLOW
 * yalnızca kullanıcıya "bir şey değişti" dedirmek istediğimizde.
 */
object HavaMotion {
    const val FAST = 150
    const val MEDIUM = 250
    const val SLOW = 400
}
