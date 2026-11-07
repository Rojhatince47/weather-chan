package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

import android.animation.ValueAnimator
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.common.location.LocationProblem
import io.github.rojhatince47.weatherchan.core.ui.component.AddLocationRow
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.core.ui.theme.WeatherBackgroundPalette
import io.github.rojhatince47.weatherchan.core.ui.theme.resolveIsDark
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.components.WeatherBackground
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.displayTitle
import kotlinx.coroutines.flow.drop

/**
 * Kaydırmalı ev sayfası: sabit üst çubuk + yatay pager.
 *
 * `onSelectCity(null)` → liste açma isteğidir; sayı taşıması ters yönde
 * çalışır (liste seçimi pager'a index olarak gelir — Task 7). İsimlendirme
 * tarihseldir (`onSelectCity`), davranışı: null = listeyi aç.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EvSayfasiScreen(
    cities: List<City>,
    currentPage: Int,
    contentFor: @Composable (City) -> Unit,
    backgroundPaletteFor: (City) -> WeatherBackgroundPalette? = { null },
    themeMode: ThemeMode,
    onPageChanged: (Int) -> Unit,
    onAddCityClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onSelectCity: (selectedCityId: Long?) -> Unit,
    onRetry: (Long) -> Unit,
    onRefresh: (Long) -> Unit,
    locationState: LocationState,
    onAddLocation: () -> Unit,
    onRetryLocation: () -> Unit,
    locationProblem: LocationProblem = LocationProblem.NONE,
    modifier: Modifier = Modifier
) {
    val openListText = stringResource(R.string.home_open_list)
    val isDark = themeMode.resolveIsDark()
    // Aktif sayfanın paleti; veri yok/boş liste → nötr (K10).
    val backgroundPalette = cities.getOrNull(currentPage)?.let { backgroundPaletteFor(it) }
        ?: WeatherBackgroundPalette.neutral(isDark)

    Box(modifier = modifier.fillMaxSize()) {
        WeatherBackground(
            palette = backgroundPalette,
            animatorsEnabled = ValueAnimator.areAnimatorsEnabled()
        )
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        HomeTitle(
                            cities = cities,
                            currentPage = currentPage,
                            onOpenList = { onSelectCity(null) },
                            openListText = openListText,
                            indicatorActive = backgroundPalette.indicatorActive,
                            indicatorInactive = backgroundPalette.indicatorInactive
                        )
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        scrolledContainerColor = Color.Transparent,
                        titleContentColor = backgroundPalette.contentColor,
                        actionIconContentColor = backgroundPalette.contentColor
                    ),
                    actions = {
                        IconButton(
                            onClick = onAddCityClick,
                            modifier = Modifier.testTag("action_add_city")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Add,
                                contentDescription = stringResource(R.string.action_open_add_city)
                            )
                        }
                        IconButton(
                            onClick = onSettingsClick,
                            modifier = Modifier.testTag("action_settings")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Settings,
                                contentDescription = stringResource(R.string.action_open_settings)
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            if (cities.isEmpty()) {
                // Hiç sayfa yoksa pager anlamsızdır; kullanıcıyı ilk şehre
                // yönlendiren sade bir boş durum gösterilir.
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(text = stringResource(R.string.home_empty_state))
                    // Konum akışı boş listede de görünür: kullanıcı izni verdikten
                    // sonra ne olduğunu görmeli, başarısızlıkta tekrar deneyebilmeli.
                    when {
                        locationState is LocationState.Loading -> Text(
                            text = stringResource(R.string.location_loading),
                            modifier = Modifier
                                .padding(top = HavaSpacing.md)
                                .testTag("location_loading")
                        )
                        locationState !is LocationState.Resolved -> AddLocationRow(
                            locationState = locationState,
                            onAddLocation = onAddLocation,
                            onRetryLocation = onRetryLocation,
                            locationProblem = locationProblem,
                            modifier = Modifier
                                .padding(top = HavaSpacing.md)
                                .padding(horizontal = HavaSpacing.lg)
                        )
                    }
                    Button(
                        onClick = onAddCityClick,
                        modifier = Modifier.padding(top = HavaSpacing.md)
                    ) {
                        Text(text = stringResource(R.string.action_open_add_city))
                    }
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    HomePager(
                        cities = cities,
                        currentPage = currentPage,
                        onPageChanged = onPageChanged,
                        contentFor = contentFor,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

/**
 * Üst çubuk başlığı: o anki şehrin adı + sayfa noktaları.
 *
 * Başlığın tamamı tıklanabilir ve ince şehir listesini açar; nokta satırı
 * dekoratiftir (ekran okuyucudan gizlenir), tek sayfada çizilmez.
 */
@Composable
private fun HomeTitle(
    cities: List<City>,
    currentPage: Int,
    onOpenList: () -> Unit,
    openListText: String,
    indicatorActive: Color,
    indicatorInactive: Color
) {
    val safeIndex = currentPage.coerceIn(0, (cities.size - 1).coerceAtLeast(0))
    val current = cities.getOrNull(safeIndex)
    // Ad boş olabilir (konum çözülemediyse); presentation dile göre çözer.
    val title = displayTitle(current?.name, current?.region, current?.isLocation ?: false)
        .ifBlank { stringResource(R.string.location_default_name) }
    // contentDescription birleştirilerek duyurulur: yalnız "listeyi aç"
    // deseği mevcut şehrin adını maskelerdi.
    val titleDescription = if (current != null) "$title, $openListText" else openListText
    Column(
        modifier = Modifier
            .testTag("action_open_list")
            .semantics { contentDescription = titleDescription }
            .clickable(role = Role.Button) { onOpenList() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (current?.isLocation == true) {
                Text(text = "📍")
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.semantics { heading() }
            )
        }
        if (cities.size > 1) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(HavaSpacing.sm, Alignment.CenterHorizontally),
                modifier = Modifier.clearAndSetSemantics { }
            ) {
                repeat(cities.size) { index ->
                    val selected = index == safeIndex
                    Box(
                        modifier = Modifier
                            .size(if (selected) HavaSpacing.sm else HavaSpacing.xs)
                            .background(
                                color = if (selected) indicatorActive else indicatorInactive,
                                shape = CircleShape
                            )
                    )
                }
            }
        }
    }
}

/**
 * Yatay sayfa kaydırıcısı.
 *
 * Dışarıdan gelen `currentPage` değişimi (liste dönüşü, konum ekleme) sayfaya
 * kaydırır; kullanıcının kaydırması `onPageChanged` ile ViewModel'e bildirilir.
 */
@Composable
private fun HomePager(
    cities: List<City>,
    currentPage: Int,
    onPageChanged: (Int) -> Unit,
    contentFor: @Composable (City) -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(initialPage = currentPage) { cities.size }
    LaunchedEffect(currentPage) {
        if (pagerState.currentPage != currentPage && !pagerState.isScrollInProgress) {
            pagerState.animateScrollToPage(currentPage)
        }
    }
    // Kararlı (settled) sayfa yayınlanır: `currentPage` kaydırmanın ortasında
    // ara değerler alır ve programatik atlama animasyonunu iptal ederdi.
    // İlk değer (drop(1)) görünümün kendi eski sayfasının yankısıdır: yayımlanırsa
    // liste dönüşü seçimini ezar (regresyon kilidi: listeSecimiGecSayfaYankisiylaEzilmez).
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.drop(1).collect { page -> onPageChanged(page) }
    }
    HorizontalPager(
        state = pagerState,
        modifier = modifier.testTag("home_pager")
    ) { index ->
        Box(modifier = Modifier.fillMaxSize()) {
            contentFor(cities[index])
        }
    }
}
