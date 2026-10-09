package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val Light = lightColorScheme(
    primary = Color(0xFF124734), onPrimary = Color.White,
    primaryContainer = Color(0xFFE4EDDD), onPrimaryContainer = Color(0xFF124734),
    secondary = Color(0xFF207450), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEAA7), onSecondaryContainer = Color(0xFF253B20),
    background = Color(0xFFF6F4EC), onBackground = Color(0xFF1C3026),
    surface = Color(0xFFFFFEF9), onSurface = Color(0xFF1C3026),
    surfaceVariant = Color(0xFFECEEE3), onSurfaceVariant = Color(0xFF536356),
    outline = Color(0xFF879183), outlineVariant = Color(0xFFDDE1D5),
    error = Color(0xFFAA332C), onError = Color.White,
    errorContainer = Color(0xFFFFE9E3), onErrorContainer = Color(0xFF722019)
)
private val Dark = darkColorScheme(
    primary = Color(0xFFA2D5AA), onPrimary = Color(0xFF123921),
    primaryContainer = Color(0xFF294C36), onPrimaryContainer = Color(0xFFDCEAA7),
    secondary = Color(0xFFDCEAA7), onSecondary = Color(0xFF243B1D),
    secondaryContainer = Color(0xFF354A2D), onSecondaryContainer = Color(0xFFDCEAA7),
    background = Color(0xFF111D18), onBackground = Color(0xFFE9EFE7),
    surface = Color(0xFF1D2B24), onSurface = Color(0xFFE9EFE7),
    surfaceVariant = Color(0xFF28392F), onSurfaceVariant = Color(0xFFB4C3B5),
    outline = Color(0xFF839585), outlineVariant = Color(0xFF3A4D40),
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

