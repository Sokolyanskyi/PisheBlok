package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.snap
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import com.pisheblok.app.ui.motion.LocalMotionEnabled

/**
 * Spring Accordion — порт из Motion-KIT: раскрытие едет пружиной высоты,
 * скрытый контент не композируется. Клик по header-зоне остаётся за вызывающим.
 */
@Composable
fun SpringAccordion(
    expanded: Boolean,
    modifier: Modifier = Modifier,
    header: @Composable () -> Unit,
    content: @Composable () -> Unit,
) {
    val motionOn = LocalMotionEnabled.current
    val sizeSpec = if (motionOn) spring<IntSize>(dampingRatio = 0.75f, stiffness = 400f) else snap<IntSize>()
    val fadeSpec = if (motionOn) spring<Float>(dampingRatio = 0.75f, stiffness = 400f) else snap<Float>()
    Column(modifier) {
        header()
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(sizeSpec) + fadeIn(fadeSpec),
            exit = shrinkVertically(sizeSpec) + fadeOut(fadeSpec),
        ) {
            content()
        }
    }
}
