package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import com.pisheblok.app.ui.motion.LocalMotionEnabled
import kotlin.math.roundToInt

/**
 * Пружинный count-up числа (счётчики карточек, итоги): значение доезжает
 * до цели с лёгким overshoot. Перекомпозится только этот Text.
 */
@Composable
fun CountUpText(
    value: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.headlineSmall,
    color: Color = Color.Unspecified,
    format: (Int) -> String = { it.toString() },
) {
    val enabled = LocalMotionEnabled.current
    val anim = remember { Animatable(value.toFloat()) }
    LaunchedEffect(value, enabled) {
        if (!enabled) {
            anim.snapTo(value.toFloat())
        } else {
            anim.animateTo(value.toFloat(), spring(dampingRatio = 0.75f, stiffness = 400f))
        }
    }
    Text(
        text = format(anim.value.roundToInt()),
        modifier = modifier,
        style = style,
        color = color,
    )
}
