package io.github.rojhatince47.weatherchan.feature.weather.domain.advice

import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WeatherAdviceEngineTest {

    private val engine = WeatherAdviceEngine()

    private fun advice(
        condition: WeatherCondition = WeatherCondition.CLEAR,
        temperature: Int = 20,
        wind: Int = 0,
        isDay: Boolean = true,
        upcoming: List<HourlyOutlook> = emptyList(),
        cityId: Long = 1L,
        hour: Int = 12,
        epochDay: Long = 20_000L
    ): WeatherAdvice = engine.advise(
        AdviceContext(
            cityId = cityId,
            temperatureCelsius = temperature,
            condition = condition,
            isDay = isDay,
            windSpeedKmh = wind,
            upcomingHours = upcoming,
            epochDay = epochDay,
            hourOfDay = hour
        )
    )

    @Test
    fun `her kosul bir kategoriye duser ve poz tasir`() {
        WeatherCondition.entries.forEach { condition ->
            val result = advice(condition = condition, temperature = 18)
            assertTrue("$condition icin poz yok", CompanionPose.entries.contains(result.pose))
        }
    }

    @Test
    fun `kategoriler ve pozlar tabloya gore eslesir`() {
        assertEquals(AdviceCategory.STORM, advice(WeatherCondition.THUNDERSTORM).category)
        assertEquals(AdviceCategory.SNOW, advice(WeatherCondition.SNOW).category)
        assertEquals(AdviceCategory.RAIN, advice(WeatherCondition.RAIN).category)
        assertEquals(AdviceCategory.RAIN, advice(WeatherCondition.LIGHT_SHOWERS).category)
        assertEquals(AdviceCategory.CLEAR, advice(WeatherCondition.CLEAR).category)
        assertEquals(AdviceCategory.CLEAR, advice(WeatherCondition.PARTLY_CLOUDY).category)
        assertEquals(AdviceCategory.CLOUDY, advice(WeatherCondition.CLOUDY).category)

        assertEquals(CompanionPose.RAINY, advice(WeatherCondition.THUNDERSTORM).pose)
        assertEquals(CompanionPose.COLD, advice(WeatherCondition.SNOW).pose)
        assertEquals(CompanionPose.CHEERFUL, advice(WeatherCondition.CLEAR).pose)
        assertEquals(CompanionPose.CALM, advice(WeatherCondition.CLOUDY).pose)
        assertEquals(CompanionPose.SLEEPY, advice(isDay = false).pose)
    }

    @Test
    fun `gece gunduz ayrimi geceyi oncele`() {
        assertEquals(
            AdviceCategory.NIGHT,
            advice(WeatherCondition.PARTLY_CLOUDY, isDay = false).category
        )
        assertEquals(
            AdviceCategory.CLEAR,
            advice(WeatherCondition.PARTLY_CLOUDY, isDay = true).category
        )
    }

    @Test
    fun `kategori poz tablosu birebir`() {
        val expected = mapOf(
            AdviceCategory.STORM to CompanionPose.RAINY,
            AdviceCategory.SNOW to CompanionPose.COLD,
            AdviceCategory.RAIN to CompanionPose.RAINY,
            AdviceCategory.RAIN_SOON to CompanionPose.RAINY,
            AdviceCategory.WINDY to CompanionPose.WINDY,
            AdviceCategory.COLD to CompanionPose.COLD,
            AdviceCategory.HOT to CompanionPose.CHEERFUL,
            AdviceCategory.NIGHT to CompanionPose.SLEEPY,
            AdviceCategory.CLEAR to CompanionPose.CHEERFUL,
            AdviceCategory.CLOUDY to CompanionPose.CALM
        )
        expected.forEach { (category, pose) -> assertEquals("$category", pose, category.pose()) }
    }

    @Test
    fun `oncelik sirasi guvenlikten konfora`() {
        assertEquals(AdviceCategory.STORM, advice(WeatherCondition.THUNDERSTORM, -5, 50, false).category)
        assertEquals(AdviceCategory.SNOW, advice(WeatherCondition.SNOW, 35, 40, isDay = false).category)
        assertEquals(AdviceCategory.RAIN, advice(WeatherCondition.RAIN, 35).category)
        assertEquals(AdviceCategory.WINDY, advice(temperature = 2, wind = 36, isDay = false).category)
        assertEquals(AdviceCategory.COLD, advice(temperature = 2, isDay = false).category)
        assertEquals(AdviceCategory.HOT, advice(temperature = 31, isDay = false).category)
        assertEquals(AdviceCategory.NIGHT, advice(temperature = 15, isDay = false).category)
    }

    @Test
    fun `yakin yagmur soguk ve ruzgardan once`() {
        assertEquals(
            AdviceCategory.RAIN_SOON,
            advice(
                temperature = 2,
                wind = 36,
                isDay = false,
                upcoming = listOf(HourlyOutlook(50, WeatherCondition.RAIN))
            ).category
        )
    }

    @Test
    fun `sicak gece ve bulutludan once`() {
        assertEquals(
            AdviceCategory.HOT,
            advice(WeatherCondition.CLOUDY, temperature = 31, isDay = false).category
        )
    }

    @Test
    fun `ruzgar soguktan once`() {
        assertEquals(
            AdviceCategory.WINDY,
            advice(temperature = 2, wind = 35, isDay = false).category
        )
    }

    @Test
    fun `gece acik ve bulutludan once`() {
        assertEquals(
            AdviceCategory.NIGHT,
            advice(WeatherCondition.CLEAR, temperature = 15, isDay = false).category
        )
        assertEquals(
            AdviceCategory.NIGHT,
            advice(WeatherCondition.CLOUDY, temperature = 15, isDay = false).category
        )
    }

    @Test
    fun `light showers yagmur ailesi ve rainy pozdadir`() {
        val result = advice(WeatherCondition.LIGHT_SHOWERS)
        assertEquals(AdviceCategory.RAIN, result.category)
        assertEquals(CompanionPose.RAINY, result.pose)
    }

    @Test
    fun `tohum gun ve kategoriyle degisir`() {
        assertNotEquals(
            advice(epochDay = 20_000L).variantSeed,
            advice(epochDay = 20_001L).variantSeed
        )
        assertNotEquals(
            advice(WeatherCondition.CLEAR).variantSeed,
            advice(WeatherCondition.CLOUDY).variantSeed
        )
    }

    @Test
    fun `esik sinirlari`() {
        assertEquals(AdviceCategory.COLD, advice(temperature = 10).category)
        assertEquals(AdviceCategory.CLEAR, advice(temperature = 11).category)
        assertEquals(AdviceCategory.HOT, advice(temperature = 30).category)
        assertEquals(AdviceCategory.CLEAR, advice(temperature = 29).category)
        assertEquals(AdviceCategory.WINDY, advice(wind = 35).category)
        assertEquals(AdviceCategory.CLEAR, advice(wind = 34).category)
    }

    @Test
    fun `yakin yagmur yalniz yagmur ailesinde ve esikte tetikler`() {
        val rainSoon = advice(
            upcoming = listOf(HourlyOutlook(50, WeatherCondition.RAIN))
        )
        assertEquals(AdviceCategory.RAIN_SOON, rainSoon.category)
        assertEquals(CompanionPose.RAINY, rainSoon.pose)

        assertEquals(
            AdviceCategory.CLEAR,
            advice(upcoming = listOf(HourlyOutlook(49, WeatherCondition.RAIN))).category
        )
        assertEquals(
            AdviceCategory.CLEAR,
            advice(upcoming = listOf(HourlyOutlook(90, WeatherCondition.SNOW))).category
        )
        assertEquals(AdviceCategory.CLEAR, advice(upcoming = emptyList()).category)
        assertEquals(
            AdviceCategory.RAIN_SOON,
            advice(wind = 36, upcoming = listOf(HourlyOutlook(80, WeatherCondition.LIGHT_SHOWERS))).category
        )
        assertEquals(
            AdviceCategory.RAIN,
            advice(WeatherCondition.RAIN, upcoming = listOf(HourlyOutlook(80, WeatherCondition.RAIN))).category
        )
    }

    @Test
    fun `tohum ayni girdide ayni farkli saatte farkli`() {
        val context = AdviceContext(7L, 18, WeatherCondition.CLEAR, true, 5, emptyList(), 20_000L, 12)
        assertEquals(engine.advise(context).variantSeed, engine.advise(context).variantSeed)
        assertNotEquals(
            engine.advise(context).variantSeed,
            engine.advise(context.copy(hourOfDay = 13)).variantSeed
        )
    }

    @Test
    fun `negatif sehir tohumu da kararli ve ayirt edicidir`() {
        val negative = advice(cityId = -1L)
        assertEquals(negative.variantSeed, advice(cityId = -1L).variantSeed)
        assertNotEquals(advice(cityId = 1L).variantSeed, negative.variantSeed)
        assertNotEquals(negative.variantSeed, advice(cityId = -1L, hour = 13).variantSeed)
    }
}
