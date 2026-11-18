package com.pisheblok.app.ui.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pisheblok.app.R
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.formatQty
import com.pisheblok.app.ui.components.DropdownField
import com.pisheblok.app.ui.components.EmptyState
import com.pisheblok.app.ui.components.ExpiryBadge
import com.pisheblok.app.ui.components.LocalAppContainer
import com.pisheblok.app.ui.components.BuyQtyInput
import com.pisheblok.app.ui.components.PriorityChips
import com.pisheblok.app.ui.components.QtyDialog
import com.pisheblok.app.ui.components.label
import com.pisheblok.app.ui.components.parseBuyQtyInput
import com.pisheblok.app.ui.locations.ItemUi
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.animatePlacementInColumn
import com.pisheblok.app.ui.motion.components.CountUpText
import com.pisheblok.app.ui.motion.components.HoldButton
import com.pisheblok.app.ui.motion.components.SpringAccordion
import com.pisheblok.app.ui.motion.components.SpringCheckbox
import com.pisheblok.app.ui.motion.motionColorSpring
import com.pisheblok.app.ui.motion.motionSpring
import com.pisheblok.app.ui.theme.Blok
import com.pisheblok.app.ui.theme.blokCardShadow
import com.pisheblok.app.ui.theme.blokTabular
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun ShoppingScreen() {
    val container = LocalAppContainer.current
    val vm: ShoppingViewModel = viewModel(key = "shopping") {
        ShoppingViewModel(container.repository)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Состояния-холдеры для стабильных лямбд списков.
    val buyForItem = remember { mutableStateOf<ItemUi?>(null) }
    val buyUrgent = remember { mutableStateOf(true) }
    var showManualAdd by rememberSaveable { mutableStateOf(false) }
    val editEntryId = remember { mutableStateOf<String?>(null) }

    val addedMsg = stringResource(R.string.added_to_list)
    val restockMsg = stringResource(R.string.restock_snack)

    // Стабильный колбэк: списки не рекомпозируют при каждой рекомпозиции экрана.
    val onToggleEntry: (BuyUi, Boolean, String) -> Unit = remember(vm, snackbar, scope, restockMsg) {
        { entry: BuyUi, checked: Boolean, restockAction: String ->
            vm.setDone(entry.id, checked)
            if (checked && entry.sourceItemId != null) {
                scope.launch {
                    val result = snackbar.showSnackbar(
                        message = restockMsg,
                        actionLabel = restockAction,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        vm.restock(entry.id)
                    }
                }
            }
        }
    }
    val onEditEntry: (String) -> Unit = remember { { editEntryId.value = it } }
    val onDeleteEntry: (String) -> Unit = remember { { vm.deleteEntry(it) } }

    // ---------- drag & drop: между «Срочно»/«Потом» и внутри списка ----------
    val haptics = LocalHapticFeedback.current
    var draggingEntry by remember { mutableStateOf<BuyUi?>(null) }
    val dragPoint = remember { mutableStateOf(Offset.Zero) }
    val urgentZone = remember { mutableStateOf(Rect.Zero) }
    val laterZone = remember { mutableStateOf(Rect.Zero) }
    val listBounds = remember { mutableStateOf(Rect.Zero) }
    // Границы карточек списка (root-координаты): для расчёта точки вставки.
    val rowBounds = remember { mutableStateMapOf<String, Rect>() }
    val listState = rememberLazyListState()
    val dragEdgePx = with(LocalDensity.current) { 120.dp.toPx() }

    /**
     * Точка вставки: (секция-приёмник, id карточки, перед которой встать;
     * null — в конец секции). Позиция — по центрам карточек: палец выше
     * центра первой карточки, чей центр ниже пальца, — вставка перед ней.
     */
    fun computeDrop(entry: BuyUi, point: Offset): Pair<Boolean, String?>? {
        val inUrgent = urgentZone.value.contains(point)
        val inLater = laterZone.value.contains(point)
        if (!inUrgent && !inLater) return null
        val targetUrgent = if (inUrgent && inLater) entry.urgent else inUrgent
        val others = (if (targetUrgent) state.urgent else state.later).filter { it.id != entry.id }
        var beforeId: String? = null
        for (other in others) {
            val b = rowBounds[other.id] ?: continue
            if (b.height <= 0f) continue
            if (point.y < b.center.y) {
                beforeId = other.id
                break
            }
        }
        return targetUrgent to beforeId
    }

    // Прямоугольник цели (куда встанет карточка): для автоскролла.
    fun dropTargetRect(targetUrgent: Boolean, beforeId: String?): Rect? {
        val list = if (targetUrgent) state.urgent else state.later
        val anchorId = beforeId ?: list.lastOrNull()?.id
        return anchorId?.let { rowBounds[it] }
    }

    val onDragStart: (BuyUi) -> Unit = remember(haptics) { { entry ->
        draggingEntry = entry
        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
    } }
    val onDrag: (Offset) -> Unit = remember { { point -> dragPoint.value = point } }
    val onDragEnd: (BuyUi) -> Unit = remember(vm) { { entry ->
        if (draggingEntry?.id == entry.id) {
            val drop = computeDrop(entry, dragPoint.value)
            if (drop != null) {
                vm.moveEntry(entry.id, drop.first, drop.second)
            }
        }
        draggingEntry = null
    } }
    val setUrgentZone: (Rect) -> Unit = remember { { urgentZone.value = it } }
    val setLaterZone: (Rect) -> Unit = remember { { laterZone.value = it } }
    val setRowBounds: (String, Rect?) -> Unit = remember { { id, rect ->
        if (rect == null) rowBounds.remove(id) else rowBounds[id] = rect
    } }

    // Автоскролл: палец у края, а цель вставки (карточка-якорь или, если
    // секция свернута, её зона) ещё не видна целиком — иначе скролл
    // «убегает» от пальца и дроп мимо цели.
    LaunchedEffect(draggingEntry) {
        val dragging = draggingEntry
        while (draggingEntry != null && dragging != null) {
            val b = listBounds.value
            if (b.height > 0f) {
                val drop = computeDrop(dragging, dragPoint.value)
                val rect = drop?.let { dropTargetRect(it.first, it.second) }
                val zone = when {
                    drop == null -> null
                    drop.first -> urgentZone.value
                    else -> laterZone.value
                }
                val target = rect ?: zone
                val targetVisible = target == null || (target.top >= b.top && target.bottom <= b.bottom)
                when {
                    !targetVisible && dragPoint.value.y > b.bottom - dragEdgePx -> listState.scrollBy(16f)
                    !targetVisible && dragPoint.value.y < b.top + dragEdgePx -> listState.scrollBy(-16f)
                }
            }
            delay(16)
        }
    }
    // Точка вставки на время перетаскивания — и подсветка ЧУЖОЙ секции
    // (заголовок горит только при переносе между секциями).
    val dragging = draggingEntry
    val drop = dragging?.let { computeDrop(it, dragPoint.value) }
    val crossSection = drop != null && drop.first != dragging.urgent
    val urgentHover = drop != null && crossSection && drop.first
    val laterHover = drop != null && crossSection && !drop.first
    val urgentDropBefore = drop?.takeIf { it.first }?.second
    val urgentDropAtEnd = drop != null && drop.first && drop.second == null
    val laterDropBefore = drop?.takeIf { !it.first }?.second
    val laterDropAtEnd = drop != null && !drop.first && drop.second == null

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(
                start = Blok.Space.l,
                end = Blok.Space.l,
                top = Blok.Space.s,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Blok.Space.s),
            modifier = Modifier
                .fillMaxSize()
                .onGloballyPositioned { listBounds.value = it.boundsInRoot() },
        ) {
            // ---------- running low ----------
            item(key = "low_header") {
                Text(
                    stringResource(R.string.section_running_low),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.low.isEmpty()) {
                item(key = "low_empty") {
                    EmptyState(
                        emoji = "🧊",
                        title = stringResource(R.string.empty_low_title),
                        subtitle = stringResource(R.string.empty_low_subtitle),
                    )
                }
            } else {
                items(state.low, key = { "low-${it.id}" }) { item ->
                    LowRow(item = item, onBuy = { buyForItem.value = item; buyUrgent.value = true })
                }
            }

            // ---------- buy list: секции на пружинных аккордеонах ----------
            item(key = "buy_header") {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 8.dp),
                ) {
                    Text(
                        stringResource(R.string.section_buy),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f),
                    )
                    if (state.hasDone) {
                        // Очистка выполненных — необратимое: hold-to-confirm.
                        HoldButton(
                            label = stringResource(R.string.clear_done),
                            onConfirm = { vm.clearDone() },
                            height = 36.dp,
                            modifier = Modifier.width(160.dp),
                        )
                    }
                    // Добавление в список — в шапке: FAB накрывал кнопки строк.
                    IconButton(onClick = { showManualAdd = true }) {
                        Icon(
                            Icons.Rounded.Add,
                            contentDescription = stringResource(R.string.add),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            if (state.urgent.isEmpty() && state.later.isEmpty()) {
                item(key = "buy_empty") {
                    EmptyState(
                        emoji = "🛒",
                        title = stringResource(R.string.empty_buy_title),
                        subtitle = stringResource(R.string.empty_buy_subtitle),
                    )
                }
            } else {
                if (state.urgent.isNotEmpty()) {
                    item(key = "urgent_section") {
                        BuySection(
                            title = stringResource(R.string.shopping_urgent),
                            urgent = true,
                            entries = state.urgent,
                            hovered = urgentHover,
                            dropBeforeId = urgentDropBefore,
                            dropAtEnd = urgentDropAtEnd,
                            draggingId = draggingEntry?.id,
                            onZone = setUrgentZone,
                            onRowBounds = setRowBounds,
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onToggle = onToggleEntry,
                            onEdit = onEditEntry,
                            onDelete = onDeleteEntry,
                        )
                    }
                }
                if (state.later.isNotEmpty()) {
                    item(key = "later_section") {
                        BuySection(
                            title = stringResource(R.string.shopping_later),
                            urgent = false,
                            entries = state.later,
                            hovered = laterHover,
                            dropBeforeId = laterDropBefore,
                            dropAtEnd = laterDropAtEnd,
                            draggingId = draggingEntry?.id,
                            onZone = setLaterZone,
                            onRowBounds = setRowBounds,
                            onDragStart = onDragStart,
                            onDrag = onDrag,
                            onDragEnd = onDragEnd,
                            onToggle = onToggleEntry,
                            onEdit = onEditEntry,
                            onDelete = onDeleteEntry,
                        )
                    }
                }
            }
        }

        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ---------- dialogs ----------
    buyForItem.value?.let { item ->
        QtyDialog(
            title = stringResource(R.string.buy_qty_question),
            subtitle = item.name,
            unit = item.unit,
            confirmText = stringResource(R.string.add_to_list),
            onConfirm = { input ->
                scope.launch { snackbar.showSnackbar(addedMsg) }
                vm.addFromItem(item, input.qty, input.note, buyUrgent.value)
                buyForItem.value = null
            },
            onDismiss = { buyForItem.value = null },
            urgent = buyUrgent.value,
            onUrgentChange = { buyUrgent.value = it },
        )
    }
    if (showManualAdd) {
        EntryDialog(
            titleRes = R.string.add_item,
            initialName = "",
            initialQtyText = "",
            initialUnit = QuantityUnit.PCS,
            initialUrgent = true,
            onConfirm = { name, input, unit, urgent ->
                vm.addManual(name, input.qty, input.note, unit, urgent)
                showManualAdd = false
            },
            onDismiss = { showManualAdd = false },
        )
    }
    editEntryId.value?.let { id ->
        val entry = state.urgent.firstOrNull { it.id == id } ?: state.later.firstOrNull { it.id == id }
        if (entry != null) {
            EntryDialog(
                titleRes = R.string.edit,
                initialName = entry.name,
                // Без количества — пустое поле, не «0».
                initialQtyText = entry.qtyNote
                    ?: (if (entry.qty > 0.0) formatQty(entry.qty) else ""),
                initialUnit = entry.unit,
                initialUrgent = entry.urgent,
                onConfirm = { name, input, unit, urgent ->
                    vm.updateEntry(id, name, input.qty, input.note, unit, urgent)
                    editEntryId.value = null
                },
                onDismiss = { editEntryId.value = null },
            )
        } else {
            editEntryId.value = null
        }
    }
}

/**
 * Секция «Купить» на пружинном аккордеоне: шапка раскрывает список,
 * раскрытие едет пружиной высоты. Состояние сохраняется при скролле.
 * Секция — drop-зона drag&drop: регистрирует свои границы, при «наведении»
 * переносимой записи шапка подсвечивается, на точке вставки рисуется
 * индикатор (перед карточкой [dropBeforeId] или в конце при [dropAtEnd]).
 */
@Composable
private fun BuySection(
    title: String,
    urgent: Boolean,
    entries: List<BuyUi>,
    hovered: Boolean,
    dropBeforeId: String?,
    dropAtEnd: Boolean,
    draggingId: String?,
    onZone: (Rect) -> Unit,
    onRowBounds: (String, Rect?) -> Unit,
    onDragStart: (BuyUi) -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: (BuyUi) -> Unit,
    onToggle: (BuyUi, Boolean, String) -> Unit,
    onEdit: (String) -> Unit,
    onDelete: (String) -> Unit,
) {
    var expanded by rememberSaveable { mutableStateOf(true) }
    val chevronRotation by animateFloatAsState(
        targetValue = if (expanded) 0f else -90f,
        animationSpec = motionSpring<Float>(BlokMotion.BOUNCY_DAMPING, BlokMotion.BOUNCY_STIFFNESS),
        label = "chevron",
    )

    Box(modifier = Modifier.onGloballyPositioned { onZone(it.boundsInRoot()) }) {
        SpringAccordion(
            expanded = expanded,
            header = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Blok.cardSmall)
                        .background(
                            if (hovered) MaterialTheme.colorScheme.primaryContainer
                            else Color.Transparent
                        )
                        .clickable { expanded = !expanded }
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(10.dp)
                            .background(
                                if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary,
                                CircleShape,
                            ),
                    )
                    Text(
                        title,
                        style = MaterialTheme.typography.titleSmall,
                        color = if (hovered) MaterialTheme.colorScheme.onPrimaryContainer
                        else if (urgent) MaterialTheme.colorScheme.error
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    CountUpText(
                        value = entries.size,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (hovered) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.width(8.dp))
                    Icon(
                        Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = if (hovered) MaterialTheme.colorScheme.onPrimaryContainer
                        else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.rotate(chevronRotation),
                    )
                }
            },
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                entries.forEach { entry ->
                    if (dropBeforeId == entry.id) InsertIndicator()
                    // key по id: remember-аниматоры принадлежат записи, а не слоту
                    // колонки — переезд айтема при отметке анимируется.
                    key(entry.id) {
                        val qtyLabel = buyQtyLabel(entry)
                        // Без количества — действие без «0 шт».
                        val restockAction = qtyLabel?.let { stringResource(R.string.restock_action, it) }
                            ?: stringResource(R.string.restock_no_qty)
                        BuyRow(
                            entry = entry,
                            dragging = draggingId == entry.id,
                            onBounds = { rect -> onRowBounds(entry.id, rect) },
                            onDragStart = { onDragStart(entry) },
                            onDrag = onDrag,
                            onDragEnd = { onDragEnd(entry) },
                            onToggle = { checked -> onToggle(entry, checked, restockAction) },
                            onEdit = { onEdit(entry.id) },
                            onDelete = { onDelete(entry.id) },
                        )
                    }
                }
                if (dropAtEnd) InsertIndicator()
            }
        }
    }
}

/**
 * Кол-во для показа: текстовая заметка, «число + единица» или null,
 * если количество не указано (число 0 возникает только из пустого поля —
 * введённый «0» сохраняется заметкой).
 */
@Composable
private fun buyQtyLabel(entry: BuyUi): String? =
    entry.qtyNote?.takeIf { it.isNotBlank() }
        ?: (if (entry.qty > 0.0) "${formatQty(entry.qty)} ${entry.unit.label()}" else null)

/** Линия-указатель места вставки при перетаскивании карточек. */
@Composable
private fun InsertIndicator() {
    Box(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .height(3.dp)
            .clip(Blok.pill)
            .background(MaterialTheme.colorScheme.primary),
    )
}

// ---------- rows ----------

@Composable
private fun LowRow(item: ItemUi, onBuy: () -> Unit) {
    Card(
        shape = Blok.cardSmall,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        modifier = Modifier
            .fillMaxWidth()
            .blokCardShadow(Blok.cardSmall),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = Blok.Space.l, top = Blok.Space.s + 2.dp, bottom = Blok.Space.s + 2.dp, end = 6.dp),
        ) {
            Column(Modifier.weight(1f)) {
                // Tutorial-лейбл: янтарная точка + место хранения.
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(6.dp)
                            .background(MaterialTheme.colorScheme.tertiary, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        item.locationPath,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Spacer(Modifier.height(2.dp))
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${formatQty(item.qty)} ${item.unit.label()}",
                        style = blokTabular(MaterialTheme.typography.titleMedium),
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    ExpiryBadge(item.expiry)
                }
            }
            IconButton(onClick = onBuy) {
                Icon(
                    Icons.Rounded.ShoppingCart,
                    contentDescription = stringResource(R.string.add_to_list),
                    tint = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun BuyRow(
    entry: BuyUi,
    dragging: Boolean,
    onBounds: (Rect?) -> Unit,
    onDragStart: () -> Unit,
    onDrag: (Offset) -> Unit,
    onDragEnd: () -> Unit,
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    // Границы карточки — для расчёта точки вставки drag&drop; при уходе
    // из композиции (секция свернулась, запись удалена) — забираем из карты.
    DisposableEffect(entry.id) {
        onDispose { onBounds(null) }
    }
    // Куплено: весь блок темнеет (контейнер к чёрному — работает в обеих
    // темах), текст приглушается. Цвета едут на цветовой пружине, а не
    // мгновенно — блок «угасает» плавно.
    val containerTarget = if (entry.done)
        lerp(MaterialTheme.colorScheme.surfaceContainerLow, Color.Black, 0.22f)
    else MaterialTheme.colorScheme.surfaceContainerLow
    val titleTarget = if (entry.done)
        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.42f)
    else MaterialTheme.colorScheme.onSurface
    val secondaryTarget = if (entry.done)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.onSurfaceVariant
    val qtyTarget = if (entry.done)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
    else MaterialTheme.colorScheme.primary
    val iconTarget = if (entry.done)
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    else MaterialTheme.colorScheme.onSurfaceVariant

    val containerColor by animateColorAsState(
        targetValue = containerTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowContainer",
    )
    val titleColor by animateColorAsState(
        targetValue = titleTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowTitle",
    )
    val secondaryColor by animateColorAsState(
        targetValue = secondaryTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowSecondary",
    )
    val qtyColor by animateColorAsState(
        targetValue = qtyTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowQty",
    )
    val iconColor by animateColorAsState(
        targetValue = iconTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowIcons",
    )

    // Актуальные колбэки для pointerInput: жест живёт в key(entry.id) и не
    // перезапускается на рекомпозиции — читаем свежие лямбды через State.
    val currentOnDragStart by rememberUpdatedState(onDragStart)
    val currentOnDrag by rememberUpdatedState(onDrag)
    val currentOnDragEnd by rememberUpdatedState(onDragEnd)
    // Позиция строки в root-координатах: палец = topLeft строки + локальная
    // позиция события (positionInRoot у PointerInputChange в этой версии
    // compose недоступен).
    val dragRowTopLeft = remember { mutableStateOf(Offset.Zero) }

    Card(
        // Плавная пересадка при переупорядочении: выполненный уезжает
        // в конец секции на пружине.
        modifier = Modifier
            .animatePlacementInColumn()
            .onGloballyPositioned { onBounds(it.boundsInRoot()) }
            .fillMaxWidth()
            .blokCardShadow(Blok.cardSmall)
            .graphicsLayer {
                val s = if (dragging) 1.03f else 1f
                scaleX = s
                scaleY = s
            },
        shape = Blok.cardSmall,
        colors = CardDefaults.cardColors(containerColor = containerColor),
        border = if (dragging) BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        else BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        ) {
            // Зона перетаскивания — чекбокс + текст (кнопки не задеваем:
            // у удаления свой hold-жест).
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .weight(1f)
                    .onGloballyPositioned { dragRowTopLeft.value = it.boundsInRoot().topLeft }
                    .pointerInput(entry.id) {
                        detectDragGesturesAfterLongPress(
                            onDragStart = { _ -> currentOnDragStart() },
                            onDrag = { change, _ ->
                                change.consume()
                                currentOnDrag(dragRowTopLeft.value + change.position)
                            },
                            onDragEnd = { currentOnDragEnd() },
                            onDragCancel = { currentOnDragEnd() },
                        )
                    },
            ) {
                // Пружинный чекбокс: протяжка-заливка, прорисовка галочки, желе-поп.
                SpringCheckbox(checked = entry.done, onCheckedChange = onToggle)
                Spacer(Modifier.width(14.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        entry.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textDecoration = if (entry.done) TextDecoration.LineThrough else null,
                        color = titleColor,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    val qtyLabel = buyQtyLabel(entry)
                    if (qtyLabel != null || entry.linkedLocationPath != null) {
                        Spacer(Modifier.height(2.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            // Количество/заметка — обычным, не жирным шрифтом;
                            // без количества единица («шт» и пр.) не показывается.
                            qtyLabel?.let { label ->
                                Text(
                                    label,
                                    style = blokTabular(MaterialTheme.typography.titleMedium),
                                    color = qtyColor,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                            entry.linkedLocationPath?.let { path ->
                                if (qtyLabel != null) Spacer(Modifier.width(6.dp))
                                Text(
                                    "· $path",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }
            Box {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(
                        Icons.Rounded.MoreVert,
                        contentDescription = stringResource(R.string.row_actions),
                        tint = iconColor,
                    )
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text(stringResource(R.string.edit)) },
                        leadingIcon = { Icon(Icons.Rounded.Edit, contentDescription = null) },
                        onClick = {
                            menuOpen = false
                            onEdit()
                        },
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = {
                            Text(
                                stringResource(R.string.delete),
                                color = MaterialTheme.colorScheme.error,
                            )
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Rounded.Delete,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                            )
                        },
                        onClick = {
                            menuOpen = false
                            onDelete()
                        },
                    )
                }
            }
        }
    }
}

// ---------- add / edit entry dialog ----------

@Composable
private fun EntryDialog(
    titleRes: Int,
    initialName: String,
    initialQtyText: String,
    initialUnit: QuantityUnit,
    initialUrgent: Boolean,
    onConfirm: (name: String, qty: BuyQtyInput, unit: QuantityUnit, urgent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    // Количество — свободный ввод: число, текст или пусто.
    var qtyText by rememberSaveable { mutableStateOf(initialQtyText) }
    var unit by rememberSaveable { mutableStateOf(initialUnit) }
    var urgent by rememberSaveable { mutableStateOf(initialUrgent) }
    val valid = name.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(titleRes)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.buy_name_label)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text(stringResource(R.string.qty_or_note)) },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.small,
                    )
                    DropdownField(
                        label = stringResource(R.string.unit_label),
                        options = QuantityUnit.entries.toList(),
                        selected = unit,
                        optionLabel = { it.label() },
                        onSelect = { unit = it },
                        modifier = Modifier.weight(1f),
                    )
                }
                PriorityChips(urgent = urgent, onUrgentChange = { urgent = it })
            }
        },
        confirmButton = {
            TextButton(
                enabled = valid,
                onClick = { onConfirm(name, parseBuyQtyInput(qtyText), unit, urgent) },
            ) {
                Text(stringResource(if (initialName.isEmpty()) R.string.add else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
