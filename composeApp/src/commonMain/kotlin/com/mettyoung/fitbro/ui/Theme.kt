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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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

// Theme-aware aliases used across screens. `@Composable` getters (not constants): hoist
// them before Canvas/DrawScope lambdas.
val BrandPrimary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val TextSecondary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val TextPrimary: Color
    @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface
val PositiveColor: Color
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.positive
val NegativeColor: Color
    @Composable @ReadOnlyComposable get() = LocalExtendedColors.current.negative

// ---------------------------------------------------------------------------
// Color schemes — ink-blue primary on cool neutrals, so the data colours (macros, burn,
// body metrics) carry the colour. `background` sits one step below `surface` so plain
// surface-coloured cards separate from the page without shadows.
// ---------------------------------------------------------------------------

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF2B4C7E),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD6E3FF),
    onPrimaryContainer = Color(0xFF001B3E),
    secondary = Color(0xFF565E71),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFDAE2F9),
    onSecondaryContainer = Color(0xFF131C2B),
    tertiary = Color(0xFF00897B),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFB2EFE6),
    onTertiaryContainer = Color(0xFF00201C),
    background = Color(0xFFF3F4F7),
    onBackground = Color(0xFF1A1C1F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1C1F),
    surfaceVariant = Color(0xFFE9ECF1),
    onSurfaceVariant = Color(0xFF5A6070),
    surfaceTint = Color(0xFF2B4C7E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF8F9FB),
    surfaceContainer = Color(0xFFFFFFFF),
    surfaceContainerHigh = Color(0xFFEEF0F4),
    surfaceContainerHighest = Color(0xFFE6E9EE),
    outline = Color(0xFF8A909C),
    outlineVariant = Color(0xFFDDE1E8),
    error = Color(0xFFBA1A1A),
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFA9C7FF),
    onPrimary = Color(0xFF0B2F5F),
    primaryContainer = Color(0xFF234678),
    onPrimaryContainer = Color(0xFFD6E3FF),
    secondary = Color(0xFFBEC6DC),
    onSecondary = Color(0xFF283141),
    secondaryContainer = Color(0xFF3E4759),
    onSecondaryContainer = Color(0xFFDAE2F9),
    tertiary = Color(0xFF7FD6C8),
    onTertiary = Color(0xFF003731),
    tertiaryContainer = Color(0xFF005048),
    onTertiaryContainer = Color(0xFFB2EFE6),
    background = Color(0xFF0E1013),
    onBackground = Color(0xFFE3E5EA),
    surface = Color(0xFF191C20),
    onSurface = Color(0xFFE3E5EA),
    surfaceVariant = Color(0xFF262A30),
    onSurfaceVariant = Color(0xFFC2C7D1),
    surfaceTint = Color(0xFFA9C7FF),
    surfaceContainerLowest = Color(0xFF0A0C0F),
    surfaceContainerLow = Color(0xFF15181C),
    surfaceContainer = Color(0xFF191C20),
    surfaceContainerHigh = Color(0xFF23272C),
    surfaceContainerHighest = Color(0xFF2E3238),
    outline = Color(0xFF8C919B),
    outlineVariant = Color(0xFF363A41),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

val FitBroTypography = Typography(
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

/** User's appearance choice (Settings → Appearance). */
enum class ThemeMode(val label: String) { SYSTEM("System"), LIGHT("Light"), DARK("Dark") }

/** True when the app is drawing its dark scheme (platform code syncs status-bar icons to it). */
val LocalIsDarkTheme = staticCompositionLocalOf { false }

@Composable
fun FitBroTheme(content: @Composable () -> Unit) {
    val mode by ThemePreference.mode.collectAsState()
    val darkTheme = when (mode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    CompositionLocalProvider(
        LocalExtendedColors provides if (darkTheme) DarkExtended else LightExtended,
        LocalIsDarkTheme provides darkTheme
    ) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
            typography = FitBroTypography,
            shapes = FitBroShapes,
            content = content
        )
    }
}
