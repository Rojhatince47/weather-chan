package io.github.rojhatince47.weatherchan.feature.settings.testing

import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier

class FakeAppLanguageApplier(override val current: AppLanguage = AppLanguage.SYSTEM) : AppLanguageApplier {
    override fun apply(language: AppLanguage) = Unit
}
