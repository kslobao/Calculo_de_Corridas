package com.calculocorridas.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary          = PrimaryLight,
    onPrimary        = OnBackground,
    secondary        = Secondary,
    background       = Background,
    surface          = Surface,
    surfaceVariant   = SurfaceVar,
    onBackground     = OnBackground,
    onSurface        = OnSurface
)

@Composable
fun CalculoCorridasTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = AppTypography,
        content     = content
    )
}
