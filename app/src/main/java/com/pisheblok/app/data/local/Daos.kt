package com.pisheblok.app.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface LocationDao {

    @Query("SELECT * FROM locations WHERE isDeleted = 0 AND section = :section ORDER BY sortOrder, name")
    fun observeBySection(section: String): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE isDeleted = 0")
    fun observeAll(): Flow<List<LocationEntity>>

    @Query("SELECT * FROM locations WHERE id = :id")
    suspend fun byId(id: String): LocationEntity?

    @Query("SELECT * FROM locations WHERE dirty = 1")
    suspend fun dirty(): List<LocationEntity>

    @Upsert
    suspend fun upsert(e: LocationEntity)

    @Upsert
    suspend fun upsertAll(list: List<LocationEntity>)

    @Query("UPDATE locations SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: Long)

    @Query("SELECT MAX(sortOrder) FROM locations WHERE parentId IS :parentId AND isDeleted = 0")
    suspend fun maxSortOrder(parentId: String?): Int?
}

@Dao
interface ItemDao {

    @Query("SELECT * FROM items WHERE isDeleted = 0 AND section = :section")
    fun observeBySection(section: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE section = :section")
    fun observeBySectionIncludingDeleted(section: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE isDeleted = 0")
    fun observeAll(): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE id = :id")
    suspend fun byId(id: String): ItemEntity?

    /** One row per distinct name — used for quick-add suggestions. */
    @Query("SELECT * FROM items WHERE section = :section GROUP BY name ORDER BY MAX(updatedAt) DESC LIMIT 50")
    fun suggestions(section: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE dirty = 1")
    suspend fun dirty(): List<ItemEntity>

    @Upsert
    suspend fun upsert(e: ItemEntity)

    @Upsert
    suspend fun upsertAll(list: List<ItemEntity>)

    @Query("UPDATE items SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: Long)
}

@Dao
interface ShoppingDao {

    @Query("SELECT * FROM shopping WHERE isDeleted = 0 ORDER BY done, createdAt DESC")
    fun observeAll(): Flow<List<ShoppingEntity>>

    @Query("SELECT * FROM shopping WHERE id = :id")
    suspend fun byId(id: String): ShoppingEntity?

    @Query("SELECT * FROM shopping WHERE dirty = 1")
    suspend fun dirty(): List<ShoppingEntity>

    @Upsert
    suspend fun upsert(e: ShoppingEntity)

    @Upsert
    suspend fun upsertAll(list: List<ShoppingEntity>)

    @Query("UPDATE shopping SET dirty = 0 WHERE id = :id AND updatedAt = :updatedAt")
    suspend fun markSyncedIfUnchanged(id: String, updatedAt: Long)

    @Query("UPDATE shopping SET isDeleted = 1, dirty = 1, updatedAt = :now WHERE done = 1 AND isDeleted = 0")
    suspend fun clearDone(now: Long)
}
