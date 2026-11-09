package com.pisheblok.app.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Available color schemes; TERRACOTTA is the original hand-crafted palette. */
enum class ThemeScheme { TERRACOTTA, EMERALD, OCEAN, LAVANDER, STRAWBERRY, SKY, LIME, INDIGO, COFFEE, SLATE }

private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)

private fun Color.lerpTo(target: Color, fraction: Float): Color = Color(
    red + (target.red - red) * fraction,
    green + (target.green - green) * fraction,
    blue + (target.blue - blue) * fraction,
    1f,
)

private data class Seeds(val primary: Color, val secondary: Color, val tertiary: Color)

private fun ThemeScheme.seeds(): Seeds = when (this) {
    ThemeScheme.TERRACOTTA -> Seeds(Color(0xFFBF3F15), Color(0xFF4F6440), Color(0xFF755A2F))
    ThemeScheme.EMERALD -> Seeds(Color(0xFF147B56), Color(0xFF3E6B4F), Color(0xFF8A5A2B))
    ThemeScheme.OCEAN -> Seeds(Color(0xFF1565C0), Color(0xFF2E6D62), Color(0xFF7059A6))
    ThemeScheme.LAVANDER -> Seeds(Color(0xFF6A4FA3), Color(0xFF4E6455), Color(0xFFA35A6B))
    ThemeScheme.STRAWBERRY -> Seeds(Color(0xFFC2185B), Color(0xFF5B5E6B), Color(0xFF8D6A3F))
    ThemeScheme.SKY -> Seeds(Color(0xFF007C91), Color(0xFF4E5D6B), Color(0xFF7B5FA6))
    ThemeScheme.LIME -> Seeds(Color(0xFF5B8C00), Color(0xFF2F6B4F), Color(0xFF8C6D1F))
    ThemeScheme.INDIGO -> Seeds(Color(0xFF3F51B5), Color(0xFF33635C), Color(0xFF904890))
    ThemeScheme.COFFEE -> Seeds(Color(0xFF795548), Color(0xFF5D6B3F), Color(0xFFA6752F))
    ThemeScheme.SLATE -> Seeds(Color(0xFF455A64), Color(0xFF4E6455), Color(0xFF6B5B95))
}

/** Derives a light Material scheme from scheme seeds (harmonized neutrals). */
private fun deriveLight(p: Color, s: Color, t: Color): ColorScheme = lightColorScheme(
    primary = p,
    onPrimary = White,
    primaryContainer = p.lerpTo(White, 0.82f),
    onPrimaryContainer = p.lerpTo(Black, 0.72f),
    secondary = s,
    onSecondary = White,
    secondaryContainer = s.lerpTo(White, 0.80f),
    onSecondaryContainer = s.lerpTo(Black, 0.68f),
    tertiary = t,
    onTertiary = White,
    tertiaryContainer = t.lerpTo(White, 0.80f),
    onTertiaryContainer = t.lerpTo(Black, 0.68f),
    error = Color(0xFFBA1A1A),
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = p.lerpTo(White, 0.94f),
    onBackground = p.lerpTo(Black, 0.86f),
    surface = p.lerpTo(White, 0.94f),
    onSurface = p.lerpTo(Black, 0.86f),
    surfaceVariant = p.lerpTo(White, 0.86f),
    onSurfaceVariant = p.lerpTo(Black, 0.52f),
    outline = p.lerpTo(Black, 0.48f),
    outlineVariant = p.lerpTo(Black, 0.20f),
    inverseSurface = p.lerpTo(Black, 0.86f),
    inverseOnSurface = p.lerpTo(White, 0.95f),
    scrim = Black,
    surfaceContainerLowest = White,
    surfaceContainerLow = p.lerpTo(White, 0.92f),
    surfaceContainer = p.lerpTo(White, 0.89f),
    surfaceContainerHigh = p.lerpTo(White, 0.86f),
    surfaceContainerHighest = p.lerpTo(White, 0.82f),
)

/** Derives a dark Material scheme from scheme seeds. */
private fun deriveDark(p: Color, s: Color, t: Color): ColorScheme = darkColorScheme(
    primary = p.lerpTo(White, 0.62f),
    onPrimary = p.lerpTo(Black, 0.78f),
    primaryContainer = p.lerpTo(Black, 0.72f),
    onPrimaryContainer = p.lerpTo(White, 0.82f),
    secondary = s.lerpTo(White, 0.62f),
    onSecondary = s.lerpTo(Black, 0.72f),
    secondaryContainer = s.lerpTo(Black, 0.68f),
    onSecondaryContainer = s.lerpTo(White, 0.80f),
    tertiary = t.lerpTo(White, 0.62f),
    onTertiary = t.lerpTo(Black, 0.72f),
    tertiaryContainer = t.lerpTo(Black, 0.68f),
    onTertiaryContainer = t.lerpTo(White, 0.80f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = p.lerpTo(Black, 0.90f),
    onBackground = p.lerpTo(White, 0.88f),
    surface = p.lerpTo(Black, 0.90f),
    onSurface = p.lerpTo(White, 0.88f),
    surfaceVariant = p.lerpTo(Black, 0.70f),
    onSurfaceVariant = p.lerpTo(White, 0.72f),
    outline = p.lerpTo(White, 0.52f),
    outlineVariant = p.lerpTo(Black, 0.55f),
    inverseSurface = p.lerpTo(White, 0.88f),
    inverseOnSurface = p.lerpTo(Black, 0.86f),
    scrim = Black,
    surfaceContainerLowest = p.lerpTo(Black, 0.93f),
    surfaceContainerLow = p.lerpTo(Black, 0.85f),
    surfaceContainer = p.lerpTo(Black, 0.82f),
    surfaceContainerHigh = p.lerpTo(Black, 0.76f),
    surfaceContainerHighest = p.lerpTo(Black, 0.70f),
)

fun ThemeScheme.lightColors(): ColorScheme = when (this) {
    ThemeScheme.TERRACOTTA -> LightColors
    else -> seeds().let { deriveLight(it.primary, it.secondary, it.tertiary) }
}

fun ThemeScheme.darkColors(): ColorScheme = when (this) {
    ThemeScheme.TERRACOTTA -> DarkColors
    else -> seeds().let { deriveDark(it.primary, it.secondary, it.tertiary) }
}

/** Preview dots for the picker: primary / secondary / tertiary seed colors. */
fun ThemeScheme.previewColors(): List<Color> = seeds().let { listOf(it.primary, it.secondary, it.tertiary) }

/** Live theme selection shared with the settings dialog. */
val LocalSchemeState = staticCompositionLocalOf<MutableState<ThemeScheme>> {
    error("SchemeState not provided")
}

object ThemePrefs {
    private const val PREFS = "pisheblok_settings"
    private const val KEY = "theme_scheme"

    fun load(context: Context): ThemeScheme {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return ThemeScheme.TERRACOTTA
        return runCatching { ThemeScheme.valueOf(name) }.getOrDefault(ThemeScheme.TERRACOTTA)
    }

    fun save(context: Context, scheme: ThemeScheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, scheme.name).apply()
    }
}
