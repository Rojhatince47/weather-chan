package io.github.rojhatince47.weatherchan.feature.settings.data.repository

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier
import javax.inject.Inject
import javax.inject.Singleton

/** [AppLanguageApplier] üretim uygulaması; [AppCompatDelegate] sarmalayıcısı. */
@Singleton
class AppCompatAppLanguageApplier @Inject constructor() : AppLanguageApplier {

    override val current: AppLanguage
        get() {
            val locales = AppCompatDelegate.getApplicationLocales()
            val language = if (locales.isEmpty) null else locales[0]?.language
            return when (language) {
                "tr" -> AppLanguage.TURKISH
                "en" -> AppLanguage.ENGLISH
                else -> AppLanguage.SYSTEM
            }
        }

    override fun apply(language: AppLanguage) {
        val locales = when (language) {
            AppLanguage.SYSTEM -> LocaleListCompat.getEmptyLocaleList()
            AppLanguage.TURKISH -> LocaleListCompat.forLanguageTags("tr")
            AppLanguage.ENGLISH -> LocaleListCompat.forLanguageTags("en")
        }
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
