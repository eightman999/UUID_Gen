package com.furinlab.eightman.uuid_gen.domain.model

enum class UuidVersion(val displayName: String) {
    V4("v4"),
    V5("v5"),
    V7("v7");

    companion object {
        fun fromString(value: String?): UuidVersion = when (value?.lowercase()) {
            "v5" -> V5
            "v7" -> V7
            else -> V4
        }
    }
}
