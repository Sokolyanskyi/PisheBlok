package com.pisheblok.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp

/**
 * Дизайн-токены PisheBlok: формы, шкала отступов, градиенты.
 * Весь новый UI-код — через эти токены, никаких ручных значений мимо системы.
 */
object Blok {

    /** Формы: крупные радиусы карточек, pill для чипов. */
    val card: Shape = RoundedCornerShape(24.dp)
    val cardSmall: Shape = RoundedCornerShape(16.dp)
    val hero: Shape = RoundedCornerShape(28.dp)
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
        if (isSystemInDarkTheme()) MaterialTheme.colorScheme.error.copy(alpha = 0.22f)
        else MaterialTheme.colorScheme.errorContainer
}
