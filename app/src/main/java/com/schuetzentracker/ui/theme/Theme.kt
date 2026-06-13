package com.schuetzentracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// ────────────────────────────────────────────────
// SCHÜTZENTRACKER THEME
// Waldgrün + Gold – klassische Schützensport-Ästhetik
// ────────────────────────────────────────────────

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF4CAF50),
    onPrimary = Color(0xFF003300),
    primaryContainer = Color(0xFF1B5E20),
    onPrimaryContainer = Color(0xFFB9F6CA),
    secondary = Color(0xFFFFB300),
    onSecondary = Color(0xFF3E2000),
    secondaryContainer = Color(0xFF5C3800),
    onSecondaryContainer = Color(0xFFFFDDB3),
    tertiary = Color(0xFF81C784),
    onTertiary = Color(0xFF00210E),
    error = Color(0xFFEF5350),
    background = Color(0xFF0D1F0D),
    onBackground = Color(0xFFE8F5E9),
    surface = Color(0xFF1A2E1A),
    onSurface = Color(0xFFDCEFDC),
    surfaceVariant = Color(0xFF1E3820),
    onSurfaceVariant = Color(0xFFB0C4B0)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF1B5E20),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFB9F6CA),
    onPrimaryContainer = Color(0xFF002204),
    secondary = Color(0xFFF57F17),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFFFECB3),
    onSecondaryContainer = Color(0xFF3E2000),
    tertiary = Color(0xFF2E7D32),
    onTertiary = Color.White,
    error = Color(0xFFB00020),
    background = Color(0xFFF5FBF5),
    onBackground = Color(0xFF1A2E1A),
    surface = Color.White,
    onSurface = Color(0xFF1A2E1A),
    surfaceVariant = Color(0xFFE8F5E9),
    onSurfaceVariant = Color(0xFF3D5E42)
)

@Composable
fun SchutzenTrackerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}
