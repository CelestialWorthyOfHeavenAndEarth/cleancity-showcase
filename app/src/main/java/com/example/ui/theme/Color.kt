package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

object CityColors {
    val canvas: Color @Composable get() = MaterialTheme.colorScheme.background
    val surface: Color @Composable get() = MaterialTheme.colorScheme.surface
    val soft: Color @Composable get() = MaterialTheme.colorScheme.surfaceVariant
    val ink: Color @Composable get() = MaterialTheme.colorScheme.onSurface
    val muted: Color @Composable get() = MaterialTheme.colorScheme.onSurfaceVariant
    val primary: Color @Composable get() = MaterialTheme.colorScheme.primary
    val onPrimary: Color @Composable get() = MaterialTheme.colorScheme.onPrimary
    val line: Color @Composable get() = MaterialTheme.colorScheme.outlineVariant
    val positive: Color @Composable get() = MaterialTheme.colorScheme.primary
    val positiveBg: Color @Composable get() = MaterialTheme.colorScheme.primaryContainer
    val warning: Color @Composable get() = if (MaterialTheme.colorScheme.background == Color(0xFF111D18)) Color(0xFFFFD18D) else Color(0xFF865200)
    val warningBg: Color @Composable get() = if (MaterialTheme.colorScheme.background == Color(0xFF111D18)) Color(0xFF443421) else Color(0xFFFCEDCE)
    val blue: Color @Composable get() = if (MaterialTheme.colorScheme.background == Color(0xFF111D18)) Color(0xFFA8C9FF) else Color(0xFF265D91)
    val blueBg: Color @Composable get() = if (MaterialTheme.colorScheme.background == Color(0xFF111D18)) Color(0xFF233A50) else Color(0xFFE5EFF7)
    val error: Color @Composable get() = MaterialTheme.colorScheme.error
    val errorBg: Color @Composable get() = MaterialTheme.colorScheme.errorContainer
    val forest = Color(0xFF124734)
    val lime = Color(0xFFDCEAA7)
    val onForest = Color(0xFFF7FAF2)
}

