package com.pisheblok.app.ui.motion.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.motionColorSpring
import com.pisheblok.app.ui.motion.motionSpring
import com.pisheblok.app.ui.motion.pressScale
import com.pisheblok.app.ui.motion.rememberPressSource
import com.pisheblok.app.ui.theme.ctaColors

/** Вкладка нижней навигации. */
data class PillTab(
    val route: String,
    val label: String,
    val icon: ImageVector,
)

private val PILL_SIZE = 44.dp
private val PILL_RADIUS = 14.dp

/**
 * Плавающая пилюля-навигация (как нижний бар референса): скруглённый
 * контейнер на отступе от краёв, лаймовый индикатор едет за иконкой
 * выбранной вкладки на жёсткой пружине (620/40), иконка отскакивает
 * при выборе. Геометрия не пересчитывается на кадр: индикатор рисуется
 * в draw-фазе по одному Animatable-отклику.
 */
@Composable
fun SpringPillNav(
    tabs: List<PillTab>,
    selectedRoute: String?,
    onSelect: (PillTab) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedIndex = tabs.indexOfFirst { it.route == selectedRoute }.coerceAtLeast(0)
    var barWidth by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val pillPx = remember(density) { with(density) { PILL_SIZE.toPx() } }

    Surface(
        // Тёмная: графит-пилюля на чёрном; светлая: белая пилюля на молоке.
        color = if (com.pisheblok.app.ui.theme.LocalDarkTheme.current)
            MaterialTheme.colorScheme.surfaceContainer
        else MaterialTheme.colorScheme.surfaceContainerLowest,
        shape = RoundedCornerShape(32.dp),
        shadowElevation = 12.dp,
        modifier = modifier
            .navigationBarsPadding()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(64.dp)
                // Ширина бара нужна для расчёта клетки вкладки — без неё
                // индикатор не нарисуется вовсе.
                .onSizeChanged { barWidth = it.width },
        ) {
            val pos = remember { Animatable(0f) }
            var hasMeasured by remember { mutableStateOf(false) }

            // Спецификация и цвет захватываются в композабл-контексте:
            // внутри LaunchedEffect/drawBehind вызывать @Composable нельзя.
            val posSpec = motionSpring<Float>(BlokMotion.PILL_LEAD_DAMPING, BlokMotion.PILL_LEAD_STIFFNESS)
            val indicatorColor = ctaColors().container

            LaunchedEffect(selectedIndex, barWidth) {
                if (barWidth <= 0 || tabs.isEmpty()) return@LaunchedEffect
                val cell = barWidth.toFloat() / tabs.size
                val target = selectedIndex * cell + (cell - pillPx) / 2f
                if (!hasMeasured) {
                    // Первый показ: мгновенный снап (грабль про нулевую геометрию).
                    pos.snapTo(target)
                    hasMeasured = true
                    return@LaunchedEffect
                }
                pos.animateTo(target, posSpec)
            }

            // Квадратный индикатор: только draw, без layout на кадр.
            Box(
                Modifier
                    .matchParentSize()
                    .drawBehind {
                        if (!hasMeasured) return@drawBehind
                        val cell = size.width / tabs.size
                        val pillCenterY = PILL_SIZE.toPx() / 2f + 1.5.dp.toPx()
                        drawRoundRect(
                            color = indicatorColor,
                            topLeft = Offset(pos.value, pillCenterY - PILL_SIZE.toPx() / 2f),
                            size = Size(pillPx, pillPx),
                            cornerRadius = CornerRadius(PILL_RADIUS.toPx(), PILL_RADIUS.toPx()),
                        )
                    }
            )

            // Вкладки центрируются по вертикали: блок «иконка + подпись»
            // стоит по центру бара, индикатор совпадает с зоной иконки.
            Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                tabs.forEachIndexed { index, tab ->
                    NavItem(
                        tab = tab,
                        selected = index == selectedIndex,
                        onSelect = { onSelect(tab) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

@Composable
private fun NavItem(
    tab: PillTab,
    selected: Boolean,
    onSelect: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val src = rememberPressSource()
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.12f else 1f,
        animationSpec = motionSpring<Float>(BlokMotion.BOUNCY_DAMPING, BlokMotion.BOUNCY_STIFFNESS),
        label = "navIconScale",
    )
    // Иконка живёт НА лаймовой пилюле (тёмный текст), подпись — ПОД ней
    // на фоне бара: цвет подписи выбранной вкладки — обычный onSurface.
    val iconTint by animateColorAsState(
        targetValue = if (selected) ctaColors().content
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "navIconTint",
    )
    val labelTint by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onSurface
        else MaterialTheme.colorScheme.onSurfaceVariant,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "navLabelTint",
    )
    Column(
        modifier
            .pressScale(src)
            .clickable(interactionSource = src, indication = null, onClick = onSelect),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Зона иконки фиксированной высоты — индикатор совпадает с ней,
        // блок иконка+подпись центрируется по вертикали в баре.
        Box(Modifier.height(44.dp), contentAlignment = Alignment.Center) {
            Icon(
                imageVector = tab.icon,
                contentDescription = tab.label,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .scale(scale),
            )
        }
        Text(
            text = tab.label,
            style = MaterialTheme.typography.labelMedium,
            color = labelTint,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
        )
    }
}
