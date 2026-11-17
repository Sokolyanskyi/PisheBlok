package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.LocalMotionEnabled
import com.pisheblok.app.ui.motion.motionSpring
import kotlinx.coroutines.delay

/**
 * Hold Button — «удержи, чтобы подтвердить» (порт из Motion-KIT). ТОЛЬКО для
 * необратимых действий: удаления айтемов, локаций, записей, очистка списков.
 * Заливка бежит слева за 1,2 с, подпись инвертируется под заливкой;
 * ранний отпуск — дёрганье с отскоком заливки; полное удержание — onConfirm.
 * Заменяет диалоги подтверждения на всех необратимых действиях.
 */
@Composable
fun HoldButton(
    label: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    height: Dp = 52.dp,
    holdMs: Int = BlokMotion.HOLD_MS,
    filledColor: Color = MaterialTheme.colorScheme.error,
    filledContentColor: Color = MaterialTheme.colorScheme.onError,
) {
    val motionOn = LocalMotionEnabled.current
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val recedeSpec = motionSpring<Float>(0.5f, 320f)

    LaunchedEffect(holding) {
        if (!motionOn) {
            if (holding) {
                progress.snapTo(1f)
                onConfirm()
            } else {
                progress.snapTo(0f)
            }
            return@LaunchedEffect
        }
        when {
            holding -> {
                progress.animateTo(1f, tween(durationMillis = holdMs, easing = LinearEasing))
                if (progress.value >= 1f) onConfirm()
            }
            progress.value >= 1f -> progress.snapTo(0f) // отпуск после срабатывания
            progress.value > 0.01f -> {
                // Ранний отпуск: «дёрганье» + отскок заливки.
                repeat(4) { i ->
                    shake.snapTo(if (i % 2 == 0) 6f else -6f)
                    delay(45)
                }
                shake.snapTo(0f)
                progress.animateTo(0f, recedeSpec)
            }
        }
    }

    val containerColor = MaterialTheme.colorScheme.surfaceVariant
    val baseLabelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier
            .height(height)
            .clip(RoundedCornerShape(50))
            .pointerInput(motionOn) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        try {
                            awaitRelease()
                        } finally {
                            holding = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        // Дорожка-подложка и бегущая заливка — только draw.
        Canvas(Modifier.matchParentSize()) {
            drawRect(containerColor)
            drawRect(filledColor, size = size.copy(width = size.width * progress.value))
        }
        // Базовая подпись.
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Bold,
            color = baseLabelColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.graphicsLayer { translationX = shake.value },
        )
        // Инвертированная подпись, видна только под заливкой (клип в draw-фазе).
        Box(
            Modifier
                .matchParentSize()
                .drawWithContent {
                    drawContext.canvas.save()
                    drawContext.canvas.clipRect(0f, 0f, size.width * progress.value, size.height)
                    drawContent()
                    drawContext.canvas.restore()
                },
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = filledContentColor,
                textAlign = TextAlign.Center,
                modifier = Modifier.align(Alignment.Center).graphicsLayer { translationX = shake.value },
            )
        }
    }
}

/**
 * Компактная hold-версия иконки-удаления для списков: круг заполняется
 * деструктивным цветом, иконка тонируется в инверсный; ранний отпуск —
 * дёрганье. Полное удержание — onConfirm (без диалога).
 */
@Composable
fun HoldIconButton(
    icon: ImageVector,
    contentDescription: String,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Dp = 40.dp,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    holdMs: Int = BlokMotion.HOLD_MS,
) {
    val motionOn = LocalMotionEnabled.current
    var holding by remember { mutableStateOf(false) }
    val progress = remember { Animatable(0f) }
    val shake = remember { Animatable(0f) }
    val recedeSpec = motionSpring<Float>(0.5f, 320f)

    LaunchedEffect(holding) {
        if (!motionOn) {
            if (holding) {
                progress.snapTo(1f)
                onConfirm()
            } else {
                progress.snapTo(0f)
            }
            return@LaunchedEffect
        }
        when {
            holding -> {
                progress.animateTo(1f, tween(durationMillis = holdMs, easing = LinearEasing))
                if (progress.value >= 1f) onConfirm()
            }
            progress.value >= 1f -> progress.snapTo(0f)
            progress.value > 0.01f -> {
                repeat(4) { i ->
                    shake.snapTo(if (i % 2 == 0) 4f else -4f)
                    delay(40)
                }
                shake.snapTo(0f)
                progress.animateTo(0f, recedeSpec)
            }
        }
    }

    val track = MaterialTheme.colorScheme.surfaceVariant
    val filledColor = MaterialTheme.colorScheme.error
    val filledContentColor = MaterialTheme.colorScheme.onError
    // Прогресс читается в композиции только этого маленького элемента.
    val p = progress.value
    val iconTint = lerp(tint, filledContentColor, p)

    Box(
        modifier
            .size(diameter)
            .clip(CircleShape)
            .pointerInput(motionOn) {
                detectTapGestures(
                    onPress = {
                        holding = true
                        try {
                            awaitRelease()
                        } finally {
                            holding = false
                        }
                    },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(Modifier.size(diameter)) {
            drawCircle(track)
            drawCircle(filledColor, radius = minOf(size.width, size.height) / 2f * p)
        }
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = iconTint,
            modifier = Modifier
                .size(diameter / 2)
                .graphicsLayer { translationX = shake.value },
        )
    }
}
