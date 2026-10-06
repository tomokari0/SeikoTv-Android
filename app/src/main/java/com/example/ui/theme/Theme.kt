package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SeikoRed,
    onPrimary = Color.White,
    primaryContainer = SeikoDarkRed,
    onPrimaryContainer = Color.White,
    secondary = SeikoRedBright,
    onSecondary = Color.White,
    tertiary = ImdbGold,
    background = SeikoBlack,
    onBackground = TextWhite,
    surface = SeikoDarkSurface,
    onSurface = TextWhite,
    surfaceVariant = SeikoSurfaceVariant,
    onSurfaceVariant = TextGray,
    outline = SeikoBorder
)

@Composable
fun SeikoTVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
