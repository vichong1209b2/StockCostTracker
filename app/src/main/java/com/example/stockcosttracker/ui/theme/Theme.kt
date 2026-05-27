package com.example.stockcosttracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.Color

private val LightColors = lightColorScheme(
    primary = Color(0xFF0E5A7A),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6F0FA),
    onPrimaryContainer = Color(0xFF063647),
    secondary = Color(0xFF5B6B36),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE3EBC5),
    onSecondaryContainer = Color(0xFF2F3B13),
    tertiary = Color(0xFF8B4A54),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFD9DE),
    onTertiaryContainer = Color(0xFF4F2029),
    background = Color(0xFFF6F8FA),
    onBackground = Color(0xFF172026),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF172026),
    surfaceVariant = Color(0xFFE2E8EC),
    onSurfaceVariant = Color(0xFF54636C),
    outline = Color(0xFF92A0A8)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF8FD3EB),
    onPrimary = Color(0xFF073544),
    primaryContainer = Color(0xFF0E5A7A),
    onPrimaryContainer = Color(0xFFD6F0FA),
    secondary = Color(0xFFC9D7A5),
    onSecondary = Color(0xFF2F3B13),
    tertiary = Color(0xFFFFB3BF),
    onTertiary = Color(0xFF4F2029),
    background = Color(0xFF101417),
    onBackground = Color(0xFFE8EEF2),
    surface = Color(0xFF171C20),
    onSurface = Color(0xFFE8EEF2),
    surfaceVariant = Color(0xFF3D474D),
    onSurfaceVariant = Color(0xFFC1CBD1)
)

@Composable
fun StockCostTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        content = content
    )
}
