package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.presentation.list.CityWeatherCardContent

/**
 * Şehir ekleme ekranı: arama kutusu + konum/ülke/dünya önerileri.
 *
 * Sorgu boşken öneri bölümü, doluyken arama sonuçları gösterilir — ikisi aynı
 * anda değil, çünkü ikisi de "nereye gitmek istiyorsun" sorusunun farklı
 * cevaplarıdır ve birlikte ekranı kirlidir.
 *
 * Satırlar [CityWeatherCardContent] ile durum renkli kartlarda gösterilir.
 * Büyüteç (detay aç) kaldırılmıştır; satıra dokunma arama sonuçlarında
 * detaya gider, önerilerde ekleme yapar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCityScreen(
    uiState: AddCityUiState,
    onQueryChange: (String) -> Unit,
    onAddCity: (City) -> Unit,
    onOpenCity: (City) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Kart koşul paleti seçimi de aynı tema kararına bağlı olmalı.
    val isDarkTheme = MaterialTheme.colorScheme.surface.luminance() < 0.5f

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.add_city_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("action_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.action_back)
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            CitySearchField(value = uiState.query, onValueChange = onQueryChange)

            val search = uiState.searchState
            when (search) {
                SearchUiState.Loading -> LinearProgressIndicator(
                    modifier = Modifier.fillMaxWidth().testTag("search_indicator")
                )
                is SearchUiState.Failure -> Text(
                    text = search.error.toUiText().asString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(
                        horizontal = HavaSpacing.lg,
                        vertical = HavaSpacing.sm
                    )
                )
                else -> Unit
            }

            if (uiState.query.isBlank()) {
                uiState.suggestions?.let { lists ->
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = HavaSpacing.lg)
                    ) {
                        val seen = mutableSetOf<Long>()
                        SuggestionBucket(R.string.suggestion_nearby, lists.nearby, seen, onAddCity, isDarkTheme)
                        SuggestionBucket(R.string.suggestion_country, lists.country, seen, onAddCity, isDarkTheme)
                        SuggestionBucket(R.string.suggestion_world, lists.world, seen, onAddCity, isDarkTheme)
                    }
                }
            } else if (search is SearchUiState.Success && search.results.isEmpty()) {
                Text(
                    text = stringResource(R.string.state_empty_search),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(
                        horizontal = HavaSpacing.lg,
                        vertical = HavaSpacing.xl
                    )
                )
            } else if (search is SearchUiState.Success) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = HavaSpacing.lg, vertical = HavaSpacing.sm),
                    verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)
                ) {
                    items(search.results.distinctBy { it.city.id }, key = { it.city.id }) { result ->
                        CityWeatherCardContent(
                            name = result.city.name,
                            region = result.city.region,
                            country = result.city.country,
                            isLocation = result.city.isLocation,
                            cardWeather = result.cardWeather,
                            isDarkTheme = isDarkTheme,
                            isLoading = result.cardWeather == null,
                            onClick = {
                                onAddCity(result.city)
                                onOpenCity(result.city)
                            },
                            onAddClick = { onAddCity(result.city) },
                            isInList = result.isInList,
                            enabled = true
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tek öneri grubunun başlığı ve satırları.
 *
 * Başlık dalı anlatır ("Yakınında" / "Ülkende" / "Dünyada"): aynı listedeki
 * satırların neden önerildiğini tek bakışta söyler.
 *
 * Çapraz-bucket mükerrerler [seen] ile elenir: ilk görülen kazanır
 * (nearby → country → world sırasıyla).
 */
@Composable
private fun SuggestionBucket(
    title: Int,
    results: List<SearchResultUiModel>,
    seen: MutableSet<Long>,
    onAddCity: (City) -> Unit,
    isDarkTheme: Boolean
) {
    val visible = results.filter { it.city.id !in seen }.onEach { seen.add(it.city.id) }
    if (visible.isEmpty()) return
    Text(
        text = stringResource(title),
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .padding(top = HavaSpacing.md, bottom = HavaSpacing.sm)
            .semantics { heading() }
    )
    Column(verticalArrangement = Arrangement.spacedBy(HavaSpacing.sm)) {
        visible.forEach { result ->
            CityWeatherCardContent(
                name = result.city.name,
                region = result.city.region,
                country = result.city.country,
                isLocation = result.city.isLocation,
                cardWeather = result.cardWeather,
                isDarkTheme = isDarkTheme,
                isLoading = result.cardWeather == null,
                onClick = { onAddCity(result.city) },
                onAddClick = { onAddCity(result.city) },
                isInList = result.isInList,
                distanceKm = result.distanceKm,
                enabled = !result.isInList,
                modifier = Modifier.testTag("suggestion_card")
            )
        }
    }
}
