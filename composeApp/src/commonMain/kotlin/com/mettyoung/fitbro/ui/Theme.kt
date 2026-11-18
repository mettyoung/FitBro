package com.mettyoung.fitbro.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ---------------------------------------------------------------------------
// Brand + data colors
// ---------------------------------------------------------------------------

// Macro series colors. Fixed across themes so charts read the same in light and dark.
val ColorProtein = Color(0xFF4F6BF0)
val ColorCarbs = Color(0xFFE5487E)
val ColorFat = Color(0xFF2FB4D8)
val ColorCardio = Color(0xFF7ED957)
val ColorWeight = Color(0xFF7F77DD)
val ColorLeanMass = Color(0xFF1D9E75)
val ColorBodyFat = Color(0xFFBA7517)
val ColorBurn = Color(0xFFFF9F1C)

/** Semantic colors Material 3 has no slot for (surplus/deficit, success). */
@Immutable
data class FitBroExtendedColors(
    val positive: Color,
    val onPositiveContainer: Color,
    val positiveContainer: Color,
    val negative: Color,
)

private val LightExtended = FitBroExtendedColors(
    positive = Color(0xFF2E7D32),
    onPositiveContainer = Color(0xFF0B3D10),
    positiveContainer = Color(0xFFCDEFD0),
    negative = Color(0xFFC62828),
)

private val DarkExtended = FitBroExtendedColors(
    positive = Color(0xFF7DD383),
    onPositiveContainer = Color(0xFFCDEFD0),
    positiveContainer = Color(0xFF1F4D23),
    negative = Color(0xFFFF8A80),
)

private val LocalExtendedColors = staticCompositionLocalOf { LightExtended }

object FitBroColors {
    val extended: FitBroExtendedColors
        @Composable @ReadOnlyComposable get() = LocalExtendedColors.current
}

// Theme-aware aliases kept for the many existing call sites. They used to be fixed light-mode
// colors, which is why dark mode looked half-broken.
val MiOrange: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val MiTextSecondary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val MiTextPrimary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val PositiveColor: Color
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.positive
val NegativeColor: Color
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.negative

// ---------------------------------------------------------------------------
// Color schemes — orange-seeded tonal palette. `background` sits one step below `surface`
// so plain surface-colored cards separate from the page without shadows.
// ---------------------------------------------------------------------------

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFFE85D04),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDCC8),
    onPrimaryContainer = Color(0xFF361200),
    secondary = Color(0xFF5E5F6E),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFE6E4F2),
    onSecondaryContainer = Color(0xFF1B1B26),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2EFE6),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF5F2EF),
    onBackground = Color(0xFF1C1B1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1C1B1A),
    surfaceVariant = Color(0xFFF1EDE9),
    onSurfaceVariant = Color(0xFF5F5852),
    surfaceTint = Color(0xFFE85D04),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF8F6),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFF3EFEC),
    surfaceContainerHighest = Color(0xFFECE7E3),
    outline = Color(0xFF8F8780),
    outlineVariant = Color(0xFFE6E0DA),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFF8A3D),
    onPrimary = Color(0xFF3A1400),
    primaryContainer = Color(0xFF6B2D00),
    onPrimaryContainer = Color(0xFFFFDCC8),
    secondary = Color(0xFFC7C5D4),
    onSecondary = Color(0xFF30303C),
    secondaryContainer = Color(0xFF45464F),
    onSecondaryContainer = Color(0xFFE6E4F2),
    tertiary = Color(0xFF7FD6C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFB2EFE6),
    background = Color(0xFF0F0E0D),
    onBackground = Color(0xFFEAE4DF),
    surface = Color(0xFF1C1A19),
    onSurface = Color(0xFFEAE4DF),
    surfaceVariant = Color(0xFF2A2725),
    onSurfaceVariant = Color(0xFFC9C1BA),
    surfaceTint = Color(0xFFFF8A3D),
    surfaceContainerLowest = Color(0xFF0B0A09),
    surfaceContainerLow = Color(0xFF171615),
    surfaceContainer = Color(0xFF1C1A19),
    surfaceContainerHigh = Color(0xFF262422),
    surfaceContainerHighest = Color(0xFF312E2C),
    outline = Color(0xFF948C85),
    outlineVariant = Color(0xFF3A3633),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val MiTypography = Typography(
    displayLarge = TextStyle(fontWeight = FontWeight.Black, fontSize = 48.sp, lineHeight = 52.sp, letterSpacing = (-1).sp),
    displayMedium = TextStyle(fontWeight = FontWeight.Black, fontSize = 32.sp, lineHeight = 38.sp, letterSpacing = (-0.5).sp),
    displaySmall = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.25).sp),
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.25).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 24.sp, lineHeight = 30.sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp, letterSpacing = 0.15.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Medium, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.2.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.3.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp, letterSpacing = 0.4.sp),
    // Used app-wide as an uppercase overline.
    labelSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 11.sp, lineHeight = 16.sp, letterSpacing = 0.8.sp),
)

val FitBroShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp),
)

@Composable
fun FitBroTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    CompositionLocalProvider(LocalExtendedColors provides if (darkTheme) DarkExtended else LightExtended) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = MiTypography,
            shapes = FitBroShapes,
            content = content
        )
    }
}
