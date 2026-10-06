package io.github.rojhatince47.weatherchan.testing

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Cihaz testlerinin metin sabitleri Türkçe yazılmıştır; default kaynak
 * İngilizce olduğundan her test uygulamayı Türkçe'ye sabitler ve bitince
 * seçimi sıfırlar (sistem dili). API 33+ cihaz gerektirir (spec §9);
 * yerel emülatör (API 36+) karşılar.
 */
class ForceTurkishLocaleRule : TestRule {
    override fun apply(base: Statement, description: Description): Statement = object : Statement() {
        override fun evaluate() {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags("tr"))
            try {
                base.evaluate()
            } finally {
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
            }
        }
    }
}
