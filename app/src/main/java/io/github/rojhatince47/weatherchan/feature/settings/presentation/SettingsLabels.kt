package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode

/** Tema ve dil etiketleri; kök özet ile alt sayfa aynı metni kullanır. */
@Composable
internal fun ThemeMode.label(): String = when (this) {
    ThemeMode.SYSTEM -> stringResource(R.string.theme_mode_system)
    ThemeMode.LIGHT -> stringResource(R.string.theme_mode_light)
    ThemeMode.DARK -> stringResource(R.string.theme_mode_dark)
}

@Composable
internal fun AppLanguage.label(): String = when (this) {
    AppLanguage.SYSTEM -> stringResource(R.string.language_system)
    AppLanguage.TURKISH -> stringResource(R.string.language_turkish)
    AppLanguage.ENGLISH -> stringResource(R.string.language_english)
}
