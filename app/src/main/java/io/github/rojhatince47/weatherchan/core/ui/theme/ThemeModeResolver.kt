package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode

/**
 * Kullanıcının seçtiği tema ile sistem temasını birleştirir.
 * Tema tercihi SYSTEM dışındaysa sistem değeri yok sayılır.
 */
@Composable
fun ThemeMode.resolveIsDark(): Boolean = when (this) {
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
    ThemeMode.SYSTEM -> isSystemInDarkTheme()
}
