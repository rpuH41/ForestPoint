package com.liulkovich.florapoint.shared.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Green10,
    secondary = PurpleGrey80,
    tertiary = Pink80
)

private val LightColorScheme = lightColorScheme(
    primary = Green10,
    secondary = PurpleGrey40,
    tertiary = Pink40
)

@Composable
fun ForestPointSharedTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    colorScheme: androidx.compose.material3.ColorScheme? = null,
    content: @Composable () -> Unit
) {
    val selectedColorScheme = colorScheme
        ?: if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = selectedColorScheme,
        typography = ForestPointTypography,
        content = content
    )
}
