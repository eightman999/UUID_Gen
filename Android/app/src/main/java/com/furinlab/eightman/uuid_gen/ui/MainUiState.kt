package com.furinlab.eightman.uuid_gen.ui

import com.furinlab.eightman.uuid_gen.domain.model.LimitState
import com.furinlab.eightman.uuid_gen.domain.model.UuidFormatOptions
import com.furinlab.eightman.uuid_gen.domain.model.UuidItem
import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion

data class MainUiState(
    val selectedVersion: UuidVersion = UuidVersion.V4,
    val namespaceInput: String = "",
    val nameInput: String = "",
    val formatOptions: UuidFormatOptions = UuidFormatOptions(),
    val generatedValue: String? = null,
    val history: List<UuidItem> = emptyList(),
    val limitState: LimitState = LimitState(isPro = false, baseLimit = 10, bonusActive = 0),
    val canSave: Boolean = true,
    val errorMessage: String? = null,
)
