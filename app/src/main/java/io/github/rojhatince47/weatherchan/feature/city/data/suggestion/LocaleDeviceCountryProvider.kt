package io.github.rojhatince47.weatherchan.feature.city.data.suggestion

import android.content.Context
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.DeviceCountryProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Cihazın yapılandırma dilinin ülke kodundan okur.
 *
 * Sistem `Locale`'i değil **uygulama yapılandırması** kullanılır: uygulama
 * dili sistem dilinden farklıysa hangisinin doğru olduğu tartışmalıdır, ama
 * yapılandırma her zaman cihazda etkin olan bağlamı verir.
 */
class LocaleDeviceCountryProvider @Inject constructor(
    @ApplicationContext private val context: Context
) : DeviceCountryProvider {
    override fun countryCode(): String? = context.resources.configuration.locales
        .takeIf { !it.isEmpty }
        ?.get(0)
        ?.country
        ?.takeIf { it.length == 2 }
        ?.uppercase()
}
