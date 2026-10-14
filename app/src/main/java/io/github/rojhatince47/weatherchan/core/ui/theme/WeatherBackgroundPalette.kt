package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.ui.graphics.Color
import io.github.rojhatince47.weatherchan.feature.weather.domain.model.WeatherCondition

/** Arka plan dekor sahnesinin görsel ailesi. LIGHT_SHOWERS, RAIN'e eşlenir (K6). */
enum class DecorFamily { CLEAR, PARTLY_CLOUDY, CLOUDY, RAIN, THUNDERSTORM, SNOW }

/** Dekor çizim tonları; çizim kodu renk icat etmez. */
data class DecorTones(
    val glow: Color,
    val body: Color,
    val cloudFill: Color,
    val cloudEdge: Color,
    val star: Color,
    val streak: Color,
    val flash: Color
)

/** Hava durumuna göre arka plan renkleri: gradyan + dekor + ön plan renkleri. */
data class WeatherBackgroundPalette(
    val gradient: List<Color>,
    val family: DecorFamily,
    val isDay: Boolean,
    val decor: DecorTones?,
    val contentColor: Color,
    val indicatorActive: Color,
    val indicatorInactive: Color
) {
    companion object {
        fun of(condition: WeatherCondition, isDark: Boolean, isDay: Boolean): WeatherBackgroundPalette {
            val family = when (condition) {
                WeatherCondition.CLEAR -> DecorFamily.CLEAR
                WeatherCondition.PARTLY_CLOUDY -> DecorFamily.PARTLY_CLOUDY
                WeatherCondition.CLOUDY -> DecorFamily.CLOUDY
                WeatherCondition.RAIN -> DecorFamily.RAIN
                WeatherCondition.THUNDERSTORM -> DecorFamily.THUNDERSTORM
                WeatherCondition.SNOW -> DecorFamily.SNOW
                WeatherCondition.LIGHT_SHOWERS -> DecorFamily.RAIN
            }
            val gradient = when {
                !isDark && isDay -> lightDay.getValue(family)
                !isDark -> lightNight.getValue(family)
                isDay -> darkDay.getValue(family)
                else -> darkNight.getValue(family)
            }
            val content: Color
            val active: Color
            val inactive: Color
            when {
                !isDark && isDay -> {
                    content = Color(0xFF12293D)
                    active = lightDayAccents.getValue(family)
                    inactive = Color(0xFF1D3346)
                }
                !isDark -> {
                    content = Color(0xFFE8EEF7)
                    active = Color(0xFFC6E6FA)
                    inactive = Color(0xFFB0C9DB)
                }
                else -> {
                    content = Color(0xFFE9F1F9)
                    active = Color(0xFFBFE3F8)
                    inactive = Color(0xFF9CBFD6)
                }
            }
            return WeatherBackgroundPalette(
                gradient = gradient,
                family = family,
                isDay = isDay,
                decor = decorFor(family, isDay),
                contentColor = content,
                indicatorActive = active,
                indicatorInactive = inactive
            )
        }

        fun neutral(isDark: Boolean): WeatherBackgroundPalette = if (isDark) {
            WeatherBackgroundPalette(
                gradient = listOf(Color(0xFF1B222B), Color(0xFF242C36), Color(0xFF2E3742)),
                family = DecorFamily.CLOUDY,
                isDay = true,
                decor = null,
                contentColor = Color(0xFFE9F1F9),
                indicatorActive = Color(0xFFBFE3F8),
                indicatorInactive = Color(0xFF9CBFD6)
            )
        } else {
            WeatherBackgroundPalette(
                gradient = listOf(Color(0xFFDCE3EA), Color(0xFFE9EEF3), Color(0xFFF4F7FA)),
                family = DecorFamily.CLOUDY,
                isDay = true,
                decor = null,
                contentColor = Color(0xFF12293D),
                indicatorActive = Color(0xFF0E4B7D),
                indicatorInactive = Color(0xFF1D3346)
            )
        }

        // --- Gradyan durakları (üst, orta, alt) ---

        private val lightDay: Map<DecorFamily, List<Color>> = mapOf(
            DecorFamily.CLEAR to listOf(Color(0xFF7FBFE8), Color(0xFFAFD8F0), Color(0xFFE4F1FA)),
            DecorFamily.PARTLY_CLOUDY to listOf(Color(0xFF8FC4E4), Color(0xFFB8D9EC), Color(0xFFE7F1F7)),
            DecorFamily.CLOUDY to listOf(Color(0xFFA3B4C2), Color(0xFFC2CFDA), Color(0xFFE6ECF1)),
            DecorFamily.RAIN to listOf(Color(0xFFA5B6C4), Color(0xFFC0CCD6), Color(0xFFE2E9EE)),
            DecorFamily.THUNDERSTORM to listOf(Color(0xFFABACC4), Color(0xFFC4C5D6), Color(0xFFE4E4EE)),
            DecorFamily.SNOW to listOf(Color(0xFFB2C4D1), Color(0xFFCCD8E1), Color(0xFFECF2F7))
        )

        private val lightNight: Map<DecorFamily, List<Color>> = mapOf(
            DecorFamily.CLEAR to listOf(Color(0xFF22354E), Color(0xFF2C4260), Color(0xFF354D6B)),
            DecorFamily.PARTLY_CLOUDY to listOf(Color(0xFF25394F), Color(0xFF304762), Color(0xFF354F6C)),
            DecorFamily.CLOUDY to listOf(Color(0xFF2B3540), Color(0xFF37424E), Color(0xFF3F4A58)),
            DecorFamily.RAIN to listOf(Color(0xFF23333F), Color(0xFF2E414F), Color(0xFF364958)),
            DecorFamily.THUNDERSTORM to listOf(Color(0xFF2A2547), Color(0xFF362F58), Color(0xFF3E3660)),
            DecorFamily.SNOW to listOf(Color(0xFF2C3A46), Color(0xFF384855), Color(0xFF3F4E5B))
        )

        private val darkDay: Map<DecorFamily, List<Color>> = mapOf(
            DecorFamily.CLEAR to listOf(Color(0xFF1B3450), Color(0xFF23415E), Color(0xFF284663)),
            DecorFamily.PARTLY_CLOUDY to listOf(Color(0xFF1D3548), Color(0xFF264357), Color(0xFF2A4A5F)),
            DecorFamily.CLOUDY to listOf(Color(0xFF20262B), Color(0xFF293035), Color(0xFF2F353B)),
            DecorFamily.RAIN to listOf(Color(0xFF16293C), Color(0xFF1E3549), Color(0xFF253E54)),
            DecorFamily.THUNDERSTORM to listOf(Color(0xFF1D1735), Color(0xFF251E42), Color(0xFF2B244C)),
            DecorFamily.SNOW to listOf(Color(0xFF1B2A36), Color(0xFF233442), Color(0xFF2A3D4D))
        )

        private val darkNight: Map<DecorFamily, List<Color>> = mapOf(
            DecorFamily.CLEAR to listOf(Color(0xFF0D1826), Color(0xFF12202F), Color(0xFF182B3E)),
            DecorFamily.PARTLY_CLOUDY to listOf(Color(0xFF0E1A24), Color(0xFF132430), Color(0xFF1A2E3D)),
            DecorFamily.CLOUDY to listOf(Color(0xFF12171B), Color(0xFF181E23), Color(0xFF1F252B)),
            DecorFamily.RAIN to listOf(Color(0xFF0B1622), Color(0xFF0F1F2E), Color(0xFF15293B)),
            DecorFamily.THUNDERSTORM to listOf(Color(0xFF0F0B1E), Color(0xFF130F26), Color(0xFF18132F)),
            DecorFamily.SNOW to listOf(Color(0xFF0D1720), Color(0xFF121F2A), Color(0xFF172836))
        )

        private val lightDayAccents: Map<DecorFamily, Color> = mapOf(
            DecorFamily.CLEAR to Color(0xFF0E4B7D),
            DecorFamily.PARTLY_CLOUDY to Color(0xFF0D4A6B),
            DecorFamily.CLOUDY to Color(0xFF24384C),
            DecorFamily.RAIN to Color(0xFF123A5E),
            DecorFamily.THUNDERSTORM to Color(0xFF2A2152),
            DecorFamily.SNOW to Color(0xFF23485F)
        )

        // --- Dekor tonları (aile × isDay; temadan bağımsız) ---

        private fun decorFor(family: DecorFamily, isDay: Boolean): DecorTones = when {
            family == DecorFamily.CLEAR && isDay -> sunnyDay
            family == DecorFamily.CLEAR -> moonNight
            family == DecorFamily.PARTLY_CLOUDY && isDay -> sunnyDay
            family == DecorFamily.PARTLY_CLOUDY -> moonNight
            family == DecorFamily.CLOUDY && isDay -> cloudyDay
            family == DecorFamily.CLOUDY -> cloudyNight
            family == DecorFamily.RAIN && isDay -> rainDay
            family == DecorFamily.RAIN -> rainNight
            family == DecorFamily.THUNDERSTORM && isDay -> stormDay
            family == DecorFamily.THUNDERSTORM -> stormNight
            isDay -> snowDay
            else -> snowNight
        }

        private val sunnyDay = DecorTones(
            glow = Color(0x66FFE3A0),
            body = Color(0xFFFFD27A),
            cloudFill = Color(0xCCFFFFFF),
            cloudEdge = Color(0x668FA3B5),
            star = Color.Transparent,
            streak = Color.Transparent,
            flash = Color.Transparent
        )
        private val moonNight = DecorTones(
            glow = Color(0x55BFD3F2),
            body = Color(0xFFDCE3F0),
            cloudFill = Color(0x995A6B80),
            cloudEdge = Color(0x663B4A5C),
            star = Color(0xFFFFFFFF),
            streak = Color.Transparent,
            flash = Color.Transparent
        )
        private val cloudyDay = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0xCCFFFFFF),
            cloudEdge = Color(0x668FA3B5),
            star = Color.Transparent,
            streak = Color.Transparent,
            flash = Color.Transparent
        )
        private val cloudyNight = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0x995A6B80),
            cloudEdge = Color(0x663B4A5C),
            star = Color(0xFFFFFFFF),
            streak = Color.Transparent,
            flash = Color.Transparent
        )
        private val rainDay = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0xCCFFFFFF),
            cloudEdge = Color(0x668FA3B5),
            star = Color.Transparent,
            streak = Color(0xB35B8FC7),
            flash = Color.Transparent
        )
        private val rainNight = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0x995A6B80),
            cloudEdge = Color(0x663B4A5C),
            star = Color.Transparent,
            streak = Color(0xB37FA8CE),
            flash = Color.Transparent
        )
        private val stormDay = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0xCC4A4F63),
            cloudEdge = Color(0x662E3345),
            star = Color.Transparent,
            streak = Color(0xB36E96C7),
            flash = Color(0x66FFFFFF)
        )
        private val stormNight = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0xB33B4257),
            cloudEdge = Color(0x66232838),
            star = Color.Transparent,
            streak = Color(0xB37FA8CE),
            flash = Color(0x66FFFFFF)
        )
        private val snowDay = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0xCCFFFFFF),
            cloudEdge = Color(0x668FA3B5),
            star = Color.Transparent,
            streak = Color(0xFFFFFFFF),
            flash = Color.Transparent
        )
        private val snowNight = DecorTones(
            glow = Color.Transparent,
            body = Color.Transparent,
            cloudFill = Color(0x995A6B80),
            cloudEdge = Color(0x663B4A5C),
            star = Color.Transparent,
            streak = Color(0xCCDCE8F2),
            flash = Color.Transparent
        )
    }
}