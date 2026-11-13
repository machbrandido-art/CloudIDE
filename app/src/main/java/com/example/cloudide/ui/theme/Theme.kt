package com.example.cloudide.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = Ink,
    onPrimary = Cream,
    secondary = Lavender,
    onSecondary = Ink,
    tertiary = Peach,
    onTertiary = Ink,
    background = Cream,
    onBackground = Ink,
    surface = Cream,
    onSurface = Ink,
    surfaceVariant = CreamDark,
    onSurfaceVariant = InkMuted,
    outline = OutlineSoft,
    error = OnChipRed,
    errorContainer = ChipRed,
    onErrorContainer = OnChipRed
)

@Composable
fun CloudIdeTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}