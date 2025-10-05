package com.furinlab.eightman.uuid_gen.data.repository

import com.furinlab.eightman.uuid_gen.data.local.BonusSlotDao
import com.furinlab.eightman.uuid_gen.data.local.BonusSlotEntity
import com.furinlab.eightman.uuid_gen.data.local.EntitlementDao
import com.furinlab.eightman.uuid_gen.data.local.EntitlementEntity
import com.furinlab.eightman.uuid_gen.data.local.UuidItemDao
import com.furinlab.eightman.uuid_gen.data.local.UuidItemEntity
import com.furinlab.eightman.uuid_gen.domain.model.BonusSlot
import com.furinlab.eightman.uuid_gen.domain.model.Entitlement
import com.furinlab.eightman.uuid_gen.domain.model.UuidFormatOptions
import com.furinlab.eightman.uuid_gen.domain.model.UuidItem
import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class DefaultUuidRepository @Inject constructor(
    private val uuidItemDao: UuidItemDao,
    private val bonusSlotDao: BonusSlotDao,
    private val entitlementDao: EntitlementDao,
) : UuidRepository {

    override val items: Flow<List<UuidItem>> = uuidItemDao.observeItems().map { entities ->
        entities.map { it.toDomain() }
    }

    override val itemCount: Flow<Int> = uuidItemDao.observeCount()

    override val bonusSlots: Flow<List<BonusSlot>> = bonusSlotDao.observeAll().map { entities ->
        entities.map { BonusSlot(id = it.id, expiresAt = it.expiresAt) }
    }

    override val entitlement: Flow<Entitlement> = entitlementDao.observe().map { entity ->
        entity?.toDomain() ?: Entitlement.Default
    }

    override suspend fun save(item: UuidItem) {
        uuidItemDao.insert(item.toEntity())
    }

    override suspend fun delete(id: String) {
        uuidItemDao.deleteById(id)
    }

    override suspend fun addBonusSlot(slot: BonusSlot) {
        bonusSlotDao.insert(BonusSlotEntity(id = slot.id, expiresAt = slot.expiresAt))
    }

    override suspend fun removeExpiredBonus(now: Long) {
        bonusSlotDao.deleteExpired(now)
    }

    override suspend fun setEntitlement(entitlement: Entitlement) {
        val entity = EntitlementEntity(
            id = 1,
            isPro = entitlement.isPro,
            proSince = entitlement.proSince,
            lastSyncAt = entitlement.lastSyncAt,
        )
        entitlementDao.upsert(entity)
    }

    private fun UuidItemEntity.toDomain(): UuidItem = UuidItem(
        id = id,
        value = value,
        version = UuidVersion.fromString(version),
        createdAt = createdAt,
        label = label,
        namespace = namespace,
        name = name,
        format = UuidFormatOptions.decode(styleFlags),
    )

    private fun EntitlementEntity.toDomain(): Entitlement = Entitlement(
        isPro = isPro,
        proSince = proSince,
        lastSyncAt = lastSyncAt,
    )

    private fun UuidItem.toEntity(): UuidItemEntity = UuidItemEntity(
        id = id,
        value = value,
        version = version.displayName,
        createdAt = createdAt,
        label = label,
        namespace = namespace,
        name = name,
        styleFlags = format.encode(),
    )
}
