package com.pisheblok.app.ui.motion

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Глобальный флаг анимаций — аналог html[data-motion="off"] из кита.
 * Уважает системную шкалу «Длительность анимации» (0 = всё мгновенно):
 * все пружины переходят в snap, вид не ломается.
 */
val LocalMotionEnabled: ProvidableCompositionLocal<Boolean> = staticCompositionLocalOf { true }

@Composable
fun rememberMotionEnabled(): Boolean {
    val context = LocalContext.current
    return remember {
        try {
            Settings.Global.getFloat(
                context.contentResolver,
                Settings.Global.ANIMATOR_DURATION_SCALE, 1f,
            ) > 0f
        } catch (_: Exception) {
            true
        }
    }
}

/**
 * Пружина, уважающая флаг анимаций: при выключенных — мгновенный snap.
 * Тип указывай явно на месте вызова: motionSpring<Float>(...) — грабль.
 */
@Composable
inline fun <reified T> motionSpring(damping: Float, stiffness: Float): AnimationSpec<T> =
    if (LocalMotionEnabled.current) spring(dampingRatio = damping, stiffness = stiffness)
    else snap()

/** Цветовая пружина: animateColorAsState требует AnimationSpec<Color>. */
@Composable
fun motionColorSpring(damping: Float, stiffness: Float): AnimationSpec<Color> =
    if (LocalMotionEnabled.current) spring(dampingRatio = damping, stiffness = stiffness)
    else snap()

/** Единый пружинный отклик нажатия для карточек, кнопок и чипов. */
fun Modifier.pressScale(
    source: MutableInteractionSource,
    pressedScale: Float = 0.97f,
): Modifier = composed {
    val pressed by source.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = motionSpring<Float>(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "pressScale",
    )
    // Значение читается внутри graphicsLayer — без рекомпозиции на кадр.
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** Источник взаимодействий для связки pressScale + clickable. */
@Composable
fun rememberPressSource(): MutableInteractionSource = remember { MutableInteractionSource() }

/**
 * Каскадный влёт при первом появлении (дети × 45ms + 70ms, как в ките).
 * Играет один раз на экран (rememberSaveable) — не переигрывается при
 * повторном входе элемента во вьюпорт. Применять только к статичным
 * секциям, НЕ к элементам LazyColumn (переигрывают при скролле — грабль).
 */
fun Modifier.staggerIn(index: Int): Modifier = composed {
    val enabled = LocalMotionEnabled.current
    var played by rememberSaveable { mutableStateOf(false) }
    val progress = remember { Animatable(if (enabled && !played) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) {
            kotlinx.coroutines.delay(BlokMotion.staggerMs(index).toLong())
            progress.animateTo(
                1f,
                spring(dampingRatio = BlokMotion.SOFT_DAMPING, stiffness = BlokMotion.SOFT_STIFFNESS),
            )
            played = true
        }
    }
    // Значение читается внутри graphicsLayer — без рекомпозиции на кадр.
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * 28.dp.toPx()
    }
}

/**
 * Плавная пересадка элемента внутри Column-контейнера: когда порядок
 * соседей меняется (выполненный айтем уезжает в конец секции), блок
 * пружиной (500/0.80) доезжает до нового места, соседи разъезжаются
 * навстречу. Сдвиг чисто визуальный (graphicsLayer.translationY) —
 * не влияет на layout-координаты, поэтому нет feedback-петли измерений.
 * Применять к прямым детям Column вместе с key(id) — иначе remember
 * живёт в слоте, а не в данных, и анимация не увидит переезд.
 */
fun Modifier.animatePlacementInColumn(): Modifier = composed {
    val shift = remember { Animatable(0f) }
    var lastY by remember { mutableFloatStateOf(0f) }
    var hasLast by remember { mutableStateOf(false) }
    var shiftFrom by remember { mutableFloatStateOf(0f) }
    val moveSpec = motionSpring<Float>(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS)

    // Слот сменился: рисуем карточку со сдвигом «откуда уехала» и
    // пружиной доводим до нуля. Анимация — только в этой точке.
    LaunchedEffect(shiftFrom) {
        if (shiftFrom != 0f) {
            shift.snapTo(shiftFrom)
            shift.animateTo(0f, moveSpec)
        }
    }

    this
        .onGloballyPositioned { coordinates ->
            val y = coordinates.positionInParent().y
            if (!hasLast) {
                lastY = y
                hasLast = true
            } else if (y != lastY) {
                shiftFrom = lastY - y
                lastY = y
            }
        }
        .graphicsLayer { translationY = shift.value }
}
