package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.AppThemeMode

private val DarkMinimalColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF121212),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFD0D0D0),
    onSecondary = Color(0xFF121212),
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

private val OledBlackColorScheme = darkColorScheme(
    primary = Color(0xFFFFFFFF),
    onPrimary = Color(0xFF000000),
    primaryContainer = Color(0xFF1F1F1F),
    onPrimaryContainer = Color(0xFFFFFFFF),
    secondary = Color(0xFFBDBDBD),
    onSecondary = Color(0xFF000000),
    background = OledBackground,
    onBackground = Color.White,
    surface = OledSurface,
    onSurface = Color.White,
    surfaceVariant = OledSurfaceVariant,
    onSurfaceVariant = Color(0xFF9E9E9E),
    outline = OledOutline
)

private val LightPaperColorScheme = lightColorScheme(
    primary = Color(0xFF111111),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFE5E7EB),
    onPrimaryContainer = Color(0xFF111111),
    secondary = Color(0xFF4B5563),
    onSecondary = Color(0xFFFFFFFF),
    background = LightBackground,
    onBackground = LightTextPrimary,
    surface = LightSurface,
    onSurface = LightTextPrimary,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightTextSecondary,
    outline = LightOutline
)

private val SlateGraphiteColorScheme = darkColorScheme(
    primary = Color(0xFF94A3B8),
    onPrimary = Color(0xFF0F172A),
    primaryContainer = Color(0xFF334155),
    onPrimaryContainer = Color(0xFFF1F5F9),
    secondary = Color(0xFF64748B),
    onSecondary = Color(0xFF0F172A),
    background = SlateBackground,
    onBackground = SlateTextPrimary,
    surface = SlateSurface,
    onSurface = SlateTextPrimary,
    surfaceVariant = SlateSurfaceVariant,
    onSurfaceVariant = Color(0xFF94A3B8),
    outline = SlateOutline
)

private val WarmSepiaColorScheme = darkColorScheme(
    primary = Color(0xFFD6C7B2),
    onPrimary = Color(0xFF1C1917),
    primaryContainer = Color(0xFF38332E),
    onPrimaryContainer = Color(0xFFF5EBE1),
    secondary = Color(0xFFA89F91),
    onSecondary = Color(0xFF1C1917),
    background = SepiaBackground,
    onBackground = SepiaTextPrimary,
    surface = SepiaSurface,
    onSurface = SepiaTextPrimary,
    surfaceVariant = SepiaSurfaceVariant,
    onSurfaceVariant = Color(0xFFA89F91),
    outline = SepiaOutline
)

@Composable
fun SmartKeyboardTheme(
    themeMode: AppThemeMode = AppThemeMode.DARK_MINIMAL,
    content: @Composable () -> Unit
) {
    val colorScheme: ColorScheme = when (themeMode) {
        AppThemeMode.DARK_MINIMAL -> DarkMinimalColorScheme
        AppThemeMode.OLED_BLACK -> OledBlackColorScheme
        AppThemeMode.LIGHT_PAPER -> LightPaperColorScheme
        AppThemeMode.SLATE_GRAPHITE -> SlateGraphiteColorScheme
        AppThemeMode.WARM_SEPIA -> WarmSepiaColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
