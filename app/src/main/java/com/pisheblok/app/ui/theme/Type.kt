package com.pisheblok.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Default = Typography()

val PisheBlokTypography = Typography(
    displaySmall = Default.displaySmall.copy(fontWeight = FontWeight.Bold),
    headlineMedium = Default.headlineMedium.copy(fontWeight = FontWeight.Bold),
    headlineSmall = Default.headlineSmall.copy(fontWeight = FontWeight.Bold),
    titleLarge = Default.titleLarge.copy(fontWeight = FontWeight.SemiBold, fontSize = 21.sp),
    titleMedium = Default.titleMedium.copy(fontWeight = FontWeight.SemiBold),
    bodyLarge = Default.bodyLarge.copy(lineHeight = 22.sp),
    bodyMedium = Default.bodyMedium.copy(lineHeight = 18.sp),
    labelLarge = Default.labelLarge.copy(fontWeight = FontWeight.SemiBold),
    labelMedium = Default.labelMedium,
)

val TitleFont = TextStyle(fontFamily = FontFamily.Default)
