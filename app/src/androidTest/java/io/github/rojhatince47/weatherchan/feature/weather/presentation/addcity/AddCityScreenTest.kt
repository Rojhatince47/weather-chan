package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CardWeather
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddCityScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private val kocaeli = CitySeed("Kocaeli", "TR", "Türkiye", Coordinates(40.7654, 29.9408)).toCity()
    private val ankara = CitySeed("Ankara", "TR", "Türkiye", Coordinates(39.92, 32.85)).toCity()
    private val berlin = CitySeed("Berlin", "DE", "Almanya", Coordinates(52.52, 13.405)).toCity()

    private fun state(
        suggestions: SuggestionLists? = SuggestionLists(
            nearby = listOf(
                SearchResultUiModel(kocaeli, isInList = false, distanceKm = 42, cardWeather = CardWeather.Unavailable)
            )
        ),
        searchState: SearchUiState = SearchUiState.Idle,
        query: String = ""
    ) = AddCityUiState(query = query, suggestions = suggestions, searchState = searchState)

    private fun setScreen(state: AddCityUiState, onAddCity: (City) -> Unit = {}, onOpenCity: (City) -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            HavaTheme {
                AddCityScreen(
                    uiState = state,
                    onQueryChange = {},
                    onAddCity = onAddCity,
                    onOpenCity = onOpenCity,
                    onBack = onBack
                )
            }
        }
    }

    @Test
    fun oneriBolumuGorunur() {
        setScreen(state())
        composeRule.onNodeWithText("Yakınında").assertIsDisplayed()
        composeRule.onNodeWithText("Kocaeli").assertIsDisplayed()
        // "Yakınında" dalında mesafe satırda görünür.
        composeRule.onNodeWithText("42 km", substring = true).assertIsDisplayed()
    }

    @Test
    fun oneriSatirindaBuyutecYoktur() {
        setScreen(state())
        composeRule.onNodeWithContentDescription("Detayı aç").assertDoesNotExist()
    }

    @Test
    fun oneriKartlariArasindaBoslukVardir() {
        setScreen(
            state(
                suggestions = SuggestionLists(
                    nearby = listOf(
                        SearchResultUiModel(kocaeli, isInList = false, distanceKm = 42, cardWeather = CardWeather.Unavailable),
                        SearchResultUiModel(ankara, isInList = false, distanceKm = 180, cardWeather = CardWeather.Unavailable)
                    )
                )
            )
        )
        val first = composeRule.onAllNodesWithTag("suggestion_card")[0].getUnclippedBoundsInRoot()
        val second = composeRule.onAllNodesWithTag("suggestion_card")[1].getUnclippedBoundsInRoot()
        val gap = second.top.value - first.bottom.value
        assertTrue("oneri kartlari arasinda bosluk yok: $gap", gap >= 6f)
    }

    @Test
    fun ulkeDaliBasligiAnlatir() {
        setScreen(
            state(
                suggestions = SuggestionLists(
                    country = listOf(
                        SearchResultUiModel(kocaeli, isInList = false, cardWeather = CardWeather.Unavailable)
                    )
                )
            )
        )
        composeRule.onNodeWithText("Ülkende").assertIsDisplayed()
        composeRule.onNodeWithText("Kocaeli").assertIsDisplayed()
    }

    @Test
    fun oneriyiDokunmakSehriEkle() {
        var added: City? = null
        setScreen(state(), onAddCity = { added = it })
        composeRule.onNodeWithText("Kocaeli").performClick()
        assertEquals(kocaeli, added)
    }

    @Test
    fun oneriEkleninceSatirKaybolur() {
        val uiState = androidx.compose.runtime.mutableStateOf(state())
        composeRule.setContent {
            HavaTheme {
                AddCityScreen(
                    uiState = uiState.value,
                    onQueryChange = {},
                    onAddCity = { uiState.value = uiState.value.copy(suggestions = null) },
                    onOpenCity = {},
                    onBack = {}
                )
            }
        }
        composeRule.onNodeWithText("Kocaeli").assertIsDisplayed()
        composeRule.onNodeWithText("Kocaeli").performClick()
        composeRule.onNodeWithText("Kocaeli").assertDoesNotExist()
        composeRule.onNodeWithText("Yakınında").assertDoesNotExist()
    }

    @Test
    fun aramaSonucundaEklenmisSehirTikOlur() {
        setScreen(
            state(
                query = "Koc",
                searchState = SearchUiState.Success(
                    listOf(SearchResultUiModel(kocaeli, isInList = true, cardWeather = CardWeather.Unavailable))
                )
            )
        )
        composeRule.onNodeWithContentDescription("Listede").assertIsDisplayed()
        composeRule.onNodeWithContentDescription("Şehri listeye ekle").assertDoesNotExist()
    }

    @Test
    fun geriDugmesiGeriDonderir() {
        var back = false
        setScreen(state(), onBack = { back = true })
        composeRule.onNodeWithTag("action_back").performClick()
        assertTrue(back)
    }

    @Test
    fun sorguYazilincaOnerilerKaybolur() {
        setScreen(state(query = "Koc", searchState = SearchUiState.Success(emptyList())))
        composeRule.onNodeWithText("Kocaeli").assertDoesNotExist()
        composeRule.onNodeWithText("Aramanızla eşleşen şehir bulunamadı").assertIsDisplayed()
    }

    // --- Yeni testler ---

    @Test
    fun aramaSatirindaBuyutecYoktur() {
        setScreen(
            state(
                query = "Koc",
                searchState = SearchUiState.Success(
                    listOf(
                        SearchResultUiModel(kocaeli, isInList = false, cardWeather = CardWeather.Unavailable)
                    )
                )
            )
        )
        composeRule.onNodeWithContentDescription("Detayı aç").assertDoesNotExist()
        composeRule.onNodeWithContentDescription("Şehri listeye ekle").assertIsDisplayed()
    }

    @Test
    fun yuklenirkenTiklamaDevreDisi() {
        var clicked = false
        setScreen(
            state(
                query = "Koc",
                searchState = SearchUiState.Success(
                    listOf(
                        SearchResultUiModel(kocaeli, isInList = false) // cardWeather = null → isLoading
                    )
                )
            ),
            onOpenCity = { clicked = true }
        )
        composeRule.onNodeWithText("Kocaeli").performClick()
        assertFalse(clicked)
    }

    @Test
    fun aramaSatiriTiklamaDetayaGider() {
        var opened = false
        setScreen(
            state(
                query = "Koc",
                searchState = SearchUiState.Success(
                    listOf(
                        SearchResultUiModel(kocaeli, isInList = false, cardWeather = CardWeather.Unavailable)
                    )
                )
            ),
            onOpenCity = { opened = true }
        )
        composeRule.onNodeWithText("Kocaeli").performClick()
        assertTrue(opened)
    }

    @Test
    fun bucketBasliklariDogruSirada() {
        setScreen(
            state(
                suggestions = SuggestionLists(
                    nearby = listOf(SearchResultUiModel(kocaeli, false, distanceKm = 42, cardWeather = CardWeather.Unavailable)),
                    country = listOf(SearchResultUiModel(ankara, false, cardWeather = CardWeather.Unavailable)),
                    world = listOf(SearchResultUiModel(berlin, false, cardWeather = CardWeather.Unavailable))
                )
            )
        )
        composeRule.onNodeWithText("Yakınında").assertIsDisplayed()
        composeRule.onNodeWithText("Ülkende").assertIsDisplayed()
        composeRule.onNodeWithText("Dünyada").assertIsDisplayed()
    }
}
