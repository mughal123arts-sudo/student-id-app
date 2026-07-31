package com.school.studentid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val NavyPrimary = Color(0xFF153F82)
private val NavyPrimaryContainer = Color(0xFF0E2C5C)
private val AccentTeal = Color(0xFF00897B)
private val BackgroundLight = Color(0xFFF6F8FC)
private val SurfaceLight = Color(0xFFFFFFFF)
private val SurfaceVariantLight = Color(0xFFEDF1F8)

private val AppColorScheme = lightColorScheme(
    primary = NavyPrimary,
    onPrimary = Color.White,
    primaryContainer = NavyPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = AccentTeal,
    onSecondary = Color.White,
    background = BackgroundLight,
    onBackground = Color(0xFF1B1B1F),
    surface = SurfaceLight,
    onSurface = Color(0xFF1B1B1F),
    surfaceVariant = SurfaceVariantLight,
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun StudentIDAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        shapes = AppShapes,
        content = content
    )
}
