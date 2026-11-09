package com.pisheblok.app.ui.locations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pisheblok.app.R
import com.pisheblok.app.data.local.ItemEntity
import com.pisheblok.app.data.local.LocationEntity
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.LocationType
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.data.model.ShoppingPriority
import com.pisheblok.app.data.repo.Repository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LocationUi(
    val id: String,
    val name: String,
    val icon: String,
    val type: LocationType,
    val itemCount: Int,
    val lowCount: Int,
)

data class ItemUi(
    val id: String,
    val locationId: String,
    val locationPath: String,
    val name: String,
    val qty: Double,
    val unit: QuantityUnit,
    val category: ItemCategory,
    val expiry: Long?,
    val minQty: Double,
    val barcode: String? = null,
    val deleted: Boolean = false,
) {
    val isLow: Boolean get() = qty <= minQty && !deleted
}

/** Quick-add hint derived from previously entered items. */
data class ItemSuggestion(
    val name: String,
    val unit: QuantityUnit,
    val category: ItemCategory,
    val minQty: Double,
)

data class BrowserState(
    val pathNames: List<String> = emptyList(),
    val currentId: String? = null,
    val currentIcon: String? = null,
    val currentName: String? = null,
    val depth: Int = 0,
    val children: List<LocationUi> = emptyList(),
    val itemsHere: List<ItemUi> = emptyList(),
    val searching: Boolean = false,
    val query: String = "",
    val results: List<ItemUi> = emptyList(),
)

class LocationsViewModel(
    private val repo: Repository,
    val section: Section,
) : ViewModel() {

    private val pathIds = MutableStateFlow<List<String>>(emptyList())
    private val searching = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    val state: StateFlow<BrowserState> = combine(
        repo.observeLocations(section),
        repo.observeItemsIncludingDeleted(section),
        pathIds,
        combine(searching, query) { s, q -> s to q },
    ) { locs, items, path, (s, q) ->
        buildState(locs, items, path, s, q)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), BrowserState())

    /** Distinct previously used items for quick-add hints. */
    val suggestions: StateFlow<List<ItemSuggestion>> = repo.observeSuggestions(section)
        .map { list ->
            list.map {
                ItemSuggestion(
                    name = it.name,
                    unit = QuantityUnit.fromId(it.unit),
                    category = ItemCategory.fromId(it.category),
                    minQty = it.minQty,
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    // ---------- navigation ----------

    fun open(id: String) {
        pathIds.value = pathIds.value + id
    }

    fun back() {
        if (pathIds.value.isNotEmpty()) {
            pathIds.value = pathIds.value.dropLast(1)
        }
    }

    fun backTo(index: Int) {
        pathIds.value = pathIds.value.take(index)
    }

    fun setSearch(active: Boolean, q: String = "") {
        searching.value = active
        if (!active) query.value = "" else query.value = q
    }

    fun setQuery(q: String) {
        query.value = q
    }

    // ---------- locations ----------

    fun addLocation(name: String, icon: String) {
        val depth = pathIds.value.size
        viewModelScope.launch {
            repo.addLocation(name, icon, addType(section, depth), pathIds.value.lastOrNull(), section)
        }
    }

    fun updateLocation(id: String, name: String, icon: String) {
        viewModelScope.launch {
            val e = repo.getLocation(id) ?: return@launch
            repo.updateLocation(e, name, icon)
        }
    }

    fun deleteLocation(id: String) {
        viewModelScope.launch {
            val e = repo.getLocation(id) ?: return@launch
            repo.deleteLocationCascade(e)
            // trim path so we never stay inside a deleted node
            val idx = pathIds.value.indexOf(id)
            if (idx >= 0) pathIds.value = pathIds.value.take(idx)
        }
    }

    // ---------- items ----------

    fun addItem(
        locationId: String,
        name: String,
        qty: Double,
        unit: QuantityUnit,
        category: ItemCategory,
        expiry: Long?,
        minQty: Double,
        barcode: String?,
    ) {
        viewModelScope.launch {
            repo.addItem(locationId, section, name, qty, unit, category, expiry, minQty, barcode)
        }
    }

    fun updateItem(
        id: String,
        name: String,
        qty: Double,
        unit: QuantityUnit,
        category: ItemCategory,
        expiry: Long?,
        minQty: Double,
        barcode: String?,
    ) {
        viewModelScope.launch {
            val e = repo.getItem(id) ?: return@launch
            repo.updateItem(e, name, qty, unit, category, expiry, minQty, barcode)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            val e = repo.getItem(id) ?: return@launch
            repo.deleteItem(e)
        }
    }

    /** Adds the item to the "Buy" list with the chosen quantity and priority. */
    fun addBuy(item: ItemUi, qty: Double, urgent: Boolean) {
        viewModelScope.launch {
            repo.addShopping(item.name, qty, item.unit, if (urgent) ShoppingPriority.URGENT else ShoppingPriority.LATER, item.id)
        }
    }

    // ---------- building ----------

    private fun buildState(
        locs: List<LocationEntity>,
        items: List<ItemEntity>,
        path: List<String>,
        searchingOn: Boolean,
        q: String,
    ): BrowserState {
        val byId = locs.associateBy { it.id }
        val childrenMap = locs.groupBy { it.parentId }
        val itemsByLoc = items.groupBy { it.locationId }

        val validPath = path.filter { byId.containsKey(it) }
        val pathLocs = validPath.mapNotNull { byId[it] }
        val currentId = validPath.lastOrNull()
        val current = currentId?.let { byId[it] }

        fun subtreeIds(rootId: String): Set<String> {
            val acc = mutableSetOf(rootId)
            val stack = ArrayDeque(listOf(rootId))
            while (stack.isNotEmpty()) {
                val cur = stack.removeFirst()
                for (c in childrenMap[cur].orEmpty()) {
                    if (acc.add(c.id)) stack.addLast(c.id)
                }
            }
            return acc
        }

        val children = childrenMap[currentId].orEmpty()
            .sortedWith(compareBy({ it.sortOrder }, { it.name }))
            .map { c ->
                val ids = subtreeIds(c.id)
                val its = items.filter { !it.isDeleted && it.locationId in ids }
                LocationUi(c.id, c.name, c.icon, LocationType.valueOf(c.type), its.size, its.count { it.qty <= it.minQty })
            }

        val itemsHere = currentId?.let { itemsByLoc[it].orEmpty() } ?: emptyList()
        val itemsHereUi = itemsHere.filter { !it.isDeleted }
            .map { it.toUi(byId) }
            .sortedWith(
                compareByDescending<ItemUi> { it.isLow }
                    .thenBy { it.expiry ?: Long.MAX_VALUE }
                    .thenBy { it.name }
            )

        val results = if (searchingOn && q.isNotBlank()) {
            val needle = q.trim().lowercase()
            items.filter { it.name.lowercase().contains(needle) }
                .map { it.toUi(byId) }
                .sortedWith(compareByDescending<ItemUi> { !it.deleted }.thenBy { it.name })
        } else emptyList()

        return BrowserState(
            pathNames = pathLocs.map { it.name },
            currentId = currentId,
            currentIcon = current?.icon,
            currentName = current?.name,
            depth = validPath.size,
            children = children,
            itemsHere = itemsHereUi,
            searching = searchingOn,
            query = q,
            results = results,
        )
    }

    private fun ItemEntity.toUi(byId: Map<String, LocationEntity>): ItemUi {
        val parts = mutableListOf<String>()
        var cur = byId[locationId]
        while (cur != null) {
            parts.add(cur.name)
            cur = cur.parentId?.let { byId[it] }
        }
        return ItemUi(
            id = id,
            locationId = locationId,
            locationPath = parts.reversed().joinToString(" › "),
            name = name,
            qty = qty,
            unit = QuantityUnit.fromId(unit),
            category = ItemCategory.fromId(category),
            expiry = expiryDate,
            minQty = minQty,
            barcode = barcode,
            deleted = isDeleted,
        )
    }

    companion object {
        fun addLabelRes(section: Section, depth: Int): Int = when (section) {
            Section.FOOD -> when (depth) {
                0 -> R.string.add_room
                1 -> R.string.add_place
                else -> R.string.add_subplace
            }
            Section.CHEM -> when (depth) {
                0 -> R.string.add_floor
                1 -> R.string.add_room
                else -> R.string.add_place
            }
        }

        fun addType(section: Section, depth: Int): LocationType = when (section) {
            Section.FOOD -> if (depth == 0) LocationType.ROOM else LocationType.PLACE
            Section.CHEM -> when (depth) {
                0 -> LocationType.FLOOR
                1 -> LocationType.ROOM
                else -> LocationType.PLACE
            }
        }

        fun iconSetFor(section: Section, depth: Int): List<String> = when {
            section == Section.CHEM && depth == 0 -> com.pisheblok.app.ui.components.ICONS_FLOORS
            (section == Section.FOOD && depth == 0) || (section == Section.CHEM && depth == 1) ->
                com.pisheblok.app.ui.components.ICONS_ROOMS
            else -> com.pisheblok.app.ui.components.ICONS_PLACES
        }
    }
}
