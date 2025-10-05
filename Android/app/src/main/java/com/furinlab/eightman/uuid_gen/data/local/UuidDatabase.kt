package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        UuidItemEntity::class,
        BonusSlotEntity::class,
        EntitlementEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class UuidDatabase : RoomDatabase() {
    abstract fun uuidItemDao(): UuidItemDao
    abstract fun bonusSlotDao(): BonusSlotDao
    abstract fun entitlementDao(): EntitlementDao
}
