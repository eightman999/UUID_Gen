package com.furinlab.eightman.uuid_gen.domain

import java.time.Instant
import java.util.UUID

data class GeneratedUuid(
    val value: UUID,
    val version: UuidVersion,
    val createdAt: Instant = Instant.now(),
    val namespace: UUID? = null,
    val name: String? = null
)

data class UuidRecord(
    val id: String,
    val rawValue: String,
    val version: UuidVersion,
    val createdAt: Instant,
    val label: String?,
    val namespace: String?,
    val name: String?,
    val formatOptions: FormatOptions
) {
    val formattedValue: String
        get() = formatOptions.format(UUID.fromString(rawValue))
}

data class BonusSlot(
    val id: String,
    val expiresAt: Instant
)

data class EntitlementState(
    val isPro: Boolean,
    val proSince: Instant?,
    val lastSyncAt: Instant?
)

data class LimitState(
    val isPro: Boolean,
    val baseLimit: Int,
    val bonusActive: Int
) {
    val capacity: Int
        get() = if (isPro) Int.MAX_VALUE else baseLimit + bonusActive

    fun canAdd(currentCount: Int): Boolean = isPro || currentCount < capacity
}

object LimitConstants {
    const val BASE_LIMIT = 10
    const val MAX_BONUS = 5
    const val BONUS_DURATION_HOURS = 24L
    val BONUS_DURATION_MILLIS: Long = BONUS_DURATION_HOURS * 60 * 60 * 1000
}
