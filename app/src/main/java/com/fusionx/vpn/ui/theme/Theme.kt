package com.fusionx.vpn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val FusionXDarkColorScheme = darkColorScheme(
    primary = CyanPrimary,
    onPrimary = DarkBackground,
    primaryContainer = CyanDark,
    onPrimaryContainer = CyanLight,
    secondary = PurplePrimary,
    onSecondary = DarkBackground,
    secondaryContainer = PurpleDark,
    onSecondaryContainer = PurpleLight,
    tertiary = GreenConnected,
    background = DarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurface,
    onSurface = TextPrimary,
    surfaceVariant = DarkCard,
    onSurfaceVariant = TextSecondary,
    error = RedDisconnected,
    onError = DarkBackground,
    outline = TextDisabled,
    outlineVariant = DarkCardElevated
)

@Composable
fun FusionXTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FusionXDarkColorScheme,
        typography = FusionTypography,
        content = content
    )
}
