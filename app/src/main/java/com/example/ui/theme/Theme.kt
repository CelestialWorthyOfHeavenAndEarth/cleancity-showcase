package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = CityColors.forest, onPrimary = Color.White,
    primaryContainer = Color(0xFFEAF4ED), onPrimaryContainer = Color(0xFF124C32),
    secondary = CityColors.forest, onSecondary = Color.White,
    secondaryContainer = CityColors.forest, onSecondaryContainer = Color.White,
    tertiary = Color(0xFF287557), onTertiary = Color.White,
    tertiaryContainer = Color(0xFFEAF4ED), onTertiaryContainer = Color(0xFF124C32),
    background = Color.White, onBackground = Color(0xFF203B2B),
    surface = Color.White, onSurface = Color(0xFF203B2B),
    surfaceVariant = Color(0xFFF0F5F2), onSurfaceVariant = Color(0xFF526B5C),
    surfaceDim = Color(0xFFE5ECE7), surfaceBright = Color.White,
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFF8FAF8),
    surfaceContainer = Color(0xFFF2F6F3), surfaceContainerHigh = Color(0xFFECF2EE),
    surfaceContainerHighest = Color(0xFFE5ECE7), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFF223A2B), inverseOnSurface = Color(0xFFF3F8F5), inversePrimary = Color(0xFF9ADBBC),
    outline = Color(0xFF7A9184), outlineVariant = Color(0xFFDFE8E1),
    error = Color(0xFFAA332C), onError = Color.White,
    errorContainer = Color(0xFFFFE9E3), onErrorContainer = Color(0xFF722019)
)
private val Dark = darkColorScheme(
    primary = Color(0xFF91D7B1), onPrimary = Color(0xFF073B24),
    primaryContainer = Color(0xFF244D36), onPrimaryContainer = Color(0xFFDCEFE2),
    secondary = Color(0xFF91D7B1), onSecondary = Color(0xFF073B24),
    secondaryContainer = Color(0xFF28573C), onSecondaryContainer = Color(0xFFF1F9F4),
    tertiary = Color(0xFF9DD7BD), onTertiary = Color(0xFF073B24),
    tertiaryContainer = Color(0xFF244D36), onTertiaryContainer = Color(0xFFDCEFE2),
    background = Color(0xFF111D18), onBackground = Color(0xFFEDF5EF),
    surface = Color(0xFF1A2D24), onSurface = Color(0xFFEDF5EF),
    surfaceVariant = Color(0xFF293E32), onSurfaceVariant = Color(0xFFB6CCBE),
    surfaceDim = Color(0xFF111D18), surfaceBright = Color(0xFF344D3F),
    surfaceContainerLowest = Color(0xFF0D1712), surfaceContainerLow = Color(0xFF17281F),
    surfaceContainer = Color(0xFF1A2D24), surfaceContainerHigh = Color(0xFF22372B),
    surfaceContainerHighest = Color(0xFF293E32), surfaceTint = Color.Transparent,
    inverseSurface = Color(0xFFE2EFE6), inverseOnSurface = Color(0xFF203B2B), inversePrimary = CityColors.forest,
    outline = Color(0xFF87A08F), outlineVariant = Color(0xFF3B5143),
    error = Color(0xFFFFB4A8), onError = Color(0xFF641D17),
    errorContainer = Color(0xFF542A25), onErrorContainer = Color(0xFFFFDAD2)
)
@Composable
@Suppress("UNUSED_PARAMETER")
fun CleanCityTheme(darkTheme: Boolean = isSystemInDarkTheme(), dynamicColor: Boolean = false, content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = if (darkTheme) Dark else Light, typography = Typography,
        shapes = Shapes(RoundedCornerShape(8.dp), RoundedCornerShape(12.dp), RoundedCornerShape(20.dp),
            RoundedCornerShape(28.dp), RoundedCornerShape(32.dp)), content = content)
}
object CitySpacing {
    val screen = 20.dp
    val small = 8.dp
    val medium = 16.dp
    val section = 28.dp
    val touch = 48.dp
}

