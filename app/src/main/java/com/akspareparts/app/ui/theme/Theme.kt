package com.akspareparts.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = OnPrimaryWhite,
    primaryContainer = DeepBlueLight,
    onPrimaryContainer = OnPrimaryWhite,
    secondary = DeepBlueDark,
    onSecondary = OnPrimaryWhite,
    background = SurfaceGrey,
    surface = CardWhite
)

private val DarkColors = darkColorScheme(
    primary = DeepBlueLight,
    onPrimary = OnPrimaryWhite,
    secondary = DeepBlue
)

@Composable
fun AKSparepartsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        content = content
    )
}
