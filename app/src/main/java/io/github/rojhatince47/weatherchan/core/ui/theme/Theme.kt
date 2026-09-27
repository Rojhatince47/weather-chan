package io.github.rojhatince47.weatherchan.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import io.github.rojhatince47.weatherchan.feature.settings.domain.model.ThemeMode

private val LightColors = lightColorScheme(
    primary = Teal40,
    onPrimary = Neutral99,
    primaryContainer = Teal90,
    onPrimaryContainer = Teal10,
    secondary = Amber40,
    onSecondary = Neutral99,
    secondaryContainer = Amber90,
    onSecondaryContainer = Amber40,
    background = Neutral99,
    onBackground = Neutral10,
    surface = Neutral99,
    onSurface = Neutral10,
    surfaceVariant = NeutralVariant90,
    onSurfaceVariant = NeutralVariant30,
    surfaceContainer = Neutral95,
    outline = Color(0xFF6F7979),
    outlineVariant = NeutralVariant90,
    error = Red40,
    onError = Neutral99
)

private val DarkColors = darkColorScheme(
    primary = Teal80,
    onPrimary = Teal20,
    primaryContainer = Teal30,
    onPrimaryContainer = Teal90,
    secondary = Amber80,
    onSecondary = Neutral10,
    secondaryContainer = Amber40,
    onSecondaryContainer = Amber90,
    background = Neutral10,
    onBackground = Neutral90,
    surface = Neutral10,
    onSurface = Neutral90,
    surfaceVariant = NeutralVariant30,
    onSurfaceVariant = Neutral90,
    surfaceContainer = Color(0xFF1D2020),
    outline = Color(0xFF8C9292),
    outlineVariant = NeutralVariant30,
    error = Red80,
    onError = Neutral10
)

@Composable
fun HavaTheme(themeMode: ThemeMode = ThemeMode.SYSTEM, content: @Composable () -> Unit) {
    // isSystemInDarkTheme() composable olduğu için varsayılan parametre değerinde
    // çağrılamaz; burada gövde içinde çözülür.
    val darkTheme = themeMode.resolveIsDark()
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = HavaTypography,
        shapes = HavaShapes,
        content = content
    )
}
