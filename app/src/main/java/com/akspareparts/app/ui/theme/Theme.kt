package com.akspareparts.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

private val LightColors = lightColorScheme(
    primary = DeepBlue,
    onPrimary = OnPrimaryWhite,
    primaryContainer = BlueContainer,
    onPrimaryContainer = OnBlueContainer,
    secondary = DeepBlueDark,
    onSecondary = OnPrimaryWhite,
    secondaryContainer = BlueContainer,
    onSecondaryContainer = OnBlueContainer,
    tertiary = AccentAmberDark,
    onTertiary = OnPrimaryWhite,
    tertiaryContainer = AmberContainer,
    onTertiaryContainer = AccentAmberDark,
    background = SurfaceGrey,
    onBackground = TextPrimary,
    surface = CardWhite,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = TextSecondary,
    outline = OutlineLight,
    error = ErrorRed,
    errorContainer = ErrorContainerLight
)

private val DarkColors = darkColorScheme(
    primary = DeepBlueLight,
    onPrimary = DeepBlueDarker,
    primaryContainer = DeepBlueDark,
    onPrimaryContainer = BlueContainer,
    secondary = DeepBlueLight,
    onSecondary = DeepBlueDarker,
    secondaryContainer = DeepBlueDark,
    onSecondaryContainer = BlueContainer,
    tertiary = AccentAmber,
    onTertiary = AccentAmberDark,
    background = DarkBackground,
    onBackground = DarkTextPrimary,
    surface = DarkSurface,
    onSurface = DarkTextPrimary,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkTextSecondary,
    outline = DarkOutline
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AKSparepartsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
