package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val base = Typography()

internal val HavaTypography = base.copy(
    // Sıcaklık gibi tek büyük sayılar için.
    displayLarge = base.displayLarge.copy(
        fontWeight = FontWeight.Light,
        fontSize = 56.sp,
        lineHeight = 60.sp,
        letterSpacing = (-1.5).sp
    ),
    headlineMedium = base.headlineMedium.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = (-0.5).sp
    ),
    titleLarge = base.titleLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.25).sp
    ),
    titleMedium = base.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = base.bodyLarge.copy(lineHeight = 24.sp),
    bodyMedium = base.bodyMedium.copy(lineHeight = 20.sp),
    labelLarge = base.labelLarge.copy(
        fontWeight = FontWeight.Bold,
        letterSpacing = 0.2.sp
    ),
    // "Sıcaklık kademesi", "Bölüm" gibi üst başlıklar.
    labelSmall = base.labelSmall.copy(
        fontWeight = FontWeight.Medium,
        letterSpacing = 0.8.sp
    )
)
