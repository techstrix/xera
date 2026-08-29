package com.phlox.tvwebbrowser.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightScheme = lightColorScheme(
    primary = WebTabSelectedLight,
    onPrimary = DayNightTextContrastDark,
    background = DayNightBackgroundLight,
    onBackground = DayNightTextContrastLight,
    surface = DayNightBackgroundLight,
    onSurface = DayNightTextContrastLight,
    surfaceVariant = ListHeaderLight,
    outline = ListDividerLight,
    secondaryContainer = ButtonBgLight,
    tertiary = ProgressTint
)

private val DarkScheme = darkColorScheme(
    primary = WebTabSelectedDark,
    onPrimary = DayNightTextContrastLight,
    background = DayNightBackgroundDark,
    onBackground = DayNightTextContrastDark,
    surface = DayNightBackgroundDark,
    onSurface = DayNightTextContrastDark,
    surfaceVariant = ListHeaderDark,
    outline = ListDividerDark,
    secondaryContainer = ButtonBgDark,
    tertiary = ProgressTint
)

@Composable
fun XeraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val scheme = if (darkTheme) DarkScheme else LightScheme
    MaterialTheme(
        colorScheme = scheme,
        content = content
    )
}
