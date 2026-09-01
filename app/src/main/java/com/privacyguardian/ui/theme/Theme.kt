package com.privacyguardian.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = Safe,
    onPrimary = Color.White,
    primaryContainer = SafeLight,
    onPrimaryContainer = TextPrimary,
    secondary = AccentIqoo,
    onSecondary = Color.White,
    tertiary = AccentPurple,
    onTertiary = Color.White,
    error = Critical,
    onError = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = CardElevated,
    onSurfaceVariant = TextSecondary,
    outline = Border,
    outlineVariant = BorderStrong,
    scrim = Color(0x990F172A)
)

@Composable
fun PrivacyGuardianTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    // Force white professional theme for demo — darkTheme param ignored for flagship light look
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = Typography,
        content = content
    )
}
