package com.pisheblok.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.pisheblok.app.R

/**
 * Хлебный шрифт для заголовков и цифр — Montserrat (OFL), статические
 * файлы по весам. Вариативный шрифт на части устройств рендерится одним
 * дефолтным весом — поэтому только TTF-пары.
 */
val BlokFontFamily: FontFamily = FontFamily(
    Font(R.font.montserrat_regular, FontWeight.Normal),
    Font(R.font.montserrat_medium, FontWeight.Medium),
    Font(R.font.montserrat_semibold, FontWeight.SemiBold),
    Font(R.font.montserrat_bold, FontWeight.Bold),
    Font(R.font.montserrat_extrabold, FontWeight.ExtraBold),
)

/** Типографическая шкала: крупные стили — Montserrat, тело — дефолт. */
val PisheBlokTypography: Typography = run {
    val base = Typography()
    base.copy(
        displayLarge = base.displayLarge.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        displayMedium = base.displayMedium.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        displaySmall = base.displaySmall.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        headlineLarge = base.headlineLarge.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        headlineMedium = base.headlineMedium.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        headlineSmall = base.headlineSmall.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.Bold),
        titleLarge = base.titleLarge.copy(
            fontFamily = BlokFontFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 21.sp,
        ),
        titleMedium = base.titleMedium.copy(fontFamily = BlokFontFamily, fontWeight = FontWeight.SemiBold),
        bodyLarge = base.bodyLarge.copy(lineHeight = 22.sp),
        bodyMedium = base.bodyMedium.copy(lineHeight = 18.sp),
        labelLarge = base.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        labelMedium = base.labelMedium,
    )
}

/** Табличные цифры: не дёргаются при смене значений (счётчики, количества). */
@Composable
fun blokTabular(style: TextStyle): TextStyle =
    style.copy(
        fontFamily = BlokFontFamily,
        fontFeatureSettings = "tnum",
    )
