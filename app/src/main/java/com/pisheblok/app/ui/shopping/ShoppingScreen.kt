package com.pisheblok.app.ui.shopping

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExtendedFloatingActionButton
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
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
import com.pisheblok.app.ui.components.PriorityChips
import com.pisheblok.app.ui.components.QtyDialog
import com.pisheblok.app.ui.components.label
import com.pisheblok.app.ui.components.parseQty
import com.pisheblok.app.ui.locations.ItemUi
import com.pisheblok.app.ui.motion.BlokMotion
import com.pisheblok.app.ui.motion.animatePlacementInColumn
import com.pisheblok.app.ui.motion.components.CountUpText
import com.pisheblok.app.ui.motion.components.HoldButton
import com.pisheblok.app.ui.motion.components.HoldIconButton
import com.pisheblok.app.ui.motion.components.SpringAccordion
import com.pisheblok.app.ui.motion.components.SpringCheckbox
import com.pisheblok.app.ui.motion.motionColorSpring
import com.pisheblok.app.ui.motion.motionSpring
import com.pisheblok.app.ui.theme.Blok
import com.pisheblok.app.ui.theme.blokCardShadow
import com.pisheblok.app.ui.theme.blokTabular
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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(
                start = Blok.Space.l,
                end = Blok.Space.l,
                top = Blok.Space.s,
                bottom = 100.dp,
            ),
            verticalArrangement = Arrangement.spacedBy(Blok.Space.s),
            modifier = Modifier.fillMaxSize(),
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
                            onToggle = onToggleEntry,
                            onEdit = onEditEntry,
                            onDelete = onDeleteEntry,
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showManualAdd = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add)) },
            containerColor = MaterialTheme.colorScheme.tertiary,
            contentColor = MaterialTheme.colorScheme.onTertiary,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ---------- dialogs ----------
    buyForItem.value?.let { item ->
        QtyDialog(
            title = stringResource(R.string.buy_qty_question),
            subtitle = item.name,
            defaultQty = 1.0,
            unit = item.unit,
            confirmText = stringResource(R.string.add_to_list),
            onConfirm = { qty ->
                scope.launch { snackbar.showSnackbar(addedMsg) }
                vm.addFromItem(item, qty, buyUrgent.value)
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
            initialQty = 1.0,
            initialUnit = QuantityUnit.PCS,
            initialUrgent = true,
            onConfirm = { name, qty, unit, urgent ->
                vm.addManual(name, qty, unit, urgent)
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
                initialQty = entry.qty,
                initialUnit = entry.unit,
                initialUrgent = entry.urgent,
                onConfirm = { name, qty, unit, urgent ->
                    vm.updateEntry(id, name, qty, unit, urgent)
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
 */
@Composable
private fun BuySection(
    title: String,
    urgent: Boolean,
    entries: List<BuyUi>,
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

    SpringAccordion(
        expanded = expanded,
        header = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(vertical = 8.dp),
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
                    color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                CountUpText(
                    value = entries.size,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.width(8.dp))
                Icon(
                    Icons.Rounded.KeyboardArrowDown,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.rotate(chevronRotation),
                )
            }
        },
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            entries.forEach { entry ->
                // key по id: remember-аниматоры принадлежат записи, а не слоту
                // колонки — переезд айтема при отметке анимируется.
                key(entry.id) {
                    val restockAction = stringResource(
                        R.string.restock_action,
                        "${formatQty(entry.qty)} ${entry.unit.label()}",
                    )
                    BuyRow(
                        entry = entry,
                        onToggle = { checked -> onToggle(entry, checked, restockAction) },
                        onEdit = { onEdit(entry.id) },
                        onDelete = { onDelete(entry.id) },
                    )
                }
            }
        }
    }
}

// ---------- rows ----------

@Composable
private fun LowRow(item: ItemUi, onBuy: () -> Unit) {
    Card(
        shape = Blok.cardSmall,
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
                Text(
                    item.name,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    item.locationPath,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(Blok.Space.xs))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${formatQty(item.qty)} ${item.unit.label()}",
                        style = blokTabular(MaterialTheme.typography.bodyMedium),
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
    onToggle: (Boolean) -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
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
    val iconColor by animateColorAsState(
        targetValue = iconTarget,
        animationSpec = motionColorSpring(BlokMotion.SOFT_DAMPING, BlokMotion.SOFT_STIFFNESS),
        label = "buyRowIcons",
    )

    Card(
        // Плавная пересадка при переупорядочении: выполненный уезжает
        // в конец секции на пружине.
        modifier = Modifier
            .animatePlacementInColumn()
            .fillMaxWidth()
            .blokCardShadow(Blok.cardSmall),
        shape = Blok.cardSmall,
        colors = CardDefaults.cardColors(containerColor = containerColor),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        ) {
            // Пружинный чекбокс: протяжка-заливка, прорисовка галочки, желе-поп.
            SpringCheckbox(checked = entry.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (entry.done) TextDecoration.LineThrough else null,
                    color = titleColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatQty(entry.qty)} ${entry.unit.label()}",
                        style = blokTabular(MaterialTheme.typography.bodyMedium),
                        color = secondaryColor,
                    )
                    entry.linkedLocationPath?.let { path ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            IconButton(onClick = onEdit) {
                Icon(
                    Icons.Rounded.Edit,
                    contentDescription = stringResource(R.string.edit),
                    tint = iconColor,
                )
            }
            // Удаление записи — необратимое: hold-to-confirm вместо диалога.
            HoldIconButton(
                icon = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.delete),
                onConfirm = onDelete,
                tint = iconColor,
            )
        }
    }
}

// ---------- add / edit entry dialog ----------

@Composable
private fun EntryDialog(
    titleRes: Int,
    initialName: String,
    initialQty: Double,
    initialUnit: QuantityUnit,
    initialUrgent: Boolean,
    onConfirm: (name: String, qty: Double, unit: QuantityUnit, urgent: Boolean) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by rememberSaveable { mutableStateOf(initialName) }
    var qtyText by rememberSaveable { mutableStateOf(formatQty(initialQty)) }
    var unit by rememberSaveable { mutableStateOf(initialUnit) }
    var urgent by rememberSaveable { mutableStateOf(initialUrgent) }
    val qty = parseQty(qtyText)
    val valid = name.isNotBlank() && qty != null && qty!! > 0.0

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
                        label = { Text(stringResource(R.string.buy_qty_label)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
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
            TextButton(enabled = valid, onClick = { onConfirm(name, qty!!, unit, urgent) }) {
                Text(stringResource(if (initialName.isEmpty()) R.string.add else R.string.save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}
