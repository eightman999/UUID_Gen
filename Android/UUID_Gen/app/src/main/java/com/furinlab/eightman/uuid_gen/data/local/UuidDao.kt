package com.furinlab.eightman.uuid_gen.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface UuidDao {
    @Query("SELECT * FROM uuid_items ORDER BY created_at DESC")
    fun observeItems(): Flow<List<UuidItemEntity>>

    @Query("SELECT * FROM bonus_slots ORDER BY expires_at ASC")
    fun observeBonusSlots(): Flow<List<BonusSlotEntity>>

    @Query("SELECT * FROM entitlement WHERE id = 0")
    fun observeEntitlement(): Flow<EntitlementEntity?>

    @Query("SELECT * FROM entitlement WHERE id = 0")
    suspend fun getEntitlement(): EntitlementEntity?

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertItem(entity: UuidItemEntity)

    @Query("SELECT COUNT(*) FROM uuid_items WHERE value = :value")
    suspend fun countByValue(value: String): Int

    @Query("SELECT COUNT(*) FROM uuid_items")
    suspend fun countItems(): Int

    @Query("DELETE FROM uuid_items WHERE id = :id")
    suspend fun deleteItem(id: String)

    @Query("DELETE FROM uuid_items")
    suspend fun clearItems()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertEntitlement(entity: EntitlementEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBonusSlot(entity: BonusSlotEntity)

    @Query("DELETE FROM bonus_slots WHERE expires_at < :now")
    suspend fun deleteExpiredBonusSlots(now: Long)

    @Query("SELECT COUNT(*) FROM bonus_slots")
    suspend fun countBonusSlots(): Int
}
