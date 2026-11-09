package com.pisheblok.app.ui.shopping

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.pisheblok.app.ui.components.ConfirmDialog
import com.pisheblok.app.ui.components.parseQty
import com.pisheblok.app.ui.locations.ItemUi
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

    var buyForItem by remember { mutableStateOf<ItemUi?>(null) }
    var buyUrgent by remember { mutableStateOf(true) }
    var showManualAdd by rememberSaveable { mutableStateOf(false) }
    var editEntryId by remember { mutableStateOf<String?>(null) }
    var deleteEntryId by remember { mutableStateOf<String?>(null) }

    val addedMsg = stringResource(R.string.added_to_list)
    val restockMsg = stringResource(R.string.restock_snack)

    val onToggleEntry: (BuyUi, Boolean, String) -> Unit = { entry, checked, restockAction ->
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

    Box(Modifier.fillMaxSize()) {
        LazyColumn(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            // ---------- running low ----------
            item {
                Text(
                    stringResource(R.string.section_running_low),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            if (state.low.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🧊",
                        title = stringResource(R.string.empty_low_title),
                        subtitle = stringResource(R.string.empty_low_subtitle),
                    )
                }
            } else {
                items(state.low, key = { "low-${it.id}" }) { item ->
                    LowRow(item = item, onBuy = { buyForItem = item; buyUrgent = true })
                }
            }

            // ---------- buy list ----------
            item {
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
                        TextButton(onClick = { vm.clearDone() }) {
                            Text(stringResource(R.string.clear_done))
                        }
                    }
                }
            }
            if (state.urgent.isEmpty() && state.later.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🛒",
                        title = stringResource(R.string.empty_buy_title),
                        subtitle = stringResource(R.string.empty_buy_subtitle),
                    )
                }
            } else {
                if (state.urgent.isNotEmpty()) {
                    item {
                        BuySectionLabel(stringResource(R.string.shopping_urgent), true)
                    }
                    items(state.urgent, key = { "buy-${it.id}" }) { entry ->
                        val restockAction = stringResource(R.string.restock_action, "${formatQty(entry.qty)} ${entry.unit.label()}")
                        BuyRow(
                            entry = entry,
                            onToggle = { checked -> onToggleEntry(entry, checked, restockAction) },
                            onEdit = { editEntryId = entry.id },
                            onDelete = { deleteEntryId = entry.id },
                        )
                    }
                }
                if (state.later.isNotEmpty()) {
                    item {
                        BuySectionLabel(stringResource(R.string.shopping_later), false)
                    }
                    items(state.later, key = { "buy-${it.id}" }) { entry ->
                        val restockAction = stringResource(R.string.restock_action, "${formatQty(entry.qty)} ${entry.unit.label()}")
                        BuyRow(
                            entry = entry,
                            onToggle = { checked -> onToggleEntry(entry, checked, restockAction) },
                            onEdit = { editEntryId = entry.id },
                            onDelete = { deleteEntryId = entry.id },
                        )
                    }
                }
            }
        }

        ExtendedFloatingActionButton(
            onClick = { showManualAdd = true },
            icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
            text = { Text(stringResource(R.string.add)) },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(20.dp),
        )
        SnackbarHost(hostState = snackbar, modifier = Modifier.align(Alignment.BottomCenter))
    }

    // ---------- dialogs ----------
    buyForItem?.let { item ->
        QtyDialog(
            title = stringResource(R.string.buy_qty_question),
            subtitle = item.name,
            defaultQty = 1.0,
            unit = item.unit,
            confirmText = stringResource(R.string.add_to_list),
            onConfirm = { qty ->
                scope.launch { snackbar.showSnackbar(addedMsg) }
                vm.addFromItem(item, qty, buyUrgent)
                buyForItem = null
            },
            onDismiss = { buyForItem = null },
            urgent = buyUrgent,
            onUrgentChange = { buyUrgent = it },
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
    editEntryId?.let { id ->
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
                    editEntryId = null
                },
                onDismiss = { editEntryId = null },
            )
        } else {
            editEntryId = null
        }
    }
    deleteEntryId?.let { id ->
        val entry = state.urgent.firstOrNull { it.id == id } ?: state.later.firstOrNull { it.id == id }
        if (entry != null) {
            ConfirmDialog(
                title = stringResource(R.string.delete_entry_confirm, entry.name),
                message = null,
                confirmText = stringResource(R.string.delete),
                onConfirm = {
                    vm.deleteEntry(id)
                    deleteEntryId = null
                },
                onDismiss = { deleteEntryId = null },
            )
        } else {
            deleteEntryId = null
        }
    }
}

// ---------- rows ----------

@Composable
private fun BuySectionLabel(text: String, urgent: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(top = 4.dp),
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
            text,
            style = MaterialTheme.typography.titleSmall,
            color = if (urgent) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun LowRow(item: ItemUi, onBuy: () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 16.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
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
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${formatQty(item.qty)} ${item.unit.label()}",
                        style = MaterialTheme.typography.bodyMedium,
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
    Card(
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 4.dp, top = 6.dp, bottom = 6.dp, end = 6.dp),
        ) {
            Checkbox(checked = entry.done, onCheckedChange = onToggle)
            Column(Modifier.weight(1f)) {
                Text(
                    entry.name,
                    style = MaterialTheme.typography.titleMedium,
                    textDecoration = if (entry.done) TextDecoration.LineThrough else null,
                    color = if (entry.done) MaterialTheme.colorScheme.onSurfaceVariant
                    else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "${formatQty(entry.qty)} ${entry.unit.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    entry.linkedLocationPath?.let { path ->
                        Spacer(Modifier.width(8.dp))
                        Text(
                            path,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
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
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Rounded.Delete,
                    contentDescription = stringResource(R.string.delete),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
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
