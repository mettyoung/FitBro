package com.mettyoung.fitbro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val MiOrange = Color(0xFFFF6700)
val MiBackground = Color(0xFFF6F6F6)
val MiCardBackground = Color(0xFFFFFFFF)
val MiTextPrimary = Color(0xFF000000)
val MiTextSecondary = Color(0xFF888888)

// Macro Colors
val ColorProtein = Color(0xFF5C6BC0)
val ColorCarbs = Color(0xFFFFA726)
val ColorFat = Color(0xFF66BB6A)

private val LightColorScheme = lightColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    background = MiBackground,
    surface = MiCardBackground,
    onSurface = MiTextPrimary,
    onSurfaceVariant = MiTextSecondary
)

private val DarkColorScheme = darkColorScheme(
    primary = MiOrange,
    onPrimary = Color.White,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
    onSurface = Color.White,
    onSurfaceVariant = Color(0xFFB0B0B0)
)

val MiTypography = Typography(
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        letterSpacing = 0.sp
    ),
    bodyLarge = TextStyle(
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        letterSpacing = 0.5.sp
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
