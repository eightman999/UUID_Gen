package com.furinlab.eightman.uuid_gen.domain.model

data class BonusSlot(
    val id: String,
    val expiresAt: Long,
)

fun BonusSlot.isExpired(now: Long): Boolean = expiresAt <= now
