package com.furinlab.eightman.uuid_gen.data

import com.furinlab.eightman.uuid_gen.data.local.BonusSlotEntity
import com.furinlab.eightman.uuid_gen.data.local.EntitlementEntity
import com.furinlab.eightman.uuid_gen.data.local.UuidDao
import com.furinlab.eightman.uuid_gen.data.local.UuidItemEntity
import com.furinlab.eightman.uuid_gen.domain.BonusSlot
import com.furinlab.eightman.uuid_gen.domain.EntitlementState
import com.furinlab.eightman.uuid_gen.domain.FormatOptions
import com.furinlab.eightman.uuid_gen.domain.GeneratedUuid
import com.furinlab.eightman.uuid_gen.domain.LimitConstants
import com.furinlab.eightman.uuid_gen.domain.LimitState
import com.furinlab.eightman.uuid_gen.domain.UuidRecord
import com.furinlab.eightman.uuid_gen.domain.UuidVersion
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

class UuidRepository(
    private val dao: UuidDao,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    data class StoreState(
        val items: List<UuidRecord>,
        val bonusSlots: List<BonusSlot>,
        val entitlement: EntitlementState,
        val limitState: LimitState
    ) {
        val canAddMore: Boolean
            get() = limitState.canAdd(items.size)
    }

    val storeState: Flow<StoreState> = combine(
        dao.observeItems(),
        dao.observeBonusSlots(),
        dao.observeEntitlement()
    ) { items, bonus, entitlementEntity ->
        val entitlement = entitlementEntity?.toDomain() ?: EntitlementState(false, null, null)
        val bonusSlots = bonus.map { it.toDomain() }
        val limitState = LimitState(entitlement.isPro, LimitConstants.BASE_LIMIT, bonusSlots.size)
        StoreState(
            items = items.map { it.toDomain() },
            bonusSlots = bonusSlots,
            entitlement = entitlement,
            limitState = limitState
        )
    }.map { state ->
        purgeExpiredBonusSlots()
        ensureEntitlementExists()
        state
    }

    suspend fun initialize() {
        withContext(dispatcher) {
            ensureEntitlementExists()
            purgeExpiredBonusSlots()
        }
    }

    suspend fun save(generated: GeneratedUuid, label: String?, formatOptions: FormatOptions) {
        withContext(dispatcher) {
            purgeExpiredBonusSlots()
            val entitlement = ensureEntitlementExists()
            val bonusCount = dao.countBonusSlots()
            val limitState = LimitState(entitlement.isPro, LimitConstants.BASE_LIMIT, bonusCount)
            val currentCount = dao.countItems()
            if (!limitState.canAdd(currentCount)) {
                throw LimitReachedException()
            }
            val value = generated.value.toString()
            if (dao.countByValue(value) > 0) {
                throw DuplicateUuidException()
            }
            val entity = UuidItemEntity(
                id = value,
                value = value,
                version = generated.version.raw,
                createdAt = generated.createdAt.toEpochMilli(),
                label = label,
                namespace = generated.namespace?.toString(),
                name = generated.name,
                styleFlags = formatOptions.raw
            )
            dao.insertItem(entity)
        }
    }

    suspend fun delete(record: UuidRecord) {
        withContext(dispatcher) {
            dao.deleteItem(record.id)
        }
    }

    suspend fun removeAll() {
        withContext(dispatcher) {
            dao.clearItems()
        }
    }

    suspend fun addBonusSlot() {
        withContext(dispatcher) {
            purgeExpiredBonusSlots()
            val count = dao.countBonusSlots()
            if (count >= LimitConstants.MAX_BONUS) {
                throw BonusLimitReachedException()
            }
            val expiresAt = Instant.now().toEpochMilli() + LimitConstants.BONUS_DURATION_MILLIS
            val entity = BonusSlotEntity(
                id = UUID.randomUUID().toString(),
                expiresAt = expiresAt
            )
            dao.insertBonusSlot(entity)
        }
    }

    suspend fun togglePro() {
        withContext(dispatcher) {
            val current = ensureEntitlementExists()
            val now = Instant.now().toEpochMilli()
            val updated = current.copy(
                isPro = !current.isPro,
                proSince = if (!current.isPro) now else null,
                lastSync = now
            )
            dao.upsertEntitlement(updated)
        }
    }

    suspend fun purgeExpiredBonusSlots() {
        withContext(dispatcher) {
            val now = Instant.now().toEpochMilli()
            dao.deleteExpiredBonusSlots(now)
        }
    }

    private suspend fun ensureEntitlementExists(): EntitlementEntity = withContext(dispatcher) {
        val current = dao.getEntitlement()
        if (current != null) {
            current
        } else {
            val entity = EntitlementEntity(id = 0, isPro = false, proSince = null, lastSync = null)
            dao.upsertEntitlement(entity)
            entity
        }
    }

    private fun UuidItemEntity.toDomain(): UuidRecord = UuidRecord(
        id = id,
        rawValue = value,
        version = UuidVersion.fromRaw(version),
        createdAt = Instant.ofEpochMilli(createdAt),
        label = label,
        namespace = namespace,
        name = name,
        formatOptions = FormatOptions(styleFlags)
    )

    private fun BonusSlotEntity.toDomain(): BonusSlot = BonusSlot(
        id = id,
        expiresAt = Instant.ofEpochMilli(expiresAt)
    )

    private fun EntitlementEntity.toDomain(): EntitlementState = EntitlementState(
        isPro = isPro,
        proSince = proSince?.let { Instant.ofEpochMilli(it) },
        lastSyncAt = lastSync?.let { Instant.ofEpochMilli(it) }
    )
}
