package com.furinlab.eightman.uuid_gen.domain.model

data class LimitState(
    val isPro: Boolean,
    val baseLimit: Int,
    val bonusActive: Int,
) {
    fun capacity(): Int = if (isPro) Int.MAX_VALUE else baseLimit + bonusActive
    fun canAdd(currentCount: Int): Boolean = currentCount < capacity()
}
