package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// ─── NagarNetra Material 3 Color Schemes ─────────────────────────────────────
// Dynamic color is intentionally disabled — we own the civic palette.
// This ensures consistent branding regardless of the user's wallpaper.
// ─────────────────────────────────────────────────────────────────────────────

private val LightColorScheme = lightColorScheme(
    primary              = CivicNavy,
    onPrimary            = OnCivicNavy,
    primaryContainer     = CivicNavyContainer,
    onPrimaryContainer   = OnCivicNavyContainer,

    secondary            = SignalTeal,
    onSecondary          = OnSignalTeal,
    secondaryContainer   = SignalTealContainer,
    onSecondaryContainer = OnSignalTealContainer,

    tertiary             = AlertAmber,
    onTertiary           = OnAlertAmber,
    tertiaryContainer    = AlertAmberContainer,
    onTertiaryContainer  = OnAlertAmberContainer,

    error                = DangerRed,
    onError              = OnDangerRed,
    errorContainer       = DangerRedContainer,
    onErrorContainer     = OnDangerRedContainer,

    background           = Background,
    onBackground         = OnBackground,
    surface              = Surface,
    onSurface            = OnSurface,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = OnSurfaceVariant,
    outline              = Outline,
    outlineVariant       = OutlineVariant,
)

private val DarkColorScheme = darkColorScheme(
    primary              = CivicNavyDark,
    onPrimary            = OnCivicNavyDark,
    primaryContainer     = CivicNavyContainerDark,
    onPrimaryContainer   = OnCivicNavyContainerDark,

    secondary            = SignalTealDark,
    onSecondary          = OnSignalTealDark,
    secondaryContainer   = SignalTealContainerDark,
    onSecondaryContainer = OnSignalTealContainerDark,

    tertiary             = AlertAmberDark,
    onTertiary           = OnAlertAmberDark,
    tertiaryContainer    = AlertAmberContainerDark,
    onTertiaryContainer  = OnAlertAmberContainerDark,

    error                = DangerRedDark,
    errorContainer       = DangerRedContainerDark,

    background           = BackgroundDark,
    onBackground         = OnBackgroundDark,
    surface              = SurfaceDark,
    onSurface            = OnSurfaceDark,
    surfaceVariant       = SurfaceVariantDark,
    onSurfaceVariant     = OnSurfaceVariantDark,
    outline              = OutlineDark,
    outlineVariant       = OutlineVariantDark,
)

/**
 * NagarNetra application theme.
 *
 * Provides the full M3 color scheme, type scale, and shapes.
 * Dynamic color (Material You) is intentionally OFF — civic branding must
 * be consistent across all devices regardless of wallpaper choices.
 */
@Composable
fun NagarNetraAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography  = NagarNetraTypography,
        content     = content,
    )
}
