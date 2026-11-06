package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode

/** Tema ve dil seçimleri; seyrek değiştiği için kendi alt sayfasında. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsAppearanceScreen(
    themeMode: ThemeMode,
    appLanguage: AppLanguage,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAppLanguageSelected: (AppLanguage) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsPageScaffold(
        title = stringResource(R.string.settings_row_appearance),
        onBack = onBack,
        modifier = modifier
    ) {
        SectionTitle(stringResource(R.string.settings_theme_section))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)) {
            ThemeMode.entries.forEach { option ->
                FilterChip(
                    selected = option == themeMode,
                    onClick = { onThemeModeSelected(option) },
                    label = { Text(option.label()) }
                )
            }
        }
        SectionTitle(stringResource(R.string.settings_language_section))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm)) {
            AppLanguage.entries.forEach { option ->
                FilterChip(
                    selected = option == appLanguage,
                    onClick = { onAppLanguageSelected(option) },
                    label = { Text(option.label()) }
                )
            }
        }
    }
}

@Composable
fun SettingsAppearanceRoute(onBack: () -> Unit, modifier: Modifier = Modifier, settingsViewModel: SettingsViewModel) {
    val themeMode by settingsViewModel.themeMode.collectAsStateWithLifecycle()
    val appLanguage by settingsViewModel.appLanguage.collectAsStateWithLifecycle()

    SettingsAppearanceScreen(
        themeMode = themeMode,
        appLanguage = appLanguage,
        onThemeModeSelected = settingsViewModel::onThemeModeSelected,
        onAppLanguageSelected = settingsViewModel::onAppLanguageSelected,
        onBack = onBack,
        modifier = modifier
    )
}
