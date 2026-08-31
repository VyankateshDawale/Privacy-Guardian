package com.privacyguardian.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Safe,
    onPrimary = Color.Black,
    primaryContainer = CardElevated,
    onPrimaryContainer = TextPrimary,
    secondary = TextSecondary,
    tertiary = Warning,
    error = Critical,
    background = Background,
    onBackground = TextPrimary,
    surface = Background,
    onSurface = TextPrimary,
    surfaceVariant = Card,
    onSurfaceVariant = TextSecondary,
    outline = Border
)

private val LightColorScheme = lightColorScheme(
    primary = Safe,
    onPrimary = Color.White,
    background = Background,
    onBackground = TextPrimary,
    surface = Card,
    onSurface = TextPrimary,
)

@Composable
fun PrivacyGuardianTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
