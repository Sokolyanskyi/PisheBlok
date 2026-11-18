package com.pisheblok.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(
    entities = [LocationEntity::class, ItemEntity::class, ShoppingEntity::class],
    version = 5,
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

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE locations ADD COLUMN iconStyle INTEGER NOT NULL DEFAULT 0")
            }
        }

        /**
         * Списку покупок — текстовое количество и ручной порядок.
         * Порядок засеян от createdAt (-createdAt = «новые вперёд»), как было
         * до сортировки, чтобы перенос не перемешал существующие списки.
         */
        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE shopping ADD COLUMN qtyNote TEXT")
                db.execSQL("ALTER TABLE shopping ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
                db.execSQL("UPDATE shopping SET sortOrder = -createdAt")
            }
        }
    }
}
