package com.pisheblok.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pisheblok.app.AppContainer
import com.pisheblok.app.R
import com.pisheblok.app.data.model.ExpiryState
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.formatQty
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.ChronoUnit

val LocalAppContainer = androidx.compose.runtime.staticCompositionLocalOf<AppContainer> {
    error("AppContainer not provided")
}

// ---------- Emoji icon sets for locations ----------

val ICONS_FLOORS = listOf("🏠", "🏢", "🪜", "🛗", "🔑")
val ICONS_ROOMS = listOf("🍳", "🛋", "🚪", "📦", "🧺", "🌿", "🥶", "🪴", "🧊", "🥫", "🧃", "🏠")
val ICONS_PLACES = listOf("❄️", "🧊", "🚪", "🗄️", "🧺", "📦", "🥫", "🧴", "🪣", "🧹", "🧼", "🚿", "🪟", "🛁")

// ---------- Helpers ----------

fun parseQty(text: String): Double? =
    text.replace(',', '.').trim().toDoubleOrNull()?.takeIf { it >= 0 }

fun expiryStateOf(expiry: Long?): ExpiryState {
    if (expiry == null) return ExpiryState.None
    val zone = ZoneId.systemDefault()
    val today = LocalDate.now(zone)
    val date = Instant.ofEpochMilli(expiry).atZone(zone).toLocalDate()
    val days = ChronoUnit.DAYS.between(today, date)
    return when {
        days < 0 -> ExpiryState.Expired
        days <= 3 -> ExpiryState.Soon
        else -> ExpiryState.Ok(days)
    }
}

fun formatDate(millis: Long): String {
    val date = Instant.ofEpochMilli(millis).atZone(ZoneId.systemDefault()).toLocalDate()
    return date.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
}

@Composable
fun QuantityUnit.label(): String = when (this) {
    QuantityUnit.PCS -> stringResource(R.string.unit_pcs)
    QuantityUnit.PACK -> stringResource(R.string.unit_pack)
    QuantityUnit.BOTTLE -> stringResource(R.string.unit_bottle)
    QuantityUnit.BOX -> stringResource(R.string.unit_box)
    QuantityUnit.KG -> stringResource(R.string.unit_kg)
    QuantityUnit.G -> stringResource(R.string.unit_g)
    QuantityUnit.L -> stringResource(R.string.unit_l)
    QuantityUnit.ML -> stringResource(R.string.unit_ml)
}

@Composable
fun ItemCategory.label(): String = when (this) {
    ItemCategory.VEG -> stringResource(R.string.cat_veg)
    ItemCategory.MEAT -> stringResource(R.string.cat_meat)
    ItemCategory.DAIRY -> stringResource(R.string.cat_dairy)
    ItemCategory.FROZEN -> stringResource(R.string.cat_frozen)
    ItemCategory.GRAINS -> stringResource(R.string.cat_grains)
    ItemCategory.CANNED -> stringResource(R.string.cat_canned)
    ItemCategory.DRINKS -> stringResource(R.string.cat_drinks)
    ItemCategory.SPICES -> stringResource(R.string.cat_spices)
    ItemCategory.SWEETS -> stringResource(R.string.cat_sweets)
    ItemCategory.SNACKS -> stringResource(R.string.cat_snacks)
    ItemCategory.DISH -> stringResource(R.string.cat_dish)
    ItemCategory.CLEAN -> stringResource(R.string.cat_clean)
    ItemCategory.LAUNDRY -> stringResource(R.string.cat_laundry)
    ItemCategory.HYGIENE -> stringResource(R.string.cat_hygiene)
    ItemCategory.CARE -> stringResource(R.string.cat_care)
    ItemCategory.OTHER -> stringResource(R.string.cat_other)
}

// ---------- Small chips & badges ----------

@Composable
fun SmallChip(
    text: String,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(8.dp),
        color = containerColor,
        contentColor = contentColor,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ExpiryBadge(expiry: Long?, modifier: Modifier = Modifier) {
    when (val state = expiryStateOf(expiry)) {
        is ExpiryState.None -> {}
        is ExpiryState.Expired -> SmallChip(
            stringResource(R.string.expired_badge),
            MaterialTheme.colorScheme.errorContainer,
            MaterialTheme.colorScheme.onErrorContainer,
            modifier,
        )
        is ExpiryState.Soon -> SmallChip(
            stringResource(R.string.expires_soon_badge),
            MaterialTheme.colorScheme.tertiaryContainer,
            MaterialTheme.colorScheme.onTertiaryContainer,
            modifier,
        )
        is ExpiryState.Ok -> SmallChip(
            stringResource(R.string.expires_days_fmt, state.daysLeft),
            MaterialTheme.colorScheme.secondaryContainer,
            MaterialTheme.colorScheme.onSecondaryContainer,
            modifier,
        )
    }
}

// ---------- Location card (grid) ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun LocationCard(
    icon: String,
    name: String,
    itemCount: Int,
    lowCount: Int,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(icon, fontSize = 26.sp)
            }
            Spacer(Modifier.height(10.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = if (itemCount == 0) stringResource(R.string.items_count_zero)
                else androidx.compose.ui.res.pluralStringResource(R.plurals.items_count, itemCount, itemCount),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (lowCount > 0) {
                Spacer(Modifier.height(8.dp))
                SmallChip(
                    androidx.compose.ui.res.pluralStringResource(R.plurals.low_inside, lowCount, lowCount),
                    MaterialTheme.colorScheme.errorContainer,
                    MaterialTheme.colorScheme.onErrorContainer,
                )
            }
        }
    }
}

@Composable
fun AddTile(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .height(132.dp)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.0f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.5.dp,
            MaterialTheme.colorScheme.outlineVariant,
        ),
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(
                Icons.Rounded.Add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(6.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ---------- Item row ----------

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ItemRow(
    name: String,
    qty: Double,
    unit: QuantityUnit,
    category: ItemCategory,
    expiry: Long?,
    isLow: Boolean,
    locationPath: String?,
    onBuy: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
    deleted: Boolean = false,
) {
    var confirmDelete by rememberSaveable { mutableStateOf(false) }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(
            containerColor = if (deleted) MaterialTheme.colorScheme.surfaceVariant
            else MaterialTheme.colorScheme.surfaceContainer
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .combinedClickable(onClick = onEdit)
                .padding(start = 16.dp, top = 12.dp, bottom = 12.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (deleted) {
                        Spacer(Modifier.width(8.dp))
                        SmallChip(
                            stringResource(R.string.search_deleted_badge),
                            MaterialTheme.colorScheme.outlineVariant,
                            MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (isLow) {
                        Spacer(Modifier.width(8.dp))
                        SmallChip(
                            stringResource(R.string.low_stock_badge),
                            MaterialTheme.colorScheme.errorContainer,
                            MaterialTheme.colorScheme.onErrorContainer,
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        "${formatQty(qty)} ${unit.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "· ${category.label()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (locationPath != null) {
                    Text(
                        locationPath,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    ExpiryBadge(expiry)
                }
            }
            if (!deleted) {
                IconButton(onClick = onBuy) {
                    Icon(
                        Icons.Rounded.ShoppingCart,
                        contentDescription = stringResource(R.string.add_to_list),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(onClick = { confirmDelete = true }) {
                    Icon(
                        Icons.Rounded.Delete,
                        contentDescription = stringResource(R.string.delete),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
    if (confirmDelete) {
        ConfirmDialog(
            title = stringResource(R.string.delete_item_confirm, name),
            message = null,
            onConfirm = { confirmDelete = false; onDelete() },
            onDismiss = { confirmDelete = false },
        )
    }
}

// ---------- Dialogs ----------

@Composable
fun ConfirmDialog(
    title: String,
    message: String?,
    confirmText: String = stringResource(R.string.delete),
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { if (message != null) Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText, color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

/** Urgent / later selector chips for shopping entries. */
@Composable
fun PriorityChips(
    urgent: Boolean,
    onUrgentChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        androidx.compose.material3.FilterChip(
            selected = urgent,
            onClick = { onUrgentChange(true) },
            label = { Text(stringResource(R.string.shopping_urgent)) },
        )
        androidx.compose.material3.FilterChip(
            selected = !urgent,
            onClick = { onUrgentChange(false) },
            label = { Text(stringResource(R.string.shopping_later)) },
        )
    }
}

/** "How many to buy?" dialog with a quantity field. */
@Composable
fun QtyDialog(
    title: String,
    subtitle: String,
    defaultQty: Double,
    unit: QuantityUnit,
    confirmText: String,
    onConfirm: (Double) -> Unit,
    onDismiss: () -> Unit,
    urgent: Boolean = true,
    onUrgentChange: (Boolean) -> Unit = {},
) {
    var text by rememberSaveable { mutableStateOf(formatQty(defaultQty)) }
    val parsed = parseQty(text)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column {
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text(stringResource(R.string.quantity)) },
                    suffix = { Text(unit.label()) },
                    singleLine = true,
                    isError = parsed == null || parsed == 0.0,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qty_field"),
                )
                Spacer(Modifier.height(10.dp))
                PriorityChips(urgent = urgent, onUrgentChange = onUrgentChange)
            }
        },
        confirmButton = {
            TextButton(
                enabled = parsed != null && parsed!! > 0.0,
                onClick = { onConfirm(parsed!!); },
            ) { Text(confirmText) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        },
    )
}

// ---------- Pickers ----------

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EmojiPicker(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        options.forEach { emoji ->
            val isSelected = emoji == selected
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier.clickable { onSelect(emoji) },
            ) {
                Text(
                    emoji,
                    fontSize = 22.sp,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                )
            }
        }
    }
}

/** Generic dropdown field used for unit & category selection. */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun <T> DropdownField(
    label: String,
    options: List<T>,
    selected: T,
    optionLabel: @Composable (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = modifier,
    ) {
        OutlinedTextField(
            value = optionLabel(selected),
            onValueChange = {},
            readOnly = true,
            label = { Text(label) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded) },
            modifier = Modifier
                .fillMaxWidth()
                .menuAnchor(),
            shape = MaterialTheme.shapes.small,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(optionLabel(option)) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    },
                )
            }
        }
    }
}

// ---------- Empty state ----------

@Composable
fun EmptyState(
    emoji: String,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(88.dp)
                .background(MaterialTheme.colorScheme.primaryContainer, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(emoji, fontSize = 40.sp)
        }
        Spacer(Modifier.height(16.dp))
        Text(
            title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

/** Small edit/delete entry point used by location long-press menus. */
@Composable
fun rememberEditIcon(): androidx.compose.ui.graphics.vector.ImageVector = Icons.Rounded.Edit
