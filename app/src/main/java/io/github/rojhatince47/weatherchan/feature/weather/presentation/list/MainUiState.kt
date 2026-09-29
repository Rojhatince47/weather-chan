package io.github.rojhatince47.weatherchan.feature.weather.presentation.list

import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityWeatherUiModel

data class MainUiState(
    val query: String = "",
    val content: UiState<List<CityWeatherUiModel>> = UiState.Loading,
    val isRefreshing: Boolean = false,
    val searchState: SearchUiState = SearchUiState.Idle,
    val permissionState: LocationPermissionState = LocationPermissionState.Restoring,
    val locationState: LocationState = LocationState.Idle,
    /** Şu an yeniden deneniyor: kart düğmesi kilitli, gösterge döner. */
    val retryingCityIds: Set<Long> = emptySet()
)

/**
 * Konum izninin **istemci tarafı** durumu.
 *
 * Bu tip yalnız diyaloğun gösterilip gösterilmediğini ve kullanıcının
 * cevabını temsil eder; gerçek sistem iznini temsil etmez. Gerçek izin her
 * okumada [android.location.LocationManager] üzerinden sınanır.
 */
sealed interface LocationPermissionState {
    /**
     * Kalıcı bayrak henüz okunmadı; diyaloğun gösterilip gösterilmeyeceği
     * **bilinmiyor**.
     *
     * Bu durum bilerek ayrı: bayrak DataStore'dan asenkron okunur ve ilk
     * Compose karesi bu okuma tamamlanmadan gelebilir. Başlangıçta `NotAsked`
     * görünürse UI diyaloğu ateşler ve kullanıcı izin vermeyi reddettikten
     * sonra bile her açılışta tekrar sorulur. Karar verilemeyen durumda
     * hiçbir şey tetiklenmez.
     */
    data object Restoring : LocationPermissionState

    /** Diyalog henüz gösterilmedi; UI bir kez başlatacak. */
    data object NotAsked : LocationPermissionState

    /** Sistem diyaloğu bir kez gösterildi, cevabı bilinmiyor. */
    data object Asked : LocationPermissionState

    /** Kullanıcı izin verdi. */
    data object Granted : LocationPermissionState

    /** Kullanıcı reddetti. */
    data object Denied : LocationPermissionState
}

/** Konumun okunma durumu. */
sealed interface LocationState {
    /** Henüz okunmadı. */
    data object Idle : LocationState

    /** Okunuyor. */
    data object Loading : LocationState

    /** Konum okundu ve listede. */
    data object Resolved : LocationState

    /**
     * Konum okunamadı: izin verilmemiş, GPS kapalı, sinyal yok ya da cihaz
     * konum servisini desteklemiyor.
     */
    data object Unavailable : LocationState
}
