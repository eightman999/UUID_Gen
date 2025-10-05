package com.furinlab.eightman.uuid_gen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.furinlab.eightman.uuid_gen.data.BonusLimitReachedException
import com.furinlab.eightman.uuid_gen.data.DuplicateUuidException
import com.furinlab.eightman.uuid_gen.data.LimitReachedException
import com.furinlab.eightman.uuid_gen.data.UserPreferencesRepository
import com.furinlab.eightman.uuid_gen.data.UuidRepository
import com.furinlab.eightman.uuid_gen.domain.FormatOption
import com.furinlab.eightman.uuid_gen.domain.GeneratedUuid
import com.furinlab.eightman.uuid_gen.domain.InvalidNameException
import com.furinlab.eightman.uuid_gen.domain.InvalidNamespaceException
import com.furinlab.eightman.uuid_gen.domain.UuidGenerator
import com.furinlab.eightman.uuid_gen.domain.UuidRecord
import com.furinlab.eightman.uuid_gen.domain.UuidVersion
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class UuidViewModel(
    private val repository: UuidRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    data class GeneratedUiState(
        val generated: GeneratedUuid,
        val formattedValue: String
    )

    val storeState: StateFlow<UuidRepository.StoreState> = repository.storeState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = UuidRepository.StoreState(
            items = emptyList(),
            bonusSlots = emptyList(),
            entitlement = com.furinlab.eightman.uuid_gen.domain.EntitlementState(
                isPro = false,
                proSince = null,
                lastSyncAt = null
            ),
            limitState = com.furinlab.eightman.uuid_gen.domain.LimitState(
                isPro = false,
                baseLimit = com.furinlab.eightman.uuid_gen.domain.LimitConstants.BASE_LIMIT,
                bonusActive = 0
            )
        )
    )

    val preferencesState: StateFlow<UserPreferencesRepository.PreferencesState> =
        preferencesRepository.preferencesState.stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = UserPreferencesRepository.PreferencesState(
                defaultVersion = UuidVersion.V7,
                formatOptions = com.furinlab.eightman.uuid_gen.domain.FormatOptions.DEFAULT
            )
        )

    private val _generated = MutableStateFlow<GeneratedUiState?>(null)
    val generated: StateFlow<GeneratedUiState?> = _generated.asStateFlow()

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = _messages.asSharedFlow()

    init {
        viewModelScope.launch { repository.initialize() }
        viewModelScope.launch {
            preferencesState.collectLatest { prefs ->
                _generated.update { current ->
                    current?.copy(formattedValue = prefs.formatOptions.format(current.generated.value))
                }
            }
        }
    }

    fun generate(version: UuidVersion, namespace: String?, name: String?) {
        viewModelScope.launch {
            try {
                val generatedUuid = UuidGenerator.generate(version, namespace, name)
                val formatted = preferencesState.value.formatOptions.format(generatedUuid.value)
                _generated.value = GeneratedUiState(generatedUuid, formatted)
                preferencesRepository.setDefaultVersion(version)
            } catch (e: InvalidNamespaceException) {
                _messages.tryEmit(e.message ?: "名前空間 UUID が不正です")
            } catch (e: InvalidNameException) {
                _messages.tryEmit(e.message ?: "Name を入力してください")
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "不明なエラーが発生しました")
            }
        }
    }

    fun saveCurrent(label: String?) {
        val current = _generated.value ?: return
        viewModelScope.launch {
            try {
                repository.save(
                    generated = current.generated,
                    label = label?.takeIf { it.isNotBlank() },
                    formatOptions = preferencesState.value.formatOptions
                )
                _messages.tryEmit("保存しました")
            } catch (e: LimitReachedException) {
                _messages.tryEmit(e.message ?: "保存上限に達しました。")
            } catch (e: DuplicateUuidException) {
                _messages.tryEmit(e.message ?: "このUUIDは既に保存されています。")
            } catch (e: Exception) {
                _messages.tryEmit(e.message ?: "保存に失敗しました")
            }
        }
    }

    fun delete(record: UuidRecord) {
        viewModelScope.launch {
            repository.delete(record)
            _messages.tryEmit("削除しました")
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.removeAll()
            _messages.tryEmit("すべて削除しました")
        }
    }

    fun addBonusSlot() {
        viewModelScope.launch {
            try {
                repository.addBonusSlot()
                _messages.tryEmit("ボーナス枠を追加しました")
            } catch (e: BonusLimitReachedException) {
                _messages.tryEmit(e.message ?: "ボーナス枠の上限に達しています。")
            }
        }
    }

    fun togglePro() {
        viewModelScope.launch {
            val currentlyPro = storeState.value.entitlement.isPro
            repository.togglePro()
            val message = if (currentlyPro) {
                "Pro を解除しました"
            } else {
                "Pro を有効化しました"
            }
            _messages.tryEmit(message)
        }
    }

    fun updateFormat(option: FormatOption, enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.setFormatOption(option, enabled)
        }
    }

    fun updateDefaultVersion(version: UuidVersion) {
        viewModelScope.launch {
            preferencesRepository.setDefaultVersion(version)
        }
    }
}
