package com.pisheblok.app.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "locations",
    indices = [Index("parentId"), Index("section")]
)
data class LocationEntity(
    @PrimaryKey val id: String,
    val name: String,
    val type: String,          // LocationType.name
    val parentId: String?,     // null = root
    val icon: String,          // emoji
    val section: String,       // Section.name
    val sortOrder: Int,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val dirty: Boolean = false,
)

@Entity(
    tableName = "items",
    indices = [Index("locationId"), Index("section")]
)
data class ItemEntity(
    @PrimaryKey val id: String,
    val locationId: String,
    val name: String,
    val qty: Double,
    val unit: String,          // Unit.id
    val category: String,      // ItemCategory.id
    val expiryDate: Long?,     // epoch millis at local midnight, null = none
    val minQty: Double,        // low-stock threshold
    val barcode: String? = null,
    val section: String,       // Section.name
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val dirty: Boolean = false,
)

@Entity(tableName = "shopping")
data class ShoppingEntity(
    @PrimaryKey val id: String,
    val name: String,
    val qty: Double,
    val unit: String,          // Unit.id
    val done: Boolean = false,
    val priority: String = "URGENT", // ShoppingPriority.name
    val sourceItemId: String?, // linked inventory item (auto-added from "running low")
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean = false,
    val dirty: Boolean = false,
)
