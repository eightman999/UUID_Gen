package com.furinlab.eightman.uuid_gen.data.repository

import com.furinlab.eightman.uuid_gen.domain.model.BonusSlot
import com.furinlab.eightman.uuid_gen.domain.model.Entitlement
import com.furinlab.eightman.uuid_gen.domain.model.UuidItem
import kotlinx.coroutines.flow.Flow

interface UuidRepository {
    val items: Flow<List<UuidItem>>
    val itemCount: Flow<Int>
    val bonusSlots: Flow<List<BonusSlot>>
    val entitlement: Flow<Entitlement>

    suspend fun save(item: UuidItem)
    suspend fun existsByValue(value: String): Boolean
    suspend fun delete(id: String)
    suspend fun addBonusSlot(slot: BonusSlot)
    suspend fun removeExpiredBonus(now: Long)
    suspend fun setEntitlement(entitlement: Entitlement)
}
