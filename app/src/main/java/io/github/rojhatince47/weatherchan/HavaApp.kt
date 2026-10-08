package io.github.rojhatince47.weatherchan

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.rememberNavController
import io.github.rojhatince47.weatherchan.core.navigation.HavaNavHost
import io.github.rojhatince47.weatherchan.feature.settings.presentation.SettingsViewModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.home.EvSayfasiViewModel

@Composable
fun HavaApp(
    modifier: Modifier = Modifier,
    // NavHost üstünde alınan ortak örnek: ev sayfası ve liste ekranları
    // sıcaklık birimi durumunu bu kaynaktan kullanır.
    settingsViewModel: SettingsViewModel = hiltViewModel(),
    // Konum + izin state machine'inin tek örneği: Ev ve Ayarlar paylaşır.
    evViewModel: EvSayfasiViewModel = hiltViewModel()
) {
    val navController = rememberNavController()

    Scaffold(modifier = modifier.fillMaxSize()) { innerPadding ->
        HavaNavHost(
            navController = navController,
            settingsViewModel = settingsViewModel,
            evViewModel = evViewModel,
            modifier = Modifier.padding(innerPadding)
        )
    }
}
