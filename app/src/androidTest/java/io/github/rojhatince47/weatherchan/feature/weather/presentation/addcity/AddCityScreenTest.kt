package io.github.rojhatince47.weatherchan.feature.weather.presentation.addcity

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySeed
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.CitySuggestion
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionGroup
import io.github.rojhatince47.weatherchan.feature.city.domain.suggestion.SuggestionKind
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.City
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.Coordinates
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AddCityScreenTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private val kocaeli = CitySeed("Kocaeli", "TR", "Türkiye", Coordinates(40.7654, 29.9408)).toCity()

    private fun state(
        suggestions: SuggestionGroup? = SuggestionGroup(SuggestionKind.Nearby, listOf(CitySuggestion(kocaeli, 42))),
        searchState: SearchUiState = SearchUiState.Idle,
        query: String = ""
    ) = AddCityUiState(query = query, suggestions = suggestions, searchState = searchState)

    private fun setScreen(state: AddCityUiState, onAddCity: (City) -> Unit = {}, onBack: () -> Unit = {}) {
        composeRule.setContent {
            HavaTheme {
                AddCityScreen(
                    uiState = state,
                    onQueryChange = {},
                    onAddCity = onAddCity,
                    onOpenCity = {},
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
        // "Yakınında" dalında mesafe satırda görünür (ülke adıyla birlikte).
        composeRule.onNodeWithText("42 km", substring = true).assertIsDisplayed()
    }

    @Test
    fun oneriSatirindaBuyutecYoktur() {
        // Öneri keşif akışıdır: satır yalnız ekler, büyüteç (detay) yalnız
        // arama sonuçlarında görünür.
        setScreen(state())
        composeRule.onNodeWithContentDescription("Detayı aç").assertDoesNotExist()
    }

    @Test
    fun ulkeDaliBasligiAnlatir() {
        setScreen(
            state(
                suggestions = SuggestionGroup(SuggestionKind.Country, listOf(CitySuggestion(kocaeli, null)))
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
        // Sonuç boşsa sessizlik değil, açıklayıcı metin gösterilir.
        composeRule.onNodeWithText("Aramanızla eşleşen şehir bulunamadı").assertIsDisplayed()
    }
}
