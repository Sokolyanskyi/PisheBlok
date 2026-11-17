package com.pisheblok.app.ui.locations

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pisheblok.app.R
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.ui.components.AddTile
import com.pisheblok.app.ui.components.EmptyState
import com.pisheblok.app.ui.components.ItemRow
import com.pisheblok.app.ui.components.LocalAppContainer
import com.pisheblok.app.ui.components.LocationCard
import com.pisheblok.app.ui.components.QtyDialog
import com.pisheblok.app.ui.motion.LocalMotionEnabled
import com.pisheblok.app.ui.motion.staggerIn
import kotlinx.coroutines.launch

/**
 * Generic drill-down browser: used for the Food tab (rooms -> places -> sections)
 * and the Chemicals tab (floors -> rooms -> places -> sections).
 *
 * Поиск живёт в отдельном flow — набор текста не пересобирает список.
 * Уход на уровень глубже — выезд контента снизу (пружина 500/0.80).
 * Колбэки списков стабильные (remember) — элементы LazyColumn не
 * рекомпозируют при каждом символе поиска.
 */
@Composable
fun LocationsScreen(section: Section, title: String) {
    val container = LocalAppContainer.current
    val vm: LocationsViewModel = viewModel(key = section.name) {
        LocationsViewModel(container.repository, section)
    }
    val state by vm.state.collectAsStateWithLifecycle()
    val search by vm.search.collectAsStateWithLifecycle()
    val suggestions by vm.suggestions.collectAsStateWithLifecycle()
    val motionOn = LocalMotionEnabled.current

    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val addedMsg = stringResource(R.string.added_to_list)

    // Состояния-холдеры: лямбды ниже захватывают их объекты и потому
    // стабильны между рекомпозициями.
    var showAddLocation by rememberSaveable { mutableStateOf(false) }
    val editLocation = remember { mutableStateOf<LocationUi?>(null) }
    var showAddItem by rememberSaveable { mutableStateOf(false) }
    val editItem = remember { mutableStateOf<ItemUi?>(null) }
    val buyItem = remember { mutableStateOf<ItemUi?>(null) }
    val buyUrgent = remember { mutableStateOf(true) }

    // ---------- стабильные колбэки для списков ----------
    val onOpen: (String) -> Unit = remember { { vm.open(it) } }
    val onEditLocation: (LocationUi) -> Unit = remember { { editLocation.value = it } }
    val onAddLocation: () -> Unit = remember { { showAddLocation = true } }
    val onAddItem: () -> Unit = remember { { showAddItem = true } }
    val onBuyFromList: (ItemUi) -> Unit = remember { { buyItem.value = it; buyUrgent.value = true } }
    val onBuySearch: (ItemUi) -> Unit = remember { { if (!it.deleted) buyItem.value = it } }
    val onEditFromList: (ItemUi) -> Unit = remember { { editItem.value = it } }
    val onEditSearch: (ItemUi) -> Unit = remember { { if (!it.deleted) editItem.value = it } }
    val onDeleteFromList: (ItemUi) -> Unit = remember { { vm.deleteItem(it.id) } }
    val onDeleteSearch: (ItemUi) -> Unit = remember { { if (!it.deleted) vm.deleteItem(it.id) } }

    BackHandler(enabled = search.active) { vm.setSearch(false) }
    BackHandler(enabled = state.depth > 0 && !search.active) { vm.back() }

    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize()) {
            // ---------- header: каскадный влёт статичных секций ----------
            Column(Modifier.padding(horizontal = 16.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.staggerIn(0),
                ) {
                    if (!search.active && state.depth > 0) {
                        IconButton(onClick = { vm.back() }) {
                            Icon(
                                Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = stringResource(R.string.cd_back),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                    }
                    if (search.active) {
                        val focusRequester = remember { androidx.compose.ui.focus.FocusRequester() }
                        LaunchedEffect(Unit) {
                            kotlinx.coroutines.delay(150)
                            focusRequester.requestFocus()
                        }
                        OutlinedTextField(
                            value = search.query,
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
                        if (search.active) vm.setSearch(false) else vm.setSearch(true)
                    }) {
                        Icon(
                            if (search.active) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = stringResource(R.string.cd_search),
                        )
                    }
                }
                if (!search.active && state.depth > 0) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .staggerIn(1),
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

            // ---------- body: выезд снизу при уходе вглубь / в поиск ----------
            // Спецификации захватываем до лямбд перехода.
            val fade = if (motionOn) tween<Float>(220) else snap<Float>()
            val rise = if (motionOn)
                androidx.compose.animation.core.spring<IntOffset>(dampingRatio = 0.8f, stiffness = 500f)
            else snap<IntOffset>()

            AnimatedContent(
                targetState = search.active to state.depth,
                transitionSpec = {
                    val entering = targetState.first || targetState.second > initialState.second
                    if (entering) {
                        slideInVertically(rise) { it / 4 } + fadeIn(fade) togetherWith
                            fadeOut(fade) + slideOutVertically(rise) { -it / 8 }
                    } else {
                        fadeIn(fade) + slideInVertically(rise) { -it / 8 } togetherWith
                            fadeOut(fade) + slideOutVertically(rise) { it / 4 }
                    }
                },
                label = "drilldown",
            ) { (searching, _) ->
                if (searching) {
                    SearchResults(
                        search = search,
                        onBuy = onBuySearch,
                        onEdit = onEditSearch,
                        onDelete = onDeleteSearch,
                    )
                } else {
                    LocationsBody(
                        section = section,
                        state = state,
                        onOpen = onOpen,
                        onEditLocation = onEditLocation,
                        onAddLocation = onAddLocation,
                        onAddItem = onAddItem,
                        onBuy = onBuyFromList,
                        onEdit = onEditFromList,
                        onDelete = onDeleteFromList,
                    )
                }
            }
        }

        if (state.depth > 0 && !search.active) {
            ExtendedFloatingActionButton(
                onClick = onAddItem,
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text(stringResource(R.string.add_item)) },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(20.dp)
                    .testTag("add_item_fab"),
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
    editLocation.value?.let { loc ->
        AddLocationDialog(
            titleRes = R.string.edit,
            iconSet = LocationsViewModel.iconSetFor(section, state.depth),
            initialName = loc.name,
            initialIcon = loc.icon,
            onConfirm = { name, icon ->
                vm.updateLocation(loc.id, name, icon)
                editLocation.value = null
            },
            onDismiss = { editLocation.value = null },
            onDelete = {
                // Hold-to-confirm уже отработал внутри диалога — удаляем сразу.
                editLocation.value = null
                vm.deleteLocation(loc.id)
            },
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
    editItem.value?.let { item ->
        EditItemSheet(
            section = section,
            editing = item,
            suggestions = suggestions,
            onDismiss = { editItem.value = null },
            onSave = { name, qty, unit, category, expiry, minQty, barcode ->
                vm.updateItem(item.id, name, qty, unit, category, expiry, minQty, barcode)
                editItem.value = null
            },
        )
    }
    buyItem.value?.let { item ->
        QtyDialog(
            title = stringResource(R.string.buy_qty_question),
            subtitle = item.name,
            defaultQty = 1.0,
            unit = item.unit,
            confirmText = stringResource(R.string.add_to_list),
            onConfirm = { qty ->
                scope.launch { snackbar.showSnackbar(addedMsg) }
                vm.addBuy(item, qty, buyUrgent.value)
                buyItem.value = null
            },
            onDismiss = { buyItem.value = null },
            urgent = buyUrgent.value,
            onUrgentChange = { buyUrgent.value = it },
        )
    }
}

@Composable
private fun SearchResults(
    search: SearchState,
    onBuy: (ItemUi) -> Unit,
    onEdit: (ItemUi) -> Unit,
    onDelete: (ItemUi) -> Unit,
) {
    if (search.results.isEmpty()) {
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
            items(search.results, key = { it.id }) { item ->
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
                    onBuy = { onBuy(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                    deleted = item.deleted,
                )
            }
        }
    }
}

@Composable
private fun LocationsBody(
    section: Section,
    state: BrowserState,
    onOpen: (String) -> Unit,
    onEditLocation: (LocationUi) -> Unit,
    onAddLocation: () -> Unit,
    onAddItem: () -> Unit,
    onBuy: (ItemUi) -> Unit,
    onEdit: (ItemUi) -> Unit,
    onDelete: (ItemUi) -> Unit,
) {
    LazyColumn(
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 100.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (state.depth == 0 && state.children.isEmpty()) {
            item(key = "empty") {
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
            item(key = "add") {
                AddTile(
                    label = stringResource(LocationsViewModel.addLabelRes(section, state.depth)),
                    onClick = onAddLocation,
                )
            }
        } else {
            if (state.children.isNotEmpty()) {
                item(key = "children") {
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
                                        onClick = { onOpen(loc.id) },
                                        onLongClick = { onEditLocation(loc) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (rowCards.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }
            item(key = "add") {
                AddTile(
                    label = stringResource(LocationsViewModel.addLabelRes(section, state.depth)),
                    onClick = onAddLocation,
                )
            }
            if (state.depth > 0 && state.itemsHere.isNotEmpty()) {
                item(key = "items_header") {
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
                    onBuy = { onBuy(item) },
                    onEdit = { onEdit(item) },
                    onDelete = { onDelete(item) },
                )
            }
            if (state.depth > 0 && state.children.isEmpty() && state.itemsHere.isEmpty()) {
                item(key = "empty_items") {
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
