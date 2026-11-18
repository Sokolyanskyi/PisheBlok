package com.pisheblok.app.ui.motion.components

import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.dp
import com.pisheblok.app.data.model.formatQty
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.LocalMotionEnabled
import com.pisheblok.app.ui.motion.motionSpring
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Jelly Scale — порт Jelly Slider из Motion-KIT: пружинная шкала с гу-пузырём
 * над числовым полем. Поле остаётся источником истины (клавиатура работает
 * как раньше), шкала синхронизирована в обе стороны. Пузырь отстаёт от
 * пальца на мягкой пружине и сливается с ним через goo-фильтр (API 31+,
 * на младших — аккуратный fallback без слияния).
 *
 * @param value текущее значение (синхронизировано с текстовым полем)
 * @param onValueChange новое значение, привязанное к шагу
 */
@Composable
fun JellyScale(
    value: Float?,
    onValueChange: (Float) -> Unit,
    min: Float,
    max: Float,
    step: Float,
    modifier: Modifier = Modifier,
    endLabels: Boolean = false,
) {
    val motionOn = LocalMotionEnabled.current
    val valid = value != null && value.isFinite() && value in min..max

    // Жёсткий палец (900/50) и мягкий пузырь (260/18 → ζ 0.56) — из кита.
    val thumb = remember { Animatable(value ?: min) }
    val bubble = remember { Animatable(value ?: min) }
    val bubbleScale = remember { Animatable(if (valid) 1f else 0f) }
    var railW by remember { mutableStateOf(0f) }

    val thumbSpec = motionSpring<Float>(BlokMotion.THUMB_DAMPING, BlokMotion.THUMB_STIFFNESS)
    val bubbleSpec = spring<Float>(dampingRatio = 0.56f, stiffness = 260f)
    val popSpec = motionSpring<Float>(0.5f, 320f)

    LaunchedEffect(value, valid) {
        val t = (value ?: min).coerceIn(min, max)
        if (!motionOn) {
            thumb.snapTo(t)
            bubble.snapTo(t)
            bubbleScale.snapTo(if (valid) 1f else 0f)
            return@LaunchedEffect
        }
        launch { thumb.animateTo(t, thumbSpec) }
        launch { bubble.animateTo(t, bubbleSpec) }
        bubbleScale.animateTo(if (valid) 1f else 0f, popSpec)
    }

    val railColor = MaterialTheme.colorScheme.outlineVariant
    val tickColor = MaterialTheme.colorScheme.outlineVariant
    val blobColor = MaterialTheme.colorScheme.primary

    fun xToValue(x: Float): Float {
        if (railW <= 0f) return min
        val norm = (x / railW).coerceIn(0f, 1f)
        val raw = min + norm * (max - min)
        val snapped = ((raw - min) / step).roundToInt() * step + min
        return snapped.coerceIn(min, max)
    }

    fun valueToX(v: Float): Float =
        if (railW <= 0f) 0f else ((v - min) / (max - min)).coerceIn(0f, 1f) * railW

    // Гу-фильтр: blur + порог по альфе — пузыри сливаются в «желе».
    val gooEffect = remember {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                val blur = RenderEffect.createBlurEffect(10f, 10f, Shader.TileMode.CLAMP)
                val matrix = android.graphics.ColorMatrix(
                    floatArrayOf(
                        1f, 0f, 0f, 0f, 0f,
                        0f, 1f, 0f, 0f, 0f,
                        0f, 0f, 1f, 0f, 0f,
                        0f, 0f, 0f, 18f, -8f, // резкий порог по альфе
                    )
                )
                val threshold = RenderEffect.createColorFilterEffect(
                    android.graphics.ColorMatrixColorFilter(matrix)
                )
                RenderEffect.createChainEffect(threshold, blur).asComposeRenderEffect()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Column(modifier.fillMaxWidth()) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { railW = it.width.toFloat() }
            .pointerInput(min, max, step) {
                detectDragGestures(
                    onDragStart = { pos -> onValueChange(xToValue(pos.x)) },
                    onDrag = { change, _ ->
                        change.consume()
                        onValueChange(xToValue(change.position.x))
                    },
                )
            },
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .drawBehind {
                    val y = size.height * 0.62f
                    drawLine(railColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 3.dp.toPx())
                    val ticks = 10
                    for (i in 0..ticks) {
                        val x = size.width * i / ticks
                        val h = if (i % 5 == 0) 9.dp.toPx() else 5.dp.toPx()
                        drawLine(
                            tickColor,
                            Offset(x, y - h / 2f),
                            Offset(x, y + h / 2f),
                            strokeWidth = 1.5f.dp.toPx(),
                        )
                    }
                },
        )

        // Две «капли» на одном goo-слое: палец на дорожке, пузырь над ним.
        // Значения читаются внутри draw/graphicsLayer — без рекомпозиции на кадр.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(34.dp)
                .graphicsLayer {
                    if (gooEffect != null) renderEffect = gooEffect
                    val s = bubbleScale.value.coerceAtLeast(0.001f)
                    scaleX = s
                    scaleY = s
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(0f, 0f)
                },
        ) {
            if (railW <= 0f || bubbleScale.value <= 0.01f) return@Canvas
            val y = size.height * 0.62f
            val thumbX = valueToX(thumb.value)
            val bubbleX = valueToX(bubble.value)
            // Пузырь-спутник тянется за пальцем.
            drawCircle(blobColor, radius = 8.dp.toPx(), center = Offset(bubbleX, y - 10.dp.toPx()))
            // Палец-«капля».
            drawCircle(blobColor, radius = 10.dp.toPx(), center = Offset(thumbX, y))
        }
    }
    if (endLabels) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                formatQty(min.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                formatQty(max.toDouble()),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
    }
}
