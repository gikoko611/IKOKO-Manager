package com.gikoko.ikokomanager.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Palette matched to the GI Manager branding: dark tech blue + cyan.
private val Cyan = Color(0xFF00C9C8)
private val IceBlue = Color(0xFF9FB4C8)

private val DarkColors = darkColorScheme(
    primary = Cyan,
    onPrimary = Color(0xFF06222B),
    primaryContainer = Color(0xFF123B5C),
    secondary = IceBlue,
    background = Color(0xFF081426),
    surface = Color(0xFF0C1B33),
    surfaceVariant = Color(0xFF15263F)
)

private val LightColors = lightColorScheme(
    primary = Color(0xFF007E85),
    secondary = Color(0xFF0E9AA7)
)

@Composable
fun IkokoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (isSystemInDarkTheme()) DarkColors else LightColors,
        content = content
    )
}
