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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.text.toUiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaSpacing
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySuggestion
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionGroup
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionKind
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City

/**
 * Şehir ekleme ekranı: arama kutusu + konum/ülke/dünya önerileri.
 *
 * Sorgu boşken öneri bölümü, doluyken arama sonuçları gösterilir — ikisi aynı
 * anda değil, çünkü ikisi de "nereye gitmek istiyorsun" sorusunun farklı
 * cevaplarıdır ve birlikte ekranı kirlidir.
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
                uiState.suggestions?.let { group ->
                    SuggestionSection(
                        group = group,
                        onAddCity = onAddCity,
                        // Kaydırılabilir: klavye açıkken ya da büyük fontta sekiz
                        // kart ekrandan taşar; `MainScreen`'deki listenin
                        // çözümüyle aynı (verticalScroll).
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                    )
                }
            } else if (search is SearchUiState.Success && search.results.isEmpty()) {
                // Sonuç yoksa hiçbir şey göstermemek, arama çalışmıyor izlenimi
                // verir. Aynı metin ana ekrandan kaldırıldı; burada yaşıyor.
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
                    // Aynı ~110 m hücresinden iki kayıt gelirse LazyColumn'un
                    // key'i çakışıp çalışma anında çöker; distinctBy sigortadır.
                    items(search.results.distinctBy { it.city.id }, key = { it.city.id }) { result ->
                        SearchResultRow(
                            result = result,
                            onAddClick = { onAddCity(result.city) },
                            onOpenClick = { onOpenCity(result.city) }
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
 */
@Composable
private fun SuggestionSection(group: SuggestionGroup, onAddCity: (City) -> Unit, modifier: Modifier = Modifier) {
    val title = stringResource(
        when (group.kind) {
            SuggestionKind.Nearby -> R.string.suggestion_nearby
            SuggestionKind.Country -> R.string.suggestion_country
            SuggestionKind.World -> R.string.suggestion_world
        }
    )
    Column(modifier = modifier.padding(horizontal = HavaSpacing.lg)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(top = HavaSpacing.md, bottom = HavaSpacing.sm)
                .semantics { heading() }
        )
        group.suggestions.forEach { suggestion: CitySuggestion ->
            SearchResultRow(
                result = SearchResultUiModel(suggestion.city, isInList = false),
                onAddClick = { onAddCity(suggestion.city) },
                onOpenClick = null,
                distanceKm = suggestion.distanceKm
            )
        }
    }
}
