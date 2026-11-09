package com.pisheblok.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocationEntity::class, ItemEntity::class, ShoppingEntity::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun locationDao(): LocationDao
    abstract fun itemDao(): ItemDao
    abstract fun shoppingDao(): ShoppingDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping ADD COLUMN priority TEXT NOT NULL DEFAULT 'URGENT'")
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE items ADD COLUMN barcode TEXT")
            }
        }
    }
}
