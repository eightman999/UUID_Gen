package com.furinlab.eightman.uuid_gen.domain.model

data class Entitlement(
    val isPro: Boolean,
    val proSince: Long?,
    val lastSyncAt: Long?,
) {
    companion object {
        val Default = Entitlement(isPro = false, proSince = null, lastSyncAt = null)
    }
}
