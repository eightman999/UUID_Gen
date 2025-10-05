package com.furinlab.eightman.uuid_gen.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.furinlab.eightman.uuid_gen.data.UserPreferencesRepository
import com.furinlab.eightman.uuid_gen.data.UuidRepository

class UuidViewModelFactory(
    private val repository: UuidRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(UuidViewModel::class.java)) {
            return UuidViewModel(repository, preferencesRepository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
