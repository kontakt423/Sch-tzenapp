package com.schuetzentracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ── FARBEN ───────────────────────────────────────────────────────────
// Modern: Lebendiges Grün + Bernstein-Akzent

private val DarkColorScheme = darkColorScheme(
    primary              = Color(0xFF4ADE80),   // green-400
    onPrimary            = Color(0xFF052E16),
    primaryContainer     = Color(0xFF14532D),
    onPrimaryContainer   = Color(0xFFBBF7D0),
    secondary            = Color(0xFFFBBF24),   // amber-400
    onSecondary          = Color(0xFF3B2500),
    secondaryContainer   = Color(0xFF4D3500),
    onSecondaryContainer = Color(0xFFFDE68A),
    tertiary             = Color(0xFF34D399),   // emerald-400
    onTertiary           = Color(0xFF022C22),
    tertiaryContainer    = Color(0xFF064E3B),
    onTertiaryContainer  = Color(0xFFA7F3D0),
    error                = Color(0xFFF87171),
    onError              = Color(0xFF450A0A),
    errorContainer       = Color(0xFF7F1D1D),
    onErrorContainer     = Color(0xFFFECACA),
    background           = Color(0xFF090E09),   // tiefes dunkelgrün-schwarz
    onBackground         = Color(0xFFDEEDDE),
    surface              = Color(0xFF111711),
    onSurface            = Color(0xFFD8ECD8),
    surfaceVariant       = Color(0xFF1C2A1C),
    onSurfaceVariant     = Color(0xFF8BAF8D),
    outline              = Color(0xFF365E3A),
    outlineVariant       = Color(0xFF213823),
    inverseSurface       = Color(0xFFD8ECD8),
    inverseOnSurface     = Color(0xFF111711),
    inversePrimary       = Color(0xFF166534)
)

private val LightColorScheme = lightColorScheme(
    primary              = Color(0xFF15803D),   // green-700
    onPrimary            = Color.White,
    primaryContainer     = Color(0xFFDCFCE7),
    onPrimaryContainer   = Color(0xFF052E16),
    secondary            = Color(0xFFB45309),   // amber-700
    onSecondary          = Color.White,
    secondaryContainer   = Color(0xFFFEF9C3),
    onSecondaryContainer = Color(0xFF3B1F00),
    tertiary             = Color(0xFF047857),   // emerald-700
    onTertiary           = Color.White,
    tertiaryContainer    = Color(0xFFD1FAE5),
    onTertiaryContainer  = Color(0xFF022C22),
    error                = Color(0xFFDC2626),
    onError              = Color.White,
    errorContainer       = Color(0xFFFEE2E2),
    onErrorContainer     = Color(0xFF7F1D1D),
    background           = Color(0xFFF4FBF4),
    onBackground         = Color(0xFF0C1A0E),
    surface              = Color.White,
    onSurface            = Color(0xFF0F1F11),
    surfaceVariant       = Color(0xFFEDF7ED),
    onSurfaceVariant     = Color(0xFF3A5C3D),
    outline              = Color(0xFF6B9E70),
    outlineVariant       = Color(0xFFC2DFC5),
    inverseSurface       = Color(0xFF1E3B20),
    inverseOnSurface     = Color(0xFFF4FBF4),
    inversePrimary       = Color(0xFF4ADE80)
)

// ── FORMEN ───────────────────────────────────────────────────────────
// Konsequent abgerundete Ecken für ein modernes Erscheinungsbild

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(6.dp),
    small      = RoundedCornerShape(10.dp),
    medium     = RoundedCornerShape(16.dp),
    large      = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

// ── TYPOGRAFIE ────────────────────────────────────────────────────────
// Klare Hierarchie: fette Headlines, leichte Body-Texte

private val AppTypography = Typography(
    displayLarge   = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 57.sp,  lineHeight = 64.sp, letterSpacing = (-0.25).sp),
    displayMedium  = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 45.sp,  lineHeight = 52.sp),
    headlineLarge  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 32.sp,  lineHeight = 40.sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 28.sp,  lineHeight = 36.sp),
    headlineSmall  = TextStyle(fontWeight = FontWeight.Bold,      fontSize = 24.sp,  lineHeight = 32.sp),
    titleLarge     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 22.sp,  lineHeight = 28.sp),
    titleMedium    = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 16.sp,  lineHeight = 24.sp, letterSpacing = 0.1.sp),
    titleSmall     = TextStyle(fontWeight = FontWeight.SemiBold,  fontSize = 14.sp,  lineHeight = 20.sp, letterSpacing = 0.1.sp),
    bodyLarge      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 16.sp,  lineHeight = 24.sp, letterSpacing = 0.5.sp),
    bodyMedium     = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 14.sp,  lineHeight = 20.sp, letterSpacing = 0.25.sp),
    bodySmall      = TextStyle(fontWeight = FontWeight.Normal,    fontSize = 12.sp,  lineHeight = 16.sp, letterSpacing = 0.4.sp),
    labelLarge     = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 14.sp,  lineHeight = 20.sp, letterSpacing = 0.1.sp),
    labelMedium    = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 12.sp,  lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelSmall     = TextStyle(fontWeight = FontWeight.Medium,    fontSize = 11.sp,  lineHeight = 16.sp, letterSpacing = 0.5.sp)
)

// ── THEME ─────────────────────────────────────────────────────────────

@Composable
fun SchutzenTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        shapes      = AppShapes,
        typography  = AppTypography,
        content     = content
    )
}
