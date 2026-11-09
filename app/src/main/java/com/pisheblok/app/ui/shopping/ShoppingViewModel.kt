package com.pisheblok.app.ui.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pisheblok.app.data.local.ItemEntity
import com.pisheblok.app.data.local.LocationEntity
import com.pisheblok.app.data.local.ShoppingEntity
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.ShoppingPriority
import com.pisheblok.app.data.repo.Repository
import com.pisheblok.app.ui.locations.ItemUi
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class BuyUi(
    val id: String,
    val name: String,
    val qty: Double,
    val unit: QuantityUnit,
    val done: Boolean,
    val urgent: Boolean,
    val sourceItemId: String?,
    val linkedLocationPath: String?,
)

data class ShoppingState(
    val low: List<ItemUi> = emptyList(),
    val urgent: List<BuyUi> = emptyList(),
    val later: List<BuyUi> = emptyList(),
    val hasDone: Boolean = false,
)

class ShoppingViewModel(
    private val repo: Repository,
) : ViewModel() {

    val state: StateFlow<ShoppingState> = combine(
        repo.observeAllItems(),
        repo.observeAllLocations(),
        repo.observeShopping(),
    ) { items, locs, shopping ->
        buildState(items, locs, shopping)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ShoppingState())

    fun addManual(name: String, qty: Double, unit: QuantityUnit, urgent: Boolean) {
        viewModelScope.launch {
            repo.addShopping(name, qty, unit, if (urgent) ShoppingPriority.URGENT else ShoppingPriority.LATER, null)
        }
    }

    /** Adds an inventory item to the buy list with the chosen quantity. */
    fun addFromItem(item: ItemUi, qty: Double, urgent: Boolean) {
        viewModelScope.launch {
            repo.addShopping(item.name, qty, item.unit, if (urgent) ShoppingPriority.URGENT else ShoppingPriority.LATER, item.id)
        }
    }

    fun setDone(entryId: String, done: Boolean) {
        viewModelScope.launch {
            val e = repo.localShoppingById(entryId) ?: return@launch
            repo.setShoppingDone(e, done)
        }
    }

    /** Restocks the linked inventory item and removes the entry from the list. */
    fun restock(entryId: String) {
        viewModelScope.launch {
            val e = repo.localShoppingById(entryId) ?: return@launch
            val sourceId = e.sourceItemId ?: return@launch
            repo.changeItemQty(sourceId, e.qty)
            repo.deleteShopping(e)
        }
    }

    fun deleteEntry(entryId: String) {
        viewModelScope.launch {
            val e = repo.localShoppingById(entryId) ?: return@launch
            repo.deleteShopping(e)
        }
    }

    fun updateEntry(entryId: String, name: String, qty: Double, unit: QuantityUnit, urgent: Boolean) {
        viewModelScope.launch {
            val e = repo.localShoppingById(entryId) ?: return@launch
            repo.updateShopping(e, name, qty, unit, if (urgent) ShoppingPriority.URGENT else ShoppingPriority.LATER)
        }
    }

    fun clearDone() {
        viewModelScope.launch { repo.clearDoneShopping() }
    }

    private fun buildState(
        items: List<ItemEntity>,
        locs: List<LocationEntity>,
        shopping: List<ShoppingEntity>,
    ): ShoppingState {
        val byId = locs.associateBy { it.id }

        fun pathOf(locationId: String): String {
            val parts = mutableListOf<String>()
            var cur = byId[locationId]
            while (cur != null) {
                parts.add(cur.name)
                cur = cur.parentId?.let { byId[it] }
            }
            return parts.reversed().joinToString(" › ")
        }

        val low = items.filter { it.qty <= it.minQty }
            .map {
                ItemUi(
                    id = it.id,
                    locationId = it.locationId,
                    locationPath = pathOf(it.locationId),
                    name = it.name,
                    qty = it.qty,
                    unit = QuantityUnit.fromId(it.unit),
                    category = ItemCategory.fromId(it.category),
                    expiry = it.expiryDate,
                    minQty = it.minQty,
                )
            }
            .sortedWith(
                compareByDescending<ItemUi> { it.expiry != null && it.expiry < System.currentTimeMillis() }
                    .thenBy { it.expiry ?: Long.MAX_VALUE }
                    .thenBy { it.name }
            )

        val all = shopping
            .sortedWith(compareByDescending<ShoppingEntity> { it.done }.thenByDescending { it.createdAt })
            .map {
                BuyUi(
                    id = it.id,
                    name = it.name,
                    qty = it.qty,
                    unit = QuantityUnit.fromId(it.unit),
                    done = it.done,
                    urgent = it.priority != "LATER",
                    sourceItemId = it.sourceItemId,
                    linkedLocationPath = it.sourceItemId?.let { sid ->
                        items.firstOrNull { item -> item.id == sid }?.let { item -> pathOf(item.locationId) }
                    },
                )
            }

        return ShoppingState(
            low = low,
            urgent = all.filter { it.urgent },
            later = all.filter { !it.urgent },
            hasDone = shopping.any { it.done },
        )
    }
}
