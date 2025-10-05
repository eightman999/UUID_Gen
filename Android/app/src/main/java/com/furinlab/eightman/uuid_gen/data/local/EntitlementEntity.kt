package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entitlement")
data class EntitlementEntity(
    @PrimaryKey val id: Int = 1,
    @ColumnInfo(name = "is_pro") val isPro: Boolean,
    @ColumnInfo(name = "pro_since") val proSince: Long?,
    @ColumnInfo(name = "last_sync") val lastSyncAt: Long?,
)
