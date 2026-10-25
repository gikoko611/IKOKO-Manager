package com.gikoko.ikokomanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val Gold = Color(0xFFF5C518)
private val DeepGold = Color(0xFFD4AF37)

private val DarkColors = darkColorScheme(
    primary = Gold,
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF2A2410),
    secondary = DeepGold,
    background = Color(0xFF0E0E0D),
    surface = Color(0xFF151412),
    surfaceVariant = Color(0xFF201E1A)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF9A7B0A),
    secondary = DeepGold
)

@Composable
fun IkokoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
