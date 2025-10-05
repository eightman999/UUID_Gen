package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BonusSlotDao {
    @Query("SELECT * FROM bonus_slots")
    fun observeAll(): Flow<List<BonusSlotEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: BonusSlotEntity)

    @Query("DELETE FROM bonus_slots WHERE expires_at <= :threshold")
    suspend fun deleteExpired(threshold: Long)

    @Query("DELETE FROM bonus_slots WHERE id = :id")
    suspend fun deleteById(id: String)
}
