package com.pisheblok.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Дизайн-токены PisheBlok: формы, шкала отступов, градиенты.
 * Весь новый UI-код — через эти токены, никаких ручных значений мимо системы.
 */
object Blok {

    /** Формы: крупные радиусы карточек (как в референсе), pill для чипов. */
    val card: Shape = RoundedCornerShape(28.dp)
    val cardSmall: Shape = RoundedCornerShape(18.dp)
    val hero: Shape = RoundedCornerShape(32.dp)
    val pill: Shape = RoundedCornerShape(50)

    /** Шкала отступов — вместо разрозненных inline-значений. */
    object Space {
        val xs = 4.dp
        val s = 8.dp
        val m = 12.dp
        val l = 16.dp
        val xl = 20.dp
        val xxl = 24.dp
    }

    /** Градиентная подложка авторизационного экрана / hero-блоков. */
    @Composable
    fun heroBrush(): Brush = Brush.verticalGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
            MaterialTheme.colorScheme.background,
        )
    )

    /** Мягкая градиентная подложка карточек-«табло». */
    @Composable
    fun statBrush(): Brush = Brush.linearGradient(
        listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = 0.16f),
            MaterialTheme.colorScheme.primary.copy(alpha = 0.04f),
        )
    )

    /** Подложка деструктивных зон (hold-кнопки, бейджи протухшего). */
    @Composable
    fun dangerContainer(): Color =
        if (LocalDarkTheme.current) MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.errorContainer
}

/**
 * Цвета главного действия. В светлом «Неон-лайм» CTA красится лаймовым
 * контейнером с тёмным текстом: сам primary в свете — оливковый, и лайм
 * остаётся только в сводке. Остальные палитры — обычная пара primary.
 */
@Immutable
data class CtaColors(val container: Color, val content: Color)

@Composable
fun ctaColors(): CtaColors {
    val scheme = LocalSchemeState.current.value
    val colors = MaterialTheme.colorScheme
    return if (scheme == ThemeScheme.NEON && !LocalDarkTheme.current) {
        CtaColors(colors.primaryContainer, colors.onPrimaryContainer)
    } else {
        CtaColors(colors.primary, colors.onPrimary)
    }
}

/**
 * Одинаковая подложка для всех карточек: мягкий оттенок палитры у верхнего
 * края, ровный containerColor книзу. Без чередования — сетка выглядит
 * цельно, тема остаётся узнаваемой (лайм в неоне, лаванда в лаванде).
 */
@Composable
fun blokTintBrush(): Brush = Brush.linearGradient(
    listOf(
        if (LocalDarkTheme.current) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
        } else {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        },
        MaterialTheme.colorScheme.surfaceContainerLowest,
    ),
)

/**
 * Мягкая тень карточки из primary палитры: на светлом фоне карточка
 * отрывается от подложки, в тёмной теме тень выключена — читаемость
 * даёт рамка и светлая заливка.
 */
@Composable
fun Modifier.blokCardShadow(shape: Shape = Blok.card): Modifier {
    val dark = LocalDarkTheme.current
    val tint = MaterialTheme.colorScheme.primary
    return this.shadow(
        elevation = if (dark) 0.dp else 6.dp,
        shape = shape,
        ambientColor = if (dark) Color.Transparent else tint.copy(alpha = 0.20f),
        spotColor = if (dark) Color.Transparent else tint.copy(alpha = 0.30f),
    )
}
