package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UuidItemDao {
    @Query("SELECT * FROM uuid_items ORDER BY created_at DESC")
    fun observeItems(): Flow<List<UuidItemEntity>>

    @Query("SELECT COUNT(*) FROM uuid_items")
    fun observeCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: UuidItemEntity)

    @Query("DELETE FROM uuid_items WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM uuid_items")
    suspend fun clear()
}
