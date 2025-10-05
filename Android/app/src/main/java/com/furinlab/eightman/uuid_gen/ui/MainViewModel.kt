package com.furinlab.eightman.uuid_gen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furinlab.eightman.uuid_gen.data.preferences.UserPreferencesRepository
import com.furinlab.eightman.uuid_gen.data.repository.UuidRepository
import com.furinlab.eightman.uuid_gen.domain.logic.UuidGenerator
import com.furinlab.eightman.uuid_gen.domain.model.BonusSlot
import com.furinlab.eightman.uuid_gen.domain.model.Entitlement
import com.furinlab.eightman.uuid_gen.domain.model.LimitState
import com.furinlab.eightman.uuid_gen.domain.model.UuidFormatOptions
import com.furinlab.eightman.uuid_gen.domain.model.UuidItem
import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion
import com.furinlab.eightman.uuid_gen.domain.model.isExpired
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

private const val BASE_LIMIT = 10
private const val MAX_BONUS = 5
private const val BONUS_DURATION_MS = 24 * 60 * 60 * 1000L

@HiltViewModel
class MainViewModel @Inject constructor(
    private val repository: UuidRepository,
    private val preferencesRepository: UserPreferencesRepository,
) : ViewModel() {

    private val namespaceInput = MutableStateFlow("")
    private val nameInput = MutableStateFlow("")
    private val selectedVersion = MutableStateFlow(UuidVersion.V4)
    private val formatOptions = MutableStateFlow(UuidFormatOptions())
    private val generatedValue = MutableStateFlow<String?>(null)
    private val errorMessage = MutableStateFlow<String?>(null)

    init {
        viewModelScope.launch {
            preferencesRepository.preferences.collect { prefs ->
                selectedVersion.value = prefs.defaultVersion
                formatOptions.value = prefs.formatOptions
            }
        }

        viewModelScope.launch {
            repository.bonusSlots.collect { slots ->
                val now = System.currentTimeMillis()
                if (slots.any { it.isExpired(now) }) {
                    repository.removeExpiredBonus(now)
                }
            }
        }
    }

    val uiState: StateFlow<MainUiState> = combine(
        selectedVersion,
        namespaceInput,
        nameInput,
        formatOptions,
        generatedValue,
        errorMessage,
        repository.items,
        repository.itemCount,
        repository.bonusSlots,
        repository.entitlement,
    ) { version, namespace, name, format, generated, error, items, count, bonusSlots, entitlement ->
        val now = System.currentTimeMillis()
        val activeBonus = bonusSlots.count { !it.isExpired(now) }
        val limitedBonus = activeBonus.coerceAtMost(MAX_BONUS)
        val limitState = LimitState(
            isPro = entitlement.isPro,
            baseLimit = BASE_LIMIT,
            bonusActive = limitedBonus,
        )
        MainUiState(
            selectedVersion = version,
            namespaceInput = namespace,
            nameInput = name,
            formatOptions = format,
            generatedValue = generated,
            history = items,
            limitState = limitState,
            canSave = limitState.canAdd(count),
            errorMessage = error,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = MainUiState(),
    )

    fun onVersionSelected(version: UuidVersion) {
        selectedVersion.value = version
        viewModelScope.launch {
            preferencesRepository.updateDefaultVersion(version)
        }
    }

    fun onFormatChanged(options: UuidFormatOptions) {
        formatOptions.value = options
        viewModelScope.launch {
            preferencesRepository.updateFormat(options)
        }
    }

    fun onNamespaceChanged(input: String) {
        namespaceInput.value = input
    }

    fun onNameChanged(input: String) {
        nameInput.value = input
    }

    fun generateUuid() {
        val version = selectedVersion.value
        val options = formatOptions.value
        errorMessage.value = null

        try {
            val uuid = when (version) {
                UuidVersion.V4 -> UuidGenerator.generate(version)
                UuidVersion.V5 -> {
                    val namespace = runCatching { UUID.fromString(namespaceInput.value) }.getOrNull()
                        ?: throw IllegalArgumentException("Namespace must be a valid UUID")
                    val name = nameInput.value.takeIf { it.isNotBlank() }
                        ?: throw IllegalArgumentException("Name is required for v5")
                    UuidGenerator.generate(version, namespace = namespace, name = name)
                }
                UuidVersion.V7 -> UuidGenerator.generate(version)
            }
            generatedValue.value = options.apply(uuid.toString())
        } catch (t: Throwable) {
            errorMessage.value = t.message
        }
    }

    fun clearGenerated() {
        generatedValue.value = null
        errorMessage.value = null
    }

    fun saveGenerated() {
        val value = generatedValue.value ?: return
        val version = selectedVersion.value
        viewModelScope.launch {
            if (!uiState.value.canSave) {
                errorMessage.value = "保存上限に達しました"
                return@launch
            }
            val id = UUID.randomUUID().toString()
            val rawUuid = extractRawValue(value)
            val item = UuidItem(
                id = id,
                value = rawUuid,
                version = version,
                createdAt = System.currentTimeMillis(),
                namespace = namespaceInput.value.takeIf { it.isNotBlank() },
                name = nameInput.value.takeIf { it.isNotBlank() },
                format = formatOptions.value,
            )
            repository.save(item)
        }
    }

    fun deleteItem(id: String) {
        viewModelScope.launch {
            repository.delete(id)
        }
    }

    fun togglePro() {
        viewModelScope.launch {
            val current = uiState.value.limitState.isPro
            val newEntitlement = Entitlement(
                isPro = !current,
                proSince = if (!current) System.currentTimeMillis() else null,
                lastSyncAt = System.currentTimeMillis(),
            )
            repository.setEntitlement(newEntitlement)
        }
    }

    fun grantBonusSlot() {
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val activeBonus = uiState.value.limitState.bonusActive
            if (activeBonus >= MAX_BONUS) {
                errorMessage.value = "ボーナス枠は最大です"
                return@launch
            }
            val slot = BonusSlot(
                id = UUID.randomUUID().toString(),
                expiresAt = now + BONUS_DURATION_MS,
            )
            repository.addBonusSlot(slot)
        }
    }

    private fun extractRawValue(formatted: String): String {
        val sanitized = formatted
            .trim { it == '{' || it == '}' }
            .replace("-", "")
            .lowercase()
        return if (sanitized.length == 32) {
            listOf(
                sanitized.substring(0, 8),
                sanitized.substring(8, 12),
                sanitized.substring(12, 16),
                sanitized.substring(16, 20),
                sanitized.substring(20, 32),
            ).joinToString("-")
        } else {
            sanitized
        }
    }
}
