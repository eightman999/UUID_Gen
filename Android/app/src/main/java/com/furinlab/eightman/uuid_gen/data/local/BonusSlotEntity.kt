package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bonus_slots")
data class BonusSlotEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "expires_at") val expiresAt: Long,
)
