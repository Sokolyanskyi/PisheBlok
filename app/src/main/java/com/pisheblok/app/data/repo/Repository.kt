package com.pisheblok.app.data.repo

import com.pisheblok.app.data.local.ItemDao
import com.pisheblok.app.data.local.ItemEntity
import com.pisheblok.app.data.local.LocationDao
import com.pisheblok.app.data.local.LocationEntity
import com.pisheblok.app.data.local.ShoppingDao
import com.pisheblok.app.data.local.ShoppingEntity
import com.pisheblok.app.data.model.ItemCategory
import com.pisheblok.app.data.model.QuantityUnit
import com.pisheblok.app.data.model.Section
import com.pisheblok.app.data.model.ShoppingPriority
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.first
import java.util.UUID

/**
 * Room-backed facade. Every mutation stamps `updatedAt` + `dirty` and emits an event,
 * which [com.pisheblok.app.sync.SyncManager] debounces into a Firestore push.
 */
class Repository(
    private val locationDao: LocationDao,
    private val itemDao: ItemDao,
    private val shoppingDao: ShoppingDao,
) {
    private val _changes = MutableSharedFlow<Unit>(extraBufferCapacity = 64)
    val changes: SharedFlow<Unit> = _changes

    private fun notifyDataChanged() {
        _changes.tryEmit(Unit)
    }

    // ---------- Observers ----------

    fun observeLocations(section: Section): Flow<List<LocationEntity>> =
        locationDao.observeBySection(section.name)

    fun observeAllLocations(): Flow<List<LocationEntity>> = locationDao.observeAll()

    fun observeItems(section: Section): Flow<List<ItemEntity>> =
        itemDao.observeBySection(section.name)

    /** Includes tombstoned (deleted) items — used by search so past items are findable. */
    fun observeItemsIncludingDeleted(section: Section): Flow<List<ItemEntity>> =
        itemDao.observeBySectionIncludingDeleted(section.name)

    fun observeSuggestions(section: Section): Flow<List<ItemEntity>> =
        itemDao.suggestions(section.name)

    fun observeAllItems(): Flow<List<ItemEntity>> = itemDao.observeAll()

    fun observeShopping(): Flow<List<ShoppingEntity>> = shoppingDao.observeAll()

    // ---------- Locations ----------

    suspend fun addLocation(
        name: String,
        icon: String,
        type: com.pisheblok.app.data.model.LocationType,
        parentId: String?,
        section: Section,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        locationDao.upsert(
            LocationEntity(
                id = id,
                name = name.trim(),
                type = type.name,
                parentId = parentId,
                icon = icon.ifBlank { "📦" },
                section = section.name,
                sortOrder = (locationDao.maxSortOrder(parentId) ?: 0) + 1,
                updatedAt = now,
                dirty = true,
            )
        )
        notifyDataChanged()
        return id
    }

    suspend fun updateLocation(e: LocationEntity, name: String, icon: String) {
        locationDao.upsert(e.copy(name = name.trim(), icon = icon.ifBlank { e.icon }, updatedAt = System.currentTimeMillis(), dirty = true))
        notifyDataChanged()
    }

    /** Tombstones the location, its whole subtree and items inside it. */
    suspend fun deleteLocationCascade(root: LocationEntity) {
        // Simple, robust approach: scan the whole table once (small data).
        val allLocs = allLocationsSnapshot()
        val doomed = mutableSetOf(root.id)
        var changed = true
        while (changed) {
            changed = false
            for (l in allLocs) {
                if (l.parentId != null && l.id !in doomed && l.parentId in doomed) {
                    doomed.add(l.id)
                    changed = true
                }
            }
        }
        val now = System.currentTimeMillis()
        allLocs.filter { it.id in doomed }.forEach {
            locationDao.upsert(it.copy(isDeleted = true, updatedAt = now, dirty = true))
        }
        allItemsSnapshot().filter { it.locationId in doomed }.forEach {
            itemDao.upsert(it.copy(isDeleted = true, updatedAt = now, dirty = true))
        }
        notifyDataChanged()
    }

    private suspend fun allLocationsSnapshot(): List<LocationEntity> =
        observeAllLocations().first()

    private suspend fun allItemsSnapshot(): List<ItemEntity> =
        observeAllItems().first()

    suspend fun getLocation(id: String): LocationEntity? = locationDao.byId(id)

    // ---------- Items ----------

    suspend fun addItem(
        locationId: String,
        section: Section,
        name: String,
        qty: Double,
        unit: QuantityUnit,
        category: ItemCategory,
        expiryDate: Long?,
        minQty: Double,
        barcode: String? = null,
    ): String {
        val id = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()
        itemDao.upsert(
            ItemEntity(
                id = id,
                locationId = locationId,
                name = name.trim(),
                qty = qty,
                unit = unit.id,
                category = category.id,
                expiryDate = expiryDate,
                minQty = minQty,
                barcode = barcode,
                section = section.name,
                createdAt = now,
                updatedAt = now,
                dirty = true,
            )
        )
        notifyDataChanged()
        return id
    }

    suspend fun updateItem(
        e: ItemEntity,
        name: String,
        qty: Double,
        unit: QuantityUnit,
        category: ItemCategory,
        expiryDate: Long?,
        minQty: Double,
        barcode: String? = e.barcode,
    ) {
        itemDao.upsert(
            e.copy(
                name = name.trim(),
                qty = qty,
                unit = unit.id,
                category = category.id,
                expiryDate = expiryDate,
                minQty = minQty,
                barcode = barcode,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            )
        )
        notifyDataChanged()
    }

    suspend fun deleteItem(e: ItemEntity) {
        itemDao.upsert(e.copy(isDeleted = true, updatedAt = System.currentTimeMillis(), dirty = true))
        notifyDataChanged()
    }

    suspend fun changeItemQty(id: String, delta: Double) {
        val e = itemDao.byId(id) ?: return
        val newQty = (e.qty + delta).coerceAtLeast(0.0)
        if (newQty == e.qty) return
        itemDao.upsert(e.copy(qty = newQty, updatedAt = System.currentTimeMillis(), dirty = true))
        notifyDataChanged()
    }

    suspend fun getItem(id: String): ItemEntity? = itemDao.byId(id)

    // ---------- Shopping ----------

    suspend fun addShopping(name: String, qty: Double, unit: QuantityUnit, priority: ShoppingPriority, sourceItemId: String?) {
        val now = System.currentTimeMillis()
        shoppingDao.upsert(
            ShoppingEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                qty = qty,
                unit = unit.id,
                done = false,
                priority = priority.name,
                sourceItemId = sourceItemId,
                createdAt = now,
                updatedAt = now,
                dirty = true,
            )
        )
        notifyDataChanged()
    }

    suspend fun updateShopping(e: ShoppingEntity, name: String, qty: Double, unit: QuantityUnit, priority: ShoppingPriority) {
        shoppingDao.upsert(
            e.copy(
                name = name.trim(),
                qty = qty,
                unit = unit.id,
                priority = priority.name,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            )
        )
        notifyDataChanged()
    }

    suspend fun setShoppingDone(e: ShoppingEntity, done: Boolean) {
        shoppingDao.upsert(e.copy(done = done, updatedAt = System.currentTimeMillis(), dirty = true))
        notifyDataChanged()
    }

    suspend fun deleteShopping(e: ShoppingEntity) {
        shoppingDao.upsert(e.copy(isDeleted = true, updatedAt = System.currentTimeMillis(), dirty = true))
        notifyDataChanged()
    }

    suspend fun clearDoneShopping() {
        shoppingDao.clearDone(System.currentTimeMillis())
        notifyDataChanged()
    }

    // ---------- Sync support (used by SyncManager) ----------

    suspend fun dirtyLocations(): List<LocationEntity> = locationDao.dirty()
    suspend fun dirtyItems(): List<ItemEntity> = itemDao.dirty()
    suspend fun dirtyShopping(): List<ShoppingEntity> = shoppingDao.dirty()

    suspend fun markLocationSynced(e: LocationEntity) = locationDao.markSyncedIfUnchanged(e.id, e.updatedAt)
    suspend fun markItemSynced(e: ItemEntity) = itemDao.markSyncedIfUnchanged(e.id, e.updatedAt)
    suspend fun markShoppingSynced(e: ShoppingEntity) = shoppingDao.markSyncedIfUnchanged(e.id, e.updatedAt)

    suspend fun upsertRemoteLocations(list: List<LocationEntity>) = locationDao.upsertAll(list)
    suspend fun upsertRemoteItems(list: List<ItemEntity>) = itemDao.upsertAll(list)
    suspend fun upsertRemoteShopping(list: List<ShoppingEntity>) = shoppingDao.upsertAll(list)

    suspend fun localLocationById(id: String): LocationEntity? = locationDao.byId(id)
    suspend fun localItemById(id: String): ItemEntity? = itemDao.byId(id)
    suspend fun localShoppingById(id: String): ShoppingEntity? = shoppingDao.byId(id)
}
