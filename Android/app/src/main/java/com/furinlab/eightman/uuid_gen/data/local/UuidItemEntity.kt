package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "uuid_items")
data class UuidItemEntity(
    @PrimaryKey val id: String,
    val value: String,
    val version: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    val label: String?,
    val namespace: String?,
    val name: String?,
    @ColumnInfo(name = "style_flags") val styleFlags: Int,
)
