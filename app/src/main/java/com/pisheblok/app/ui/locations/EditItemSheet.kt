package com.pisheblok.app.ui.locations

import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
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
import com.pisheblok.app.ui.motion.components.JellyScale
import com.pisheblok.app.ui.theme.Blok
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset

/** Converts UTC-midnight millis from DatePicker to local-midnight millis. */
fun normalizeToLocalMidnight(utcMillis: Long): Long {
    val date = Instant.ofEpochMilli(utcMillis).atZone(ZoneOffset.UTC).toLocalDate()
    return date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
}

/**
 * Add / edit item form in a bottom sheet.
 *
 * Лист открывается сразу на всю высоту: «Сохранить» закреплена внизу,
 * сканер — иконка в поле имени, а редкие поля (категория, срок, порог)
 * живут в раскрывающемся блоке «Дополнительно».
 */
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
        // «Овощи и фрукты» по умолчанию вешали категорию на крекер — теперь «Другое».
        mutableStateOf(editing?.category ?: ItemCategory.OTHER)
    }
    var expiryRaw by rememberSaveable { mutableStateOf(editing?.expiry ?: -1L) }
    val expiry: Long? = if (expiryRaw < 0) null else expiryRaw
    var barcodeRaw by rememberSaveable { mutableStateOf(editing?.barcode ?: "") }
    val barcode: String? = barcodeRaw.ifBlank { null }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var advancedExpanded by rememberSaveable { mutableStateOf(editing != null) }

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
        .take(3)

    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(Modifier.fillMaxWidth().imePadding()) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = Blok.Space.xl),
                verticalArrangement = Arrangement.spacedBy(Blok.Space.m),
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
                    trailingIcon = {
                        IconButton(onClick = {
                            scanMissed = false
                            scanLauncher.launch(
                                android.content.Intent(context, com.pisheblok.app.scanner.ScannerActivity::class.java)
                            )
                        }) {
                            Icon(
                                Icons.Rounded.QrCodeScanner,
                                contentDescription = stringResource(R.string.scan_barcode),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                        }
                    },
                    supportingText = {
                        when {
                            barcode != null -> Text(stringResource(R.string.barcode_label, barcode))
                            scanMissed -> Text(
                                stringResource(R.string.scan_not_found),
                                color = MaterialTheme.colorScheme.error,
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_name_field"),
                    shape = MaterialTheme.shapes.small,
                )
                if (filteredSuggestions.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .padding(end = Blok.Space.l),
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
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = qtyText,
                        onValueChange = { qtyText = it },
                        label = { Text(stringResource(R.string.item_qty)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("item_qty_field"),
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
                // Желе-шкала под полем количества: поле — источник истины,
                // шкала синхронизирована в обе стороны.
                val (qtyMax, qtyStep) = when (unit) {
                    QuantityUnit.PCS, QuantityUnit.PACK, QuantityUnit.BOTTLE, QuantityUnit.BOX -> 12f to 1f
                    QuantityUnit.KG, QuantityUnit.L -> 10f to 0.5f
                    QuantityUnit.G, QuantityUnit.ML -> 2000f to 50f
                }
                JellyScale(
                    value = qty?.toFloat(),
                    onValueChange = { qtyText = formatQty(it.toDouble()) },
                    min = 0f,
                    max = qtyMax,
                    step = qtyStep,
                    endLabels = true,
                )

                // ---------- Дополнительно ----------
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(Blok.pill)
                        .clickable { advancedExpanded = !advancedExpanded }
                        .padding(vertical = 10.dp),
                ) {
                    Text(
                        stringResource(R.string.item_more),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f),
                    )
                    Icon(
                        if (advancedExpanded) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (advancedExpanded) {
                    Column(verticalArrangement = Arrangement.spacedBy(Blok.Space.m)) {
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
                                        Icon(
                                            Icons.Rounded.Close,
                                            contentDescription = stringResource(R.string.expiry_clear),
                                        )
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
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("item_min_field"),
                            shape = MaterialTheme.shapes.small,
                        )
                    }
                }
                Spacer(Modifier.height(Blok.Space.s))
            }

            // ---------- Закреплённая панель сохранения ----------
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerLow,
                tonalElevation = 3.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = Blok.Space.xl, vertical = Blok.Space.m)
                ) {
                    Button(
                        enabled = valid,
                        onClick = { onSave(name, qty!!, unit, category, expiry, minQty, barcode) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Text(stringResource(R.string.save))
                    }
                }
            }
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
