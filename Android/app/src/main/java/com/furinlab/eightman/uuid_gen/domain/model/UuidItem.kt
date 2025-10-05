package com.furinlab.eightman.uuid_gen.domain.model

data class UuidItem(
    val id: String,
    val value: String,
    val version: UuidVersion,
    val createdAt: Long,
    val label: String? = null,
    val namespace: String? = null,
    val name: String? = null,
    val format: UuidFormatOptions = UuidFormatOptions(),
)
