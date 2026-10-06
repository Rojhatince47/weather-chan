package io.github.rojhatince47.weatherchan.feature.weather.presentation.home

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.component.AddLocationRow
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.LocationState

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
    modifier: Modifier = Modifier
) {
    val openListText = stringResource(R.string.home_open_list)

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    HomeTitle(
                        cities = cities,
                        currentPage = currentPage,
                        onOpenList = { onSelectCity(null) },
                        openListText = openListText
                    )
                },
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
                // Konum kaydı yoksa geri ekleme satırı pager'ın üstünde
                // sabit durur: sayfalar arasında kaybolan bir eylem,
                // konumu yalnız boş listeye indirerek geri getirtirdi.
                // Görünürlük `locationState`'in kendisine bakılarak değil
                // veriyle karar verilir: konum sayfası ekrandaysa satırın
                // gösterileceği bir durum yoktur.
                val hasLocationPage = cities.any { it.isLocation }
                if (!hasLocationPage && locationState !is LocationState.Resolved) {
                    AddLocationRow(
                        locationState = locationState,
                        onAddLocation = onAddLocation,
                        onRetryLocation = onRetryLocation,
                        modifier = Modifier.padding(horizontal = HavaSpacing.lg)
                    )
                }
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

/**
 * Üst çubuk başlığı: o anki şehrin adı + sayfa noktaları.
 *
 * Başlığın tamamı tıklanabilir ve ince şehir listesini açar; nokta satırı
 * dekoratiftir (ekran okuyucudan gizlenir), tek sayfada çizilmez.
 */
@Composable
private fun HomeTitle(cities: List<City>, currentPage: Int, onOpenList: () -> Unit, openListText: String) {
    val safeIndex = currentPage.coerceIn(0, (cities.size - 1).coerceAtLeast(0))
    val current = cities.getOrNull(safeIndex)
    // Ad boş olabilir (konum çözülemediyse); presentation dile göre çözer.
    val title = current?.name.orEmpty().ifBlank { stringResource(R.string.location_default_name) }
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
                                color = if (selected) {
                                    MaterialTheme.colorScheme.primary
                                } else {
                                    MaterialTheme.colorScheme.outlineVariant
                                },
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
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page -> onPageChanged(page) }
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
