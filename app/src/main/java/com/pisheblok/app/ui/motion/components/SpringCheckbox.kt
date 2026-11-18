package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.LocalMotionEnabled
import com.pisheblok.app.ui.motion.motionSpring
import com.pisheblok.app.ui.motion.pressScale
import com.pisheblok.app.ui.motion.rememberPressSource
import kotlinx.coroutines.launch

/**
 * Пружинный чекбокс (порт духа Motion-KIT): заливка-протяжка слева,
 * галочка прорисовывается по PathMeasure (точки 200/0.60), при установке
 * — желе-поп (приседание + отскок 320/0.50). Все значения читаются
 * ВНУТРИ draw/graphicsLayer — рекомпозиции на кадр нет, только перерисовка.
 *
 * @param boxSize сторона квадрата-дорожки (зона касания — 40dp)
 */
@Composable
fun SpringCheckbox(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    boxSize: Dp = 24.dp,
) {
    val motionOn = LocalMotionEnabled.current
    val fill = remember { Animatable(if (checked) 1f else 0f) }
    val mark = remember { Animatable(if (checked) 1f else 0f) }
    // Желе-осевое сплющивание: +1 — приседание при установке, −0.6 — лёгкий отклик при снятии.
    val squash = remember { Animatable(0f) }

    // Спеки захватываются в композабл-контексте: внутри LaunchedEffect
    // вызывать @Composable нельзя (грабль).
    val softSpec = motionSpring<Float>(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS)
    val popSpec = motionSpring<Float>(BlokMotion.BOUNCY_DAMPING, BlokMotion.BOUNCY_STIFFNESS)
    val markSpec = motionSpring<Float>(BlokMotion.CURVE_DAMPING, BlokMotion.CURVE_STIFFNESS)

    LaunchedEffect(checked) {
        if (!motionOn) {
            fill.snapTo(if (checked) 1f else 0f)
            mark.snapTo(if (checked) 1f else 0f)
            squash.snapTo(0f)
            return@LaunchedEffect
        }
        if (checked) {
            launch {
                squash.snapTo(1f)
                squash.animateTo(0f, popSpec)
            }
            // Заливка и галочка едут ПАРАЛЛЕЛЬНО: последовательная версия
            // удваивала время и выглядела «залипшей» до прорисовки галочки.
            launch { fill.animateTo(1f, softSpec) }
            mark.animateTo(1f, markSpec)
        } else {
            launch {
                squash.snapTo(-0.6f)
                squash.animateTo(0f, popSpec)
            }
            launch { mark.animateTo(0f, softSpec) }
            fill.animateTo(0f, softSpec)
        }
    }

    // Цвета захватываем до draw-лямбды: внутри неё вызывать @Composable нельзя.
    val uncheckedBorder = MaterialTheme.colorScheme.outlineVariant
    val fillColor = MaterialTheme.colorScheme.primary
    val checkColor = MaterialTheme.colorScheme.onPrimary
    val src = rememberPressSource()

    Box(
        modifier
            .size(40.dp)
            .pressScale(src)
            .clickable(
                interactionSource = src,
                indication = null,
                role = Role.Checkbox,
                onClick = { onCheckedChange(!checked) },
            )
            .semantics { selected = checked },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(boxSize)
                // Желе: осевое сплющивание без рекомпозиции.
                .graphicsLayer {
                    val s = squash.value * 0.16f
                    scaleX = 1f + s
                    scaleY = 1f - s
                }
                .drawBehind {
                    val strokeW = boxSize.toPx() * 0.075f
                    val radius = boxSize.toPx() * 0.28f
                    val f = fill.value

                    // Дорожка: рамка остаётся серой — её догоняет и накрывает
                    // заливка-протяжка (лерп рамки впереди заливки давал
                    // «ползущий» ореол вокруг ещё не заполненной части).
                    drawRoundRect(
                        color = uncheckedBorder,
                        topLeft = Offset(strokeW / 2f, strokeW / 2f),
                        size = Size(size.width - strokeW, size.height - strokeW),
                        cornerRadius = CornerRadius(radius, radius),
                        style = Stroke(strokeW),
                    )

                    // Заливка-протяжка слева (как у hold-кнопки).
                    if (f > 0.01f) {
                        val w = size.width * f
                        drawRoundRect(
                            color = fillColor,
                            topLeft = Offset(0f, 0f),
                            size = Size(w, size.height),
                            cornerRadius = CornerRadius(minOf(radius, w / 2f), minOf(radius, w / 2f)),
                        )
                    }

                    // Галочка прорисовывается частичным сегментом пути.
                    val m = mark.value
                    if (m > 0.01f) {
                        val path = Path().apply {
                            moveTo(size.width * 0.24f, size.height * 0.54f)
                            lineTo(size.width * 0.42f, size.height * 0.72f)
                            lineTo(size.width * 0.78f, size.height * 0.30f)
                        }
                        val pm = PathMeasure()
                        pm.setPath(path, false)
                        val partial = Path()
                        pm.getSegment(0f, pm.length * m, partial, true)
                        drawPath(
                            partial,
                            color = checkColor,
                            style = Stroke(
                                width = boxSize.toPx() * 0.11f,
                                cap = StrokeCap.Round,
                                join = StrokeJoin.Round,
                            ),
                        )
                    }
                },
        )
    }
}
