package com.mettyoung.fitbro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Modern Premium Palette
val MiOrange = Color(0xFFFF6B00) // Slightly more vibrant
val MiBackground = Color(0xFFF8F9FA)
val MiCardBackground = Color(0xFFFFFFFF)
val MiTextPrimary = Color(0xFF1A1C1E)
val MiTextSecondary = Color(0xFF6C757D)

// Macro Colors - More modern tones
val ColorProtein = Color(0xFF4361EE)
val ColorCarbs = Color(0xFFF72585)
val ColorFat = Color(0xFF4CC9F0)
val ColorBurn = Color(0xFFFF9F1C)

private val LightColorScheme = lightColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    secondary = Color(0xFF2D3142),
    tertiary = ColorBurn,
    background = MiBackground,
    surface = MiCardBackground,
    surfaceVariant = Color(0xFFF1F3F5),
    onSurface = MiTextPrimary,
    onSurfaceVariant = MiTextSecondary,
    outlineVariant = Color(0xFFE9ECEF)
)

private val DarkColorScheme = darkColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    secondary = Color(0xFFBFC2C7),
    tertiary = ColorBurn,
    background = Color(0xFF0F1113),
    surface = Color(0xFF1A1C1E),
    surfaceVariant = Color(0xFF2C2F33),
    onSurface = Color(0xFFE2E2E6),
    onSurfaceVariant = Color(0xFFAFB1B6),
    outlineVariant = Color(0xFF3F4448)
)

val MiTypography = Typography(
    displayMedium = TextStyle(
        fontWeight = FontWeight.Black,
        fontSize = 32.sp,
        letterSpacing = (-0.5).sp
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.ExtraBold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        letterSpacing = 0.8.sp
    )
)

@Composable
fun FitBroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = MiTypography,
        content = content
    )
}
