package com.pisheblok.app.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val LightColors = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    scrim = ScrimLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
)

val DarkColors = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    scrim = ScrimDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
)

/**
 * Мягкое перетекание цветов при смене темы: MaterialTheme получает схему,
 * у которой каждый цвет анимируется к целевому значению. Без этого смена
 * палитры выглядит как мгновенная вспышка.
 */
@Composable
private fun animatedScheme(target: ColorScheme): ColorScheme {
    @Composable
    fun c(get: ColorScheme.() -> Color) = animateColorAsState(target.get(), label = "themeColor").value
    return target.copy(
        primary = c { primary },
        onPrimary = c { onPrimary },
        primaryContainer = c { primaryContainer },
        onPrimaryContainer = c { onPrimaryContainer },
        secondary = c { secondary },
        onSecondary = c { onSecondary },
        secondaryContainer = c { secondaryContainer },
        onSecondaryContainer = c { onSecondaryContainer },
        tertiary = c { tertiary },
        onTertiary = c { onTertiary },
        tertiaryContainer = c { tertiaryContainer },
        onTertiaryContainer = c { onTertiaryContainer },
        background = c { background },
        onBackground = c { onBackground },
        surface = c { surface },
        onSurface = c { onSurface },
        surfaceVariant = c { surfaceVariant },
        onSurfaceVariant = c { onSurfaceVariant },
        surfaceContainerLowest = c { surfaceContainerLowest },
        surfaceContainerLow = c { surfaceContainerLow },
        surfaceContainer = c { surfaceContainer },
        surfaceContainerHigh = c { surfaceContainerHigh },
        surfaceContainerHighest = c { surfaceContainerHighest },
        outline = c { outline },
        outlineVariant = c { outlineVariant },
        inverseSurface = c { inverseSurface },
        inverseOnSurface = c { inverseOnSurface },
        error = c { error },
        onError = c { onError },
        errorContainer = c { errorContainer },
        onErrorContainer = c { onErrorContainer },
    )
}

@Composable
fun PisheBlokTheme(
    scheme: ThemeScheme = ThemeScheme.TERRACOTTA,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val target = if (darkTheme) scheme.darkColors() else scheme.lightColors()
    MaterialTheme(
        colorScheme = animatedScheme(target),
        typography = PisheBlokTypography,
        shapes = PisheBlokShapes,
        content = content,
    )
}
