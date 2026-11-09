package com.pisheblok.app.sync

import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.pisheblok.app.auth.AuthManager
import com.pisheblok.app.data.local.ItemEntity
import com.pisheblok.app.data.local.LocationEntity
import com.pisheblok.app.data.local.ShoppingEntity
import com.pisheblok.app.data.repo.Repository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.tasks.await

sealed interface SyncStatus {
    /** Local mode or signed out — no sync happening. */
    data object Idle : SyncStatus
    data object Syncing : SyncStatus
    data object Ok : SyncStatus
    data object Offline : SyncStatus
    data object Error : SyncStatus
}

private const val BATCH = 400

/**
 * Replicates Room data to Firestore (users/{uid}/locations|items|shopping).
 *
 * Strategy: Room is the source of truth. Full sync = pull remote docs and merge them
 * into Room with last-write-wins (by `updatedAt`), then push all locally `dirty` docs.
 * A realtime snapshot listener applies remote changes immediately; local changes are
 * debounced and pushed. Tombstones (`isDeleted`) propagate deletions between devices.
 */
@OptIn(FlowPreview::class)
class SyncManager(
    private val scope: CoroutineScope,
    private val repo: Repository,
    private val auth: AuthManager,
    private val network: NetworkObserver,
) {
    private val _status = MutableStateFlow<SyncStatus>(SyncStatus.Idle)
    val status: StateFlow<SyncStatus> = _status

    @Volatile
    private var uid: String? = null

    private val listeners = mutableListOf<ListenerRegistration>()
    private val mutex = Mutex()
    private var authJob: Job? = null
    private var eventJob: Job? = null
    private var networkJob: Job? = null

    fun start() {
        authJob = scope.launch {
            auth.state.collect { s ->
                val user = s.user
                if (s.cloudReady && user != null) {
                    if (user.uid != uid) attach(user.uid)
                } else {
                    detach()
                }
            }
        }
        eventJob = scope.launch {
            repo.changes.debounce(1500).collect {
                if (uid != null) pushDirty()
            }
        }
        networkJob = scope.launch {
            network.online.debounce(500).collect { up ->
                if (up && uid != null) fullSync()
            }
        }
    }

    private fun attach(newUid: String) {
        detach()
        uid = newUid
        val fs = FirebaseFirestore.getInstance()
        val base = "users/$newUid"

        listeners += fs.collection("$base/locations").addSnapshotListener { snap, _ ->
            val docs = snap?.documents ?: return@addSnapshotListener
            scope.launch { applyMergedLocations(docs.mapNotNull { it.toLocation() }) }
        }
        listeners += fs.collection("$base/items").addSnapshotListener { snap, _ ->
            val docs = snap?.documents ?: return@addSnapshotListener
            scope.launch { applyMergedItems(docs.mapNotNull { it.toItem() }) }
        }
        listeners += fs.collection("$base/shopping").addSnapshotListener { snap, _ ->
            val docs = snap?.documents ?: return@addSnapshotListener
            scope.launch { applyMergedShopping(docs.mapNotNull { it.toShopping() }) }
        }

        scope.launch { fullSync() }
    }

    private fun detach() {
        listeners.forEach { it.remove() }
        listeners.clear()
        uid = null
        _status.value = SyncStatus.Idle
    }

    // ---------- Full sync ----------

    suspend fun fullSync() {
        val u = uid ?: return
        if (!network.online.value) {
            _status.value = SyncStatus.Offline
            return
        }
        mutex.withLock {
            _status.value = SyncStatus.Syncing
            try {
                val fs = FirebaseFirestore.getInstance()
                val base = "users/$u"

                val locs = fs.collection("$base/locations").get().await()
                    .documents.mapNotNull { it.toLocation() }
                applyMergedLocations(locs)

                val items = fs.collection("$base/items").get().await()
                    .documents.mapNotNull { it.toItem() }
                applyMergedItems(items)

                val shopping = fs.collection("$base/shopping").get().await()
                    .documents.mapNotNull { it.toShopping() }
                applyMergedShopping(shopping)

                pushDirty()
                _status.value = SyncStatus.Ok
            } catch (e: Exception) {
                _status.value =
                    if (network.online.value) SyncStatus.Error else SyncStatus.Offline
            }
        }
    }

    private suspend fun pushDirty() {
        val u = uid ?: return
        val fs = FirebaseFirestore.getInstance()
        val base = "users/$u"
        try {
            while (true) {
                val chunk = repo.dirtyLocations().take(BATCH)
                if (chunk.isEmpty()) break
                fs.batch().apply {
                    chunk.forEach { set(fs.document("$base/locations/${it.id}"), it.toFsMap()) }
                }.commit().await()
                chunk.forEach { repo.markLocationSynced(it) }
            }
            while (true) {
                val chunk = repo.dirtyItems().take(BATCH)
                if (chunk.isEmpty()) break
                fs.batch().apply {
                    chunk.forEach { set(fs.document("$base/items/${it.id}"), it.toFsMap()) }
                }.commit().await()
                chunk.forEach { repo.markItemSynced(it) }
            }
            while (true) {
                val chunk = repo.dirtyShopping().take(BATCH)
                if (chunk.isEmpty()) break
                fs.batch().apply {
                    chunk.forEach { set(fs.document("$base/shopping/${it.id}"), it.toFsMap()) }
                }.commit().await()
                chunk.forEach { repo.markShoppingSynced(it) }
            }
            if (_status.value is SyncStatus.Error || _status.value is SyncStatus.Offline) {
                _status.value = SyncStatus.Ok
            }
        } catch (e: Exception) {
            _status.value =
                if (network.online.value) SyncStatus.Error else SyncStatus.Offline
        }
    }

    // ---------- LWW merge ----------

    private suspend fun applyMergedLocations(remote: List<LocationEntity>) {
        for (r in remote) {
            val local = repo.localLocationById(r.id)
            if (local == null || r.updatedAt > local.updatedAt) {
                repo.upsertRemoteLocations(listOf(r.copy(dirty = false)))
            }
        }
    }

    private suspend fun applyMergedItems(remote: List<ItemEntity>) {
        for (r in remote) {
            val local = repo.localItemById(r.id)
            if (local == null || r.updatedAt > local.updatedAt) {
                repo.upsertRemoteItems(listOf(r.copy(dirty = false)))
            }
        }
    }

    private suspend fun applyMergedShopping(remote: List<ShoppingEntity>) {
        for (r in remote) {
            val local = repo.localShoppingById(r.id)
            if (local == null || r.updatedAt > local.updatedAt) {
                repo.upsertRemoteShopping(listOf(r.copy(dirty = false)))
            }
        }
    }

    // ---------- Firestore mapping ----------

    private fun LocationEntity.toFsMap() = mapOf(
        "name" to name,
        "type" to type,
        "parentId" to parentId,
        "icon" to icon,
        "section" to section,
        "sortOrder" to sortOrder,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
    )

    private fun ItemEntity.toFsMap() = mapOf(
        "locationId" to locationId,
        "name" to name,
        "qty" to qty,
        "unit" to unit,
        "category" to category,
        "expiryDate" to expiryDate,
        "minQty" to minQty,
        "barcode" to barcode,
        "section" to section,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
    )

    private fun ShoppingEntity.toFsMap() = mapOf(
        "name" to name,
        "qty" to qty,
        "unit" to unit,
        "done" to done,
        "priority" to priority,
        "sourceItemId" to sourceItemId,
        "createdAt" to createdAt,
        "updatedAt" to updatedAt,
        "isDeleted" to isDeleted,
    )

    private fun DocumentSnapshot.toLocation(): LocationEntity? = LocationEntity(
        id = id,
        name = getString("name") ?: "",
        type = getString("type") ?: "PLACE",
        parentId = getString("parentId"),
        icon = getString("icon") ?: "📦",
        section = getString("section") ?: "FOOD",
        sortOrder = (getLong("sortOrder") ?: 0L).toInt(),
        updatedAt = getLong("updatedAt") ?: 0L,
        isDeleted = getBoolean("isDeleted") ?: false,
        dirty = false,
    )

    private fun DocumentSnapshot.toItem(): ItemEntity? {
        val locationId = getString("locationId") ?: return null
        return ItemEntity(
            id = id,
            locationId = locationId,
            name = getString("name") ?: "",
            qty = getDouble("qty") ?: 0.0,
            unit = getString("unit") ?: "pcs",
            category = getString("category") ?: "other",
            expiryDate = getLong("expiryDate"),
            minQty = getDouble("minQty") ?: 0.0,
            barcode = getString("barcode"),
            section = getString("section") ?: "FOOD",
            createdAt = getLong("createdAt") ?: 0L,
            updatedAt = getLong("updatedAt") ?: 0L,
            isDeleted = getBoolean("isDeleted") ?: false,
            dirty = false,
        )
    }

    private fun DocumentSnapshot.toShopping(): ShoppingEntity = ShoppingEntity(
        id = id,
        name = getString("name") ?: "",
        qty = getDouble("qty") ?: 0.0,
        unit = getString("unit") ?: "pcs",
        done = getBoolean("done") ?: false,
        priority = getString("priority") ?: "URGENT",
        sourceItemId = getString("sourceItemId"),
        createdAt = getLong("createdAt") ?: 0L,
        updatedAt = getLong("updatedAt") ?: 0L,
        isDeleted = getBoolean("isDeleted") ?: false,
        dirty = false,
    )
}
