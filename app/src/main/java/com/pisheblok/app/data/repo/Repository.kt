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
import com.pisheblok.app.ui.components.FRIDGE_ICON
import com.pisheblok.app.ui.components.OLD_ICON_MAP
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

    /**
     * Перенос старых статичных эмодзи на библиотеку и назначение
     * холодильной иконки локациям с «холодильником» в имени.
     * Идемпотентно: уже перенесённые не трогаются.
     */
    suspend fun migrateOldEmojiIcons() {
        val all = allLocationsSnapshot()
        for (loc in all) {
            var newIcon = OLD_ICON_MAP[loc.icon] ?: loc.icon
            val name = loc.name.lowercase()
            val fridgeLike = listOf("холодильник", "холодильная", "холодильн", "холодил", "холод", "refrig", "fridge")
                .any { name.contains(it) }
            if (fridgeLike) newIcon = FRIDGE_ICON
            if (newIcon == loc.icon) continue
            updateLocation(loc, loc.name, newIcon, loc.iconStyle)
        }
    }

    suspend fun addLocation(
        name: String,
        icon: String,
        iconStyle: Int = 0,
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
                iconStyle = iconStyle.coerceIn(0, 3),
                section = section.name,
                sortOrder = (locationDao.maxSortOrder(parentId) ?: 0) + 1,
                updatedAt = now,
                dirty = true,
            )
        )
        notifyDataChanged()
        return id
    }

    suspend fun updateLocation(e: LocationEntity, name: String, icon: String, iconStyle: Int = e.iconStyle) {
        locationDao.upsert(
            e.copy(
                name = name.trim(),
                icon = icon.ifBlank { e.icon },
                iconStyle = iconStyle.coerceIn(0, 3),
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            )
        )
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

    suspend fun addShopping(
        name: String,
        qty: Double,
        qtyNote: String?,
        unit: QuantityUnit,
        priority: ShoppingPriority,
        sourceItemId: String?,
    ) {
        val now = System.currentTimeMillis()
        shoppingDao.upsert(
            ShoppingEntity(
                id = UUID.randomUUID().toString(),
                name = name.trim(),
                qty = qty,
                qtyNote = qtyNote?.trim()?.ifBlank { null },
                unit = unit.id,
                done = false,
                priority = priority.name,
                // Новые записи — в верх списка (меньше = выше).
                sortOrder = (shoppingDao.minSortOrder() ?: 0L) - 1L,
                sourceItemId = sourceItemId,
                createdAt = now,
                updatedAt = now,
                dirty = true,
            )
        )
        notifyDataChanged()
    }

    suspend fun updateShopping(
        e: ShoppingEntity,
        name: String,
        qty: Double,
        qtyNote: String?,
        unit: QuantityUnit,
        priority: ShoppingPriority,
    ) {
        shoppingDao.upsert(
            e.copy(
                name = name.trim(),
                qty = qty,
                qtyNote = qtyNote?.trim()?.ifBlank { null },
                unit = unit.id,
                priority = priority.name,
                updatedAt = System.currentTimeMillis(),
                dirty = true,
            )
        )
        notifyDataChanged()
    }

    /**
     * Перенос записи drag&drop: в другую секцию и/или на позицию
     * перед [beforeId] (null — в конец секции). Секция-приёмник
     * перенумеровывается сплошными sortOrder в порядке отображения.
     */
    suspend fun moveShopping(id: String, priority: ShoppingPriority, beforeId: String?) {
        val e = shoppingDao.byId(id) ?: return
        val all = shoppingDao.observeAll().first()
        val now = System.currentTimeMillis()

        // Порядок отображения секции-приёмника: купленное тонет в конец,
        // внутри группы — по ручному sortOrder, новые вперёд.
        val section = all
            .filter { it.priority == priority.name }
            .sortedWith(compareBy<ShoppingEntity> { it.done }.thenBy { it.sortOrder }.thenByDescending { it.createdAt })
            .toMutableList()
        section.removeAll { it.id == id }

        val insertAt = beforeId?.let { b -> section.indexOfFirst { it.id == b } }?.takeIf { it >= 0 }
            ?: section.size
        section.add(insertAt, e.copy(priority = priority.name))

        // База ниже всех существующих значений: новые записи (min - 1)
        // остаются сверху после перенумерации.
        val base = (all.minOfOrNull { it.sortOrder } ?: 0L) - section.size - 1L
        section.forEachIndexed { index, row ->
            val moved = row.id == id
            shoppingDao.upsert(
                row.copy(
                    priority = if (moved) priority.name else row.priority,
                    sortOrder = base + index,
                    updatedAt = now,
                    dirty = true,
                )
            )
        }
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
