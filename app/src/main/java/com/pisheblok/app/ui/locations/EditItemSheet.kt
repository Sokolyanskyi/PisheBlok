package com.pisheblok.app.ui.locations

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.pisheblok.app.R
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.data.model.formatQty
import com.pisheblok.app.ui.components.DropdownField
import com.pisheblok.app.ui.components.formatDate
import com.pisheblok.app.ui.components.label
import com.pisheblok.app.ui.components.parseQty
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Converts UTC-midnight millis from DatePicker to local-midnight millis. */
fun normalizeToLocalMidnight(utcMillis: Long): Long {
    val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/** Add / edit item form in a bottom sheet. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditItemSheet(
    section: Section,
    editing: ItemUi?,
    suggestions: List<ItemSuggestion>,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, unit: QuantityUnit, category: ItemCategory, expiry: Long?, minQty: Double, barcode: String?) -> Unit,
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var name by rememberSaveable { mutableStateOf(editing?.name ?: "") }
    var qtyText by rememberSaveable { mutableStateOf(formatQty(editing?.qty ?: 1.0)) }
    var minQtyText by rememberSaveable { mutableStateOf(formatQty(editing?.minQty ?: 0.0)) }
    var unit by rememberSaveable { mutableStateOf(editing?.unit ?: QuantityUnit.PCS) }
    var category by rememberSaveable {
        mutableStateOf(editing?.category ?: ItemCategory.forSection(section).first())
    }
    var expiryRaw by rememberSaveable { mutableStateOf(editing?.expiry ?: -1L) }
    val expiry: Long? = if (expiryRaw < 0) null else expiryRaw
    var barcodeRaw by rememberSaveable { mutableStateOf(editing?.barcode ?: "") }
    val barcode: String? = barcodeRaw.ifBlank { null }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }

    var scanMissed by rememberSaveable { mutableStateOf(false) }
    val scanLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) { res ->
        if (res.resultCode == android.app.Activity.RESULT_OK) {
            barcodeRaw = res.data?.getStringExtra("barcode") ?: ""
            val productName = res.data?.getStringExtra("productName").orEmpty()
            scanMissed = productName.isBlank()
            if (productName.isNotBlank()) name = productName
        }
    }

    val qty = parseQty(qtyText)
    val minQty = parseQty(minQtyText) ?: 0.0
    val valid = name.isNotBlank() && qty != null && qty!! > 0.0
    val filteredSuggestions = suggestions
        .filter { s -> s.name.contains(name.trim(), ignoreCase = true) && !s.name.equals(name.trim(), ignoreCase = true) }
        .take(4)

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                stringResource(if (editing == null) R.string.add_item else R.string.edit_item),
                style = MaterialTheme.typography.titleLarge,
            )
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text(stringResource(R.string.item_name)) },
                singleLine = true,
                supportingText = {
                    if (barcode != null) Text(stringResource(R.string.barcode_label, barcode))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            if (filteredSuggestions.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    filteredSuggestions.forEach { s ->
                        androidx.compose.material3.SuggestionChip(
                            onClick = {
                                name = s.name
                                unit = s.unit
                                category = s.category
                                minQtyText = formatQty(s.minQty)
                            },
                            label = { Text(s.name, maxLines = 1) },
                        )
                    }
                }
            }
            OutlinedButton(
                onClick = {
                    scanMissed = false
                    scanLauncher.launch(android.content.Intent(context, com.pisheblok.app.scanner.ScannerActivity::class.java))
                },
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            ) {
                Icon(Icons.Rounded.QrCodeScanner, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(stringResource(R.string.scan_barcode))
            }
            if (scanMissed) {
                Text(
                    stringResource(R.string.scan_not_found),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = qtyText,
                    onValueChange = { qtyText = it },
                    label = { Text(stringResource(R.string.item_qty)) },
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
            DropdownField(
                label = stringResource(R.string.category_label),
                options = ItemCategory.forSection(section),
                selected = category,
                optionLabel = { it.label() },
                onSelect = { category = it },
            )
            if (section == Section.FOOD) {
                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.small,
                ) {
                    Icon(Icons.Rounded.CalendarMonth, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        expiry?.let { formatDate(it) }
                            ?: stringResource(R.string.item_expiry) + " · " + stringResource(R.string.expiry_optional),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.weight(1f))
                    if (expiry != null) {
                        IconButton(onClick = { expiryRaw = -1L }) {
                            Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.expiry_clear))
                        }
                    }
                }
            }
            OutlinedTextField(
                value = minQtyText,
                onValueChange = { minQtyText = it },
                label = { Text(stringResource(R.string.item_min_qty)) },
                supportingText = { Text(stringResource(R.string.item_min_qty_hint)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.small,
            )
            Button(
                enabled = valid,
                onClick = {
                    onSave(name, qty!!, unit, category, expiry, minQty, barcode)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Text(stringResource(R.string.save))
            }
            Spacer(Modifier.navigationBarsPadding())
            Spacer(Modifier.height(8.dp))
        }
    }

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(
            initialSelectedDateMillis = expiry ?: System.currentTimeMillis()
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    pickerState.selectedDateMillis?.let { expiryRaw = normalizeToLocalMidnight(it) }
                    showDatePicker = false
                }) { Text(stringResource(R.string.ok)) }
            },
            dismissButton = {
                TextButton(onClick = {
                    expiryRaw = -1L
                    showDatePicker = false
                }) { Text(stringResource(R.string.expiry_clear)) }
            },
        ) {
            DatePicker(state = pickerState)
        }
    }
}
