package io.github.rojhatince47.weatherchan.testing

import android.content.Context
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatActivity
import java.util.Locale

/**
 * Cihaz testlerinin host etkinliği.
 *
 * Kaynaklar `attachBaseContext`te deterministik biçimde Türkçe'ye sabitlenir:
 * `AppCompatDelegate.setApplicationLocales` ComponentActivity tabanlı
 * host'larda yeniden oluşturma tetiklemediğinden (spec §9/Y2) bu yol
 * seçildi. İngilizce gibi başka bir dil sınanacaksa [overrideLocaleTag]
 * değiştirilip etkinlik `recreate()` ile yeniden kurulur.
 */
class HavaTestActivity : AppCompatActivity() {

    companion object {
        /** Testler gerektiğinde değiştirir; `null` = varsayılan Türkçe. */
        @Volatile
        var overrideLocaleTag: String? = null
    }

    override fun attachBaseContext(newBase: Context) {
        val tag = overrideLocaleTag ?: "tr"
        val config = Configuration(newBase.resources.configuration)
        config.setLocale(Locale.forLanguageTag(tag))
        super.attachBaseContext(newBase.createConfigurationContext(config))
    }
}
