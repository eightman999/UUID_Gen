package com.furinlab.eightman.uuid_gen.domain

enum class UuidVersion(val raw: String, val title: String, val description: String) {
    V4("v4", "UUID v4", "ランダム値ベース"),
    V5("v5", "UUID v5", "名前空間 + 名前 (SHA-1)"),
    V7("v7", "UUID v7", "時刻ベース (RFC 9562)");

    companion object {
        fun fromRaw(raw: String?): UuidVersion = when (raw) {
            V4.raw -> V4
            V5.raw -> V5
            V7.raw -> V7
            else -> V7
        }
    }
}
