package com.pisheblok.app.ui.locations

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import com.pisheblok.app.R
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.ui.components.EmptyState
import com.pisheblok.app.ui.components.ItemRow
import com.pisheblok.app.ui.components.LocalAppContainer
import com.pisheblok.app.ui.components.SmallChip
import com.pisheblok.app.ui.components.LocationCard
import com.pisheblok.app.ui.components.AddTile
import com.pisheblok.app.ui.components.ConfirmDialog
import com.pisheblok.app.ui.components.QtyDialog

/**
 * Generic drill-down browser: used for the Food tab (rooms -> places -> sections)
 * and the Chemicals tab (floors -> rooms -> places -> sections).
 */
@Composable
fun LocationsScreen(section: Section, title: String) {
    val container = LocalAppContainer.current
    val vm: LocationsViewModel = viewModel(key = section.name) {
        LocationsViewModel(container.repository, section)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.added_to_list)

    var showAddLocation by rememberSaveable { mutableStateOf(false) }
    var editLocation by remember { mutableStateOf<LocationUi?>(null) }
    var showAddItem by rememberSaveable { mutableStateOf(false) }
    var editItem by remember { mutableStateOf<ItemUi?>(null) }
    var buyItem by remember { mutableStateOf<ItemUi?>(null) }
    var buyUrgent by remember { mutableStateOf(true) }
    var deleteLocation by remember { mutableStateOf<LocationUi?>(null) }

    BackHandler(enabled = state.searching) { vm.setSearch(false) }
    BackHandler(enabled = state.depth > 0 && !state.searching) { vm.back() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---------- header ----------
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!state.searching && state.depth > 0) {
                        IconButton(onClick = { vm.back() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (state.searching) {
                        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(150)
                            focusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = state.query,
                            onValueChange = { vm.setQuery(it) },
                            placeholder = { Text(stringResource(R.string.search_hint)) },
                            singleLine = true,
                            shape = MaterialTheme.shapes.small,
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                                .testTag("search_field"),
                        )
                    } else {
                        Text(
                            text = when {
                                state.depth == 0 -> title
                                else -> state.currentName ?: ""
                            },
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                    }
                    IconButton(onClick = {
                        if (state.searching) vm.setSearch(false) else vm.setSearch(true)
                    }) {
                        Icon(
                            if (state.searching) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search),
                        )
                    }
                }
                if (!state.searching && state.depth > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        state.pathNames.forEachIndexed { index, name ->
                            Text(
                                text = name,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (index == state.pathNames.lastIndex)
                                    MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .padding(vertical = 6.dp)
                                    .clickable { vm.backTo(index) },
                            )
                            if (index != state.pathNames.lastIndex) {
                                Text(
                                    text = "›",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.outline,
                                )
                            }
                        }
                    }
                }
            }

            // ---------- body ----------
            if (state.searching) {
                if (state.results.isEmpty()) {
                    EmptyState(
                        emoji = "🔍",
                        title = stringResource(R.string.search_empty),
                        subtitle = "",
                    )
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize(),
                    ) {
                        items(state.results, key = { it.id }) { item ->
                            ItemRow(
                                name = item.name,
                                qty = item.qty,
                                unit = item.unit,
                                category = item.category,
                                expiry = item.expiry,
                                isLow = item.isLow,
                                locationPath = if (item.locationPath.isBlank())
                                    stringResource(R.string.search_place_removed)
                                else item.locationPath,
                                onBuy = { if (!item.deleted) buyItem = item },
                                onEdit = { if (!item.deleted) editItem = item },
                                onDelete = { if (!item.deleted) vm.deleteItem(item.id) },
                                deleted = item.deleted,
                            )
                        }
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    if (state.depth == 0 && state.children.isEmpty()) {
                        item {
                            EmptyState(
                                emoji = if (section == Section.FOOD) "🍳" else "🏠",
                                title = stringResource(
                                    if (section == Section.FOOD) R.string.empty_rooms_title else R.string.empty_floors_title
                                ),
                                subtitle = stringResource(
                                    if (section == Section.FOOD) R.string.empty_rooms_subtitle else R.string.empty_floors_subtitle
                                ),
                            )
                        }
                        item {
                            AddTile(
                                label = stringResource(LocationsViewModel.addLabelRes(section, state.depth)),
                                onClick = { showAddLocation = true },
                            )
                        }
                    } else {
                        if (state.children.isNotEmpty()) {
                            item {
                                Column(
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.animateContentSize(),
                                ) {
                                    state.children.chunked(2).forEach { rowCards ->
                                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                            rowCards.forEach { loc ->
                                                LocationCard(
                                                    icon = loc.icon,
                                                    name = loc.name,
                                                    itemCount = loc.itemCount,
                                                    lowCount = loc.lowCount,
                                                    onClick = { vm.open(loc.id) },
                                                    onLongClick = { editLocation = loc },
                                                    modifier = Modifier.weight(1f),
                                                )
                                            }
                                            if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                                        }
                                    }
                                }
                            }
                        }
                        item {
                            AddTile(
                                label = stringResource(LocationsViewModel.addLabelRes(section, state.depth)),
                                onClick = { showAddLocation = true },
                            )
                        }
                        if (state.depth > 0 && state.itemsHere.isNotEmpty()) {
                            item {
                                Text(
                                    stringResource(R.string.items_header),
                                    style = MaterialTheme.typography.titleMedium,
                                    modifier = Modifier.padding(top = 6.dp),
                                )
                            }
                        }
                        items(state.itemsHere, key = { it.id }) { item ->
                            ItemRow(
                                name = item.name,
                                qty = item.qty,
                                unit = item.unit,
                                category = item.category,
                                expiry = item.expiry,
                                isLow = item.isLow,
                                locationPath = null,
                                onBuy = { buyItem = item; buyUrgent = true },
                                onEdit = { editItem = item },
                                onDelete = { vm.deleteItem(item.id) },
                            )
                        }
                        if (state.depth > 0 && state.children.isEmpty() && state.itemsHere.isEmpty()) {
                            item {
                                EmptyState(
                                    emoji = "📦",
                                    title = stringResource(R.string.empty_items_title),
                                    subtitle = stringResource(R.string.empty_items_subtitle),
                                )
                            }
                        }
                    }
                }
            }
        }

        if (state.depth > 0 && !state.searching) {
            ExtendedFloatingActionButton(
                onClick = { showAddItem = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_item)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp),
            )
        }
        SnackbarHost(
            hostState = snackbar,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }

    // ---------- dialogs ----------
    if (showAddLocation) {
        AddLocationDialog(
            titleRes = LocationsViewModel.addLabelRes(section, state.depth),
            iconSet = LocationsViewModel.iconSetFor(section, state.depth),
            initialName = null,
            initialIcon = null,
            onConfirm = { name, icon ->
                vm.addLocation(name, icon)
                showAddLocation = false
            },
            onDismiss = { showAddLocation = false },
        )
    }
    editLocation?.let { loc ->
        AddLocationDialog(
            titleRes = R.string.edit,
            iconSet = LocationsViewModel.iconSetFor(section, state.depth),
            initialName = loc.name,
            initialIcon = loc.icon,
            onConfirm = { name, icon ->
                vm.updateLocation(loc.id, name, icon)
                editLocation = null
            },
            onDismiss = { editLocation = null },
            onDelete = {
                editLocation = null
                deleteLocation = loc
            },
        )
    }
    deleteLocation?.let { loc ->
        ConfirmDialog(
            title = stringResource(R.string.delete_location_title, loc.name),
            message = stringResource(R.string.delete_location_message),
            onConfirm = {
                vm.deleteLocation(loc.id)
                deleteLocation = null
            },
            onDismiss = { deleteLocation = null },
        )
    }
    if (showAddItem) {
        val locationId = state.currentId
        if (locationId != null) {
            EditItemSheet(
                section = section,
                editing = null,
                suggestions = suggestions,
                onDismiss = { showAddItem = false },
                onSave = { name, qty, unit, category, expiry, minQty, barcode ->
                    vm.addItem(locationId, name, qty, unit, category, expiry, minQty, barcode)
                    showAddItem = false
                },
            )
        } else {
            showAddItem = false
        }
    }
    editItem?.let { item ->
        EditItemSheet(
            section = section,
            editing = item,
            suggestions = suggestions,
            onDismiss = { editItem = null },
            onSave = { name, qty, unit, category, expiry, minQty, barcode ->
                vm.updateItem(item.id, name, qty, unit, category, expiry, minQty, barcode)
                editItem = null
            },
        )
    }
    buyItem?.let { item ->
        QtyDialog(
            title = stringResource(R.string.buy_qty_question),
            subtitle = item.name,
            defaultQty = 1.0,
            unit = item.unit,
            confirmText = stringResource(R.string.add_to_list),
            onConfirm = { qty ->
                scope.launch { snackbar.showSnackbar(addedMsg) }
                vm.addBuy(item, qty, buyUrgent)
                buyItem = null
            },
            onDismiss = { buyItem = null },
            urgent = buyUrgent,
            onUrgentChange = { buyUrgent = it },
        )
    }
}
