package io.github.rojhatince47.weatherchan.feature.weather.presentation.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.github.rojhatince47.weatherchan.R
import io.github.rojhatince47.weatherchan.core.ui.state.UiState
import io.github.rojhatince47.weatherchan.core.ui.text.UiText
import io.github.rojhatince47.weatherchan.core.ui.theme.HavaTheme
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CityUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.CurrentWeatherUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.DailyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.ForecastDetailUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.HourlyForecastUiModel
import io.github.rojhatince47.weatherchan.feature.weather.presentation.model.WeatherIcon
import io.github.rojhatince47.weatherchan.testing.HavaTestActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class ForecastContentTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<HavaTestActivity>()

    private fun detailFixture(hourlyCount: Int = 25) = ForecastDetailUiModel(
        city = CityUiModel(1, "İstanbul", "Marmara", "Türkiye", 41.01, 28.98),
        currentWeather = CurrentWeatherUiModel(
            temperatureText = "18°C",
            icon = WeatherIcon.Glyph("☀"),
            conditionLabel = UiText.Resource(R.string.condition_clear),
            windSpeedText = "8 km/s",
            humidityText = "%50",
            condition = WeatherCondition.CLEAR,
            isDay = true
        ),
        hourlyForecast = List(hourlyCount) { i ->
            HourlyForecastUiModel(
                // 14:00 → yarın 14:00 oluk simülasyonu: ilk hücre "Şimdi",
                // gerisi saat (pencere mapper'da değil — bu katman yalnız çizim).
                timeLabel = if (i == 0) UiText.Resource(R.string.hourly_now) else UiText.Dynamic("%02d:00".format((14 + i) % 24)),
                temperatureText = "2${i % 10}°C",
                temperatureValue = 20 + i % 10,
                icon = WeatherIcon.Glyph("⛅"),
                precipitationProbabilityText = "%10"
            )
        },
        dailyForecast = List(7) { day ->
            DailyForecastUiModel(
                dayOffset = day,
                dateText = UiText.Resource(R.string.forecast_today),
                minTemperatureText = "10°C",
                maxTemperatureText = "20°C",
                icon = WeatherIcon.Glyph("☁"),
                condition = WeatherCondition.CLOUDY
            )
        }
    )

    /**
     * Düz içerik — grafik/hücre görünürlük testleri için. İlk hücre doğal
     * olarak görünürde çizilir.
     */
    private fun setForecastContent(hourlyCount: Int = 25) {
        composeRule.setContent {
            HavaTheme {
                ForecastContent(
                    uiState = DetailUiState(content = UiState.Success(detailFixture(hourlyCount))),
                    onRetry = {},
                    onRefresh = {},
                    isDark = false,
                    contentColor = Color.Black
                )
            }
        }
    }

    /**
     * Yüksekliği sabit sarmalayıcı — LazyColumn sanallaştırması tüm 34 öğeyi
     * compose etsin diye (genişlik ölçümü için). Kök içerik pencereden büyük
     * olduğu için dikeyde ortalanır; bu yalnız ölçüm testi içindir, görünürlük
     * assertion'ı kurulamaz (bkz. grafik testi).
     */
    private fun setForecastContentWrapped(hourlyCount: Int = 24) {
        composeRule.setContent {
            HavaTheme {
                Box(Modifier.requiredHeight(4000.dp)) {
                    ForecastContent(
                        uiState = DetailUiState(content = UiState.Success(detailFixture(hourlyCount))),
                        onRetry = {},
                        onRefresh = {},
                        isDark = false,
                        contentColor = Color.Black
                    )
                }
            }
        }
    }

    private fun setForecastContentScaled(hourlyCount: Int, fontScale: Float) {
        composeRule.setContent {
            val base = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(base.density, fontScale)) {
                HavaTheme {
                    Box(Modifier.requiredHeight(4000.dp)) {
                        ForecastContent(
                            uiState = DetailUiState(content = UiState.Success(detailFixture(hourlyCount))),
                            onRetry = {},
                            onRefresh = {},
                            isDark = false,
                            contentColor = Color.Black
                        )
                    }
                }
            }
        }
    }

    private fun widthDp(tag: String): Float {
        val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    private fun widthDp(index: Int, tag: String): Float {
        val bounds = composeRule.onAllNodesWithTag(tag)[index].getUnclippedBoundsInRoot()
        return (bounds.right - bounds.left).value
    }

    private fun setForecastContentOnBackground(contentColor: Color, background: Color, uiState: DetailUiState) {
        composeRule.setContent {
            HavaTheme {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(background)
                ) {
                    ForecastContent(
                        uiState = uiState,
                        onRetry = {},
                        onRefresh = {},
                        isDark = false,
                        contentColor = contentColor
                    )
                }
            }
        }
    }

    private fun countLightPixels(image: android.graphics.Bitmap): Int {
        var count = 0
        for (y in 0 until image.height step 2) {
            for (x in 0 until image.width step 2) {
                val p = image.getPixel(x, y)
                if (android.graphics.Color.red(p) > 240 &&
                    android.graphics.Color.green(p) > 240 &&
                    android.graphics.Color.blue(p) > 240
                ) {
                    count++
                }
            }
        }
        return count
    }

    @Test
    fun bolumBasliklariVerilenContentColorIleCizilir() {
        // Açık tema + koyu arka plan (gece paleti) durumu: başlıklar açık renk
        // olmalı; tema varsayılanı koyu gri kalırsa bu test düşer (regresyon kilidi).
        setForecastContentOnBackground(
            contentColor = Color.White,
            background = Color(0xFF22354E),
            uiState = DetailUiState(content = UiState.Success(detailFixture(3)))
        )
        val image = composeRule.onNodeWithTag("pull_refresh").captureToImage().asAndroidBitmap()
        assertTrue("bolum basliklari acik renkte cizilmedi", countLightPixels(image) > 50)
    }

    @Test
    fun hataMetniVerilenContentColorIleCizilir() {
        setForecastContentOnBackground(
            contentColor = Color.White,
            background = Color(0xFF22354E),
            uiState = DetailUiState(content = UiState.Error(UiText.Dynamic("Baglanti hatasi")))
        )
        val image = composeRule.onNodeWithTag("pull_refresh").captureToImage().asAndroidBitmap()
        assertTrue("hata metni acik renkte cizilmedi", countLightPixels(image) > 50)
    }

    @Test
    fun evKartlariTamGenislikCizilir() {
        setForecastContentWrapped()
        val expectedWidth = widthDp("pull_refresh") - 32f

        assertEquals(expectedWidth, widthDp("current_weather_card"), 1f)
        assertEquals(expectedWidth, widthDp("hourly_chart"), 1f)
        repeat(7) { index ->
            assertEquals(expectedWidth, widthDp(index, "daily_card"), 1f)
        }
    }

    @Test
    fun saatlikGrafikHucreleriKaydirilabilir() {
        setForecastContent()
        composeRule.onNodeWithTag("hourly_chart").assertExists()
        composeRule.onNodeWithText("Saatlik Tahmin").assertExists()
        composeRule.onNodeWithText(composeRule.activity.getString(R.string.hourly_now)).assertIsDisplayed()
        composeRule.onNodeWithText("15:00").assertExists() // kırpılmış olsa da semantics'te var
        composeRule.onNodeWithText("14:00").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun saatlikListeBoskenGrafikCizilmez() {
        setForecastContent(hourlyCount = 0)
        composeRule.onNodeWithTag("hourly_chart").assertDoesNotExist()
        composeRule.onNodeWithText("Saatlik Tahmin").assertExists()
    }

    @Test
    fun hucreIcerigiKirpmiyorYuksekFontOlceginde() {
        setForecastContentScaled(hourlyCount = 4, fontScale = 1.5f)

        fun bounds(tag: String) = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

        fun height(tag: String): Float {
            val b = bounds(tag)
            return (b.bottom - b.top).value
        }

        repeat(4) { i ->
            val cellBottom = bounds("hourly_cell_$i").bottom.value
            listOf("hourly_time_$i", "hourly_icon_$i", "hourly_temp_$i", "hourly_precip_$i").forEach { tag ->
                // Sıkışma tespiti: sabit hücre yüksekliği bütçeyi bitirince alt
                // satırlar sıfır yüksekliğe düşer (yüzde "gözükmüyor" semptomu).
                assertTrue("$tag sikistirilmis (h=${height(tag)})", height(tag) >= 4f)
                assertTrue("$tag hucre alt sinirini asiyor", bounds(tag).bottom.value <= cellBottom + 0.5f)
            }
        }
        val first = height("hourly_cell_0")
        repeat(4) { i -> assertEquals(first, height("hourly_cell_$i"), 1f) }
    }
}
