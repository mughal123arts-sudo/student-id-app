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
// ---- App color palette (soft, professional) ----
val AppPrimary = Color(0xFF5C7AEA)          // Soft indigo blue
val AppPrimaryContainer = Color(0xFF3E5FC4) // Deeper blue (for gradient/depth)
val AppBackground = Color(0xFFF5F7FB)       // Soft off-white
val AppSurface = Color(0xFFFFFFFF)
val AppFolderCardColor = Color(0xFFFFF3E6)  // Soft warm cream
val AppButtonColor = Color(0xFF4CAF93)       // Soft emerald/teal
val AppTextColor = Color(0xFF2D3142)         // Charcoal navy

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
    surfaceVariant = Color(0xFFF1F3F9),
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
