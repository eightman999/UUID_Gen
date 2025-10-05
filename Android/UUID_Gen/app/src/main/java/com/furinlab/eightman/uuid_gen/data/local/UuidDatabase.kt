package com.furinlab.eightman.uuid_gen.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UuidItemEntity::class, BonusSlotEntity::class, EntitlementEntity::class],
    version = 1,
    exportSchema = false
)
abstract class UuidDatabase : RoomDatabase() {
    abstract fun uuidDao(): UuidDao

    companion object {
        fun build(context: Context): UuidDatabase = Room.databaseBuilder(
            context.applicationContext,
            UuidDatabase::class.java,
            "uuid_gen.db"
        ).fallbackToDestructiveMigration().build()
    }
}
