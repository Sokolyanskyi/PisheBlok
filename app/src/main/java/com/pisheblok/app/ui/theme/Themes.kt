package com.pisheblok.app.ui.theme

import android.content.Context
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** Available color schemes; NEON is the default hand-crafted «lime on graphite» palette. */
enum class ThemeScheme { NEON, TERRACOTTA, EMERALD, OCEAN, LAVANDER, STRAWBERRY, SKY, LIME, INDIGO, COFFEE, SLATE }

/** Режим светлоты: следовать системе или принудительно светлая/тёмная. */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

// Тёплые базы «свежей кухни»: вместо чистых бело-чёрных нейтралей все
// производные палитры лерпятся к крему/тёплым чернилам — интерьер общий.
private val White = Color(0xFFFFFFFF)
private val Black = Color(0xFF000000)
private val Cream = Color(0xFFFAF6EF)
private val CreamWhite = Color(0xFFFDFBF4)
private val Ink = Color(0xFF2B2A22)
private val WarmBlack = Color(0xFF171B12)
private val DarkCream = Color(0xFFEAE6D8)

private fun Color.lerpTo(target: Color, fraction: Float): Color = Color(
    red + (target.red - red) * fraction,
    green + (target.green - green) * fraction,
    blue + (target.blue - blue) * fraction,
    1f,
)

private data class Seeds(val primary: Color, val secondary: Color, val tertiary: Color)

private fun ThemeScheme.seeds(): Seeds = when (this) {
    ThemeScheme.NEON -> Seeds(Color(0xFFC8F044), Color(0xFFFFD44F), Color(0xFFFFB68C))
    ThemeScheme.TERRACOTTA -> Seeds(Color(0xFF4C6B48), Color(0xFF55794A), Color(0xFFA65432))
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
    primaryContainer = p.lerpTo(CreamWhite, 0.82f),
    onPrimaryContainer = p.lerpTo(Ink, 0.72f),
    secondary = s,
    onSecondary = White,
    secondaryContainer = s.lerpTo(CreamWhite, 0.80f),
    onSecondaryContainer = s.lerpTo(Ink, 0.68f),
    tertiary = t,
    onTertiary = White,
    tertiaryContainer = t.lerpTo(CreamWhite, 0.80f),
    onTertiaryContainer = t.lerpTo(Ink, 0.68f),
    error = Color(0xFFBA1A1A),
    onError = White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
    background = p.lerpTo(Cream, 0.94f),
    onBackground = p.lerpTo(Ink, 0.86f),
    surface = p.lerpTo(Cream, 0.94f),
    onSurface = p.lerpTo(Ink, 0.86f),
    surfaceVariant = p.lerpTo(CreamWhite, 0.86f),
    onSurfaceVariant = p.lerpTo(Ink, 0.52f),
    outline = p.lerpTo(Ink, 0.48f),
    outlineVariant = p.lerpTo(Ink, 0.20f),
    inverseSurface = p.lerpTo(Ink, 0.86f),
    inverseOnSurface = p.lerpTo(CreamWhite, 0.95f),
    scrim = Black,
    surfaceContainerLowest = White,
    surfaceContainerLow = p.lerpTo(CreamWhite, 0.92f),
    surfaceContainer = p.lerpTo(Cream, 0.89f),
    surfaceContainerHigh = p.lerpTo(Cream, 0.86f),
    surfaceContainerHighest = p.lerpTo(Cream, 0.82f),
)

/** Derives a dark Material scheme from scheme seeds. */
private fun deriveDark(p: Color, s: Color, t: Color): ColorScheme = darkColorScheme(
    primary = p.lerpTo(DarkCream, 0.62f),
    onPrimary = p.lerpTo(WarmBlack, 0.78f),
    primaryContainer = p.lerpTo(WarmBlack, 0.72f),
    onPrimaryContainer = p.lerpTo(DarkCream, 0.82f),
    secondary = s.lerpTo(DarkCream, 0.62f),
    onSecondary = s.lerpTo(WarmBlack, 0.72f),
    secondaryContainer = s.lerpTo(WarmBlack, 0.68f),
    onSecondaryContainer = s.lerpTo(DarkCream, 0.80f),
    tertiary = t.lerpTo(DarkCream, 0.62f),
    onTertiary = t.lerpTo(WarmBlack, 0.72f),
    tertiaryContainer = t.lerpTo(WarmBlack, 0.68f),
    onTertiaryContainer = t.lerpTo(DarkCream, 0.82f),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
    background = p.lerpTo(WarmBlack, 0.90f),
    onBackground = p.lerpTo(DarkCream, 0.88f),
    surface = p.lerpTo(WarmBlack, 0.90f),
    onSurface = p.lerpTo(DarkCream, 0.88f),
    surfaceVariant = p.lerpTo(WarmBlack, 0.70f),
    onSurfaceVariant = p.lerpTo(DarkCream, 0.72f),
    outline = p.lerpTo(DarkCream, 0.52f),
    outlineVariant = p.lerpTo(WarmBlack, 0.55f),
    inverseSurface = p.lerpTo(DarkCream, 0.88f),
    inverseOnSurface = p.lerpTo(WarmBlack, 0.86f),
    scrim = Black,
    surfaceContainerLowest = p.lerpTo(WarmBlack, 0.93f),
    surfaceContainerLow = p.lerpTo(WarmBlack, 0.85f),
    surfaceContainer = p.lerpTo(WarmBlack, 0.82f),
    surfaceContainerHigh = p.lerpTo(WarmBlack, 0.76f),
    surfaceContainerHighest = p.lerpTo(WarmBlack, 0.70f),
)

fun ThemeScheme.lightColors(): ColorScheme = when (this) {
    ThemeScheme.NEON -> NeonLightColors
    ThemeScheme.TERRACOTTA -> LightColors
    else -> seeds().let { deriveLight(it.primary, it.secondary, it.tertiary) }
}

fun ThemeScheme.darkColors(): ColorScheme = when (this) {
    ThemeScheme.NEON -> NeonDarkColors
    ThemeScheme.TERRACOTTA -> DarkColors
    else -> seeds().let { deriveDark(it.primary, it.secondary, it.tertiary) }
}

/** Preview dots for the picker: primary / secondary / tertiary seed colors. */
fun ThemeScheme.previewColors(): List<Color> = seeds().let { listOf(it.primary, it.secondary, it.tertiary) }

/** Live theme selection shared with the settings dialog. */
val LocalSchemeState = staticCompositionLocalOf<MutableState<ThemeScheme>> {
    error("SchemeState not provided")
}

/** Live light/dark mode selection shared with the settings dialog. */
val LocalThemeMode = staticCompositionLocalOf<MutableState<ThemeMode>> {
    error("ThemeModeState not provided")
}

object ThemePrefs {
    private const val PREFS = "pisheblok_settings"
    private const val KEY = "theme_scheme"
    private const val MODE_KEY = "theme_mode"

    fun load(context: Context): ThemeScheme {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, null) ?: return ThemeScheme.NEON
        return runCatching { ThemeScheme.valueOf(name) }.getOrDefault(ThemeScheme.NEON)
    }

    fun save(context: Context, scheme: ThemeScheme) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, scheme.name).apply()
    }

    fun loadMode(context: Context): ThemeMode {
        val name = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(MODE_KEY, null) ?: return ThemeMode.SYSTEM
        return runCatching { ThemeMode.valueOf(name) }.getOrDefault(ThemeMode.SYSTEM)
    }

    fun saveMode(context: Context, mode: ThemeMode) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(MODE_KEY, mode.name).apply()
    }
}
