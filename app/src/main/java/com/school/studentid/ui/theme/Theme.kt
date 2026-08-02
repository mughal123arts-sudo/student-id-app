package com.school.studentid.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

// ---- App color palette ----
val AppPrimary = Color(0xFF2E8B57)          // Sea Green
val AppPrimaryContainer = Color(0xFF1B5E20)
val AppBackground = Color(0xFFE8F5E9)       // Light Sea Green background
val AppSurface = Color(0xFFFFFFFF)
val AppFolderCardColor = Color(0xFFFFD8A8)  // Light Orange
val AppButtonColor = Color(0xFFF4A261)      // Orange
val AppTextColor = Color(0xFF1B4332)

private val AppColorScheme = lightColorScheme(
    primary = AppPrimary,
    onPrimary = Color.White,
    primaryContainer = AppPrimaryContainer,
    onPrimaryContainer = Color.White,
    secondary = AppButtonColor,
    onSecondary = Color.White,
    background = AppBackground,
    onBackground = AppTextColor,
    surface = AppSurface,
    onSurface = AppTextColor,
    surfaceVariant = Color(0xFFF1F8F1),
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(14.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

private val baseTypography = Typography()
private val AppTypography = baseTypography.copy(
    headlineSmall = baseTypography.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = baseTypography.titleLarge.copy(fontWeight = FontWeight.Bold),
    titleMedium = baseTypography.titleMedium.copy(fontWeight = FontWeight.Bold)
)

@Composable
fun StudentIDAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        shapes = AppShapes,
        typography = AppTypography,
        content = content
    )
}
