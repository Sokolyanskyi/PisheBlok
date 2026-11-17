package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.LocalMotionEnabled

/**
 * Кольцо свежести: дуга заполняется по близости к истечению срока.
 * Прогресс анимируется на пружине точек (200/0.60) и читается ВНУТРИ
 * draw-лямбды — рекомпозиции на кадр нет, только перерисовка.
 *
 * @param progress 0..1 — доля «сгоревшего» срока
 */
@Composable
fun ExpiryRing(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
    diameter: Dp = 18.dp,
    track: Color = Color.Transparent,
) {
    val enabled = LocalMotionEnabled.current
    val anim = remember { Animatable(progress.coerceIn(0f, 1f)) }
    LaunchedEffect(progress, enabled) {
        if (!enabled) {
            anim.snapTo(progress.coerceIn(0f, 1f))
        } else {
            anim.animateTo(
                progress.coerceIn(0f, 1f),
                spring(dampingRatio = BlokMotion.CURVE_DAMPING, stiffness = BlokMotion.CURVE_STIFFNESS),
            )
        }
    }
    Box(
        modifier
            .size(diameter)
            .drawBehind {
                val stroke = 2.5.dp.toPx()
                val inset = stroke / 2f
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                if (track != Color.Transparent) {
                    drawArc(
                        color = track,
                        startAngle = 0f, sweepAngle = 360f, useCenter = false,
                        topLeft = topLeft, size = arcSize,
                        style = Stroke(stroke, cap = StrokeCap.Round),
                    )
                }
                drawArc(
                    color = color,
                    startAngle = -90f, sweepAngle = 360f * anim.value, useCenter = false,
                    topLeft = topLeft, size = arcSize,
                    style = Stroke(stroke, cap = StrokeCap.Round),
                )
            },
    )
}
