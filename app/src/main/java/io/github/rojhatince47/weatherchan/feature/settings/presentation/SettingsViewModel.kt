package io.github.rojhatince47.weatherchan.feature.settings.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationSettings
import io.github.rojhatince47.weatherchan.feature.notifications.domain.model.NotificationType
import io.github.rojhatince47.weatherchan.feature.notifications.domain.repository.NotificationSettingsRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.AppLanguage
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.TemperatureUnit
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.AppLanguageApplier
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.SoundEffectsRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.TemperatureUnitRepository
import io.github.rojhatince47.weatherchan.feature.settings.domain.repository.ThemeModeRepository
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.TemperatureUnitUiModel
import io.github.rojhatince47.weatherchan.feature.settings.presentation.model.toUiModel
import java.time.LocalTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val repository: TemperatureUnitRepository,
    private val themeModeRepository: ThemeModeRepository,
    private val appLanguageApplier: AppLanguageApplier,
    private val soundEffectsRepository: SoundEffectsRepository,
    private val notificationSettingsRepository: NotificationSettingsRepository
) : ViewModel() {

    val uiState: StateFlow<UiState<TemperatureUnitUiModel>> = repository.unit
        .map<TemperatureUnit, UiState<TemperatureUnitUiModel>> { unit ->
            UiState.Success(unit.toUiModel())
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UiState.Success(repository.unit.value.toUiModel())
        )

    // Depo zaten bir StateFlow yayınlıyor; stateIn ile sarmalamak yalnızca
    // Eagerly başlatma ve initialValue kırılma riskini eklerdi. Doğrudan yayınla.
    val themeMode: StateFlow<ThemeMode> = themeModeRepository.themeMode

    // Yazma artık disk erişimi yaptığı için suspend; ViewModel kapsamında
    // başlatılır, çağıran (ekran) beklemez.
    fun onToggleUnit() {
        val next = when (repository.unit.value) {
            TemperatureUnit.CELSIUS -> TemperatureUnit.FAHRENHEIT
            TemperatureUnit.FAHRENHEIT -> TemperatureUnit.CELSIUS
        }
        viewModelScope.launch { repository.setUnit(next) }
    }

    fun onUnitSelected(unit: TemperatureUnit) {
        viewModelScope.launch { repository.setUnit(unit) }
    }

    fun onCycleThemeMode() {
        viewModelScope.launch {
            themeModeRepository.setThemeMode(themeModeRepository.themeMode.value.next())
        }
    }

    fun onThemeModeSelected(themeMode: ThemeMode) {
        viewModelScope.launch { themeModeRepository.setThemeMode(themeMode) }
    }

    // Dil tercihi AppCompat'te kalıcıdır; state yalnızca seçim görselliği
    // içindir ve ayarlar ekranına her girişte tazelenir (spec §5).
    private val _appLanguage = MutableStateFlow(appLanguageApplier.current)
    val appLanguage: StateFlow<AppLanguage> = _appLanguage.asStateFlow()

    fun refreshAppLanguage() {
        _appLanguage.value = appLanguageApplier.current
    }

    fun onAppLanguageSelected(language: AppLanguage) {
        appLanguageApplier.apply(language)
        _appLanguage.value = language
    }

    // --- ses efektleri ---

    val soundEffectsEnabled: StateFlow<Boolean> = soundEffectsRepository.enabled

    fun onSoundEffectsEnabledChanged(enabled: Boolean) {
        viewModelScope.launch { soundEffectsRepository.setEnabled(enabled) }
    }

    // --- bildirimler ---

    val notificationSettings: StateFlow<NotificationSettings> = notificationSettingsRepository.settings

    fun onNotificationMasterChanged(enabled: Boolean) {
        viewModelScope.launch { notificationSettingsRepository.setMasterEnabled(enabled) }
    }

    fun onNotificationTypeChanged(type: NotificationType, enabled: Boolean) {
        viewModelScope.launch { notificationSettingsRepository.setTypeEnabled(type, enabled) }
    }

    fun onNotificationSummaryTimeChanged(time: LocalTime) {
        viewModelScope.launch { notificationSettingsRepository.setSummaryTime(time) }
    }
}
