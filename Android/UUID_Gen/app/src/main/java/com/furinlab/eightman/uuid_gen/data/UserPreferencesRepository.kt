package com.furinlab.eightman.uuid_gen.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.furinlab.eightman.uuid_gen.domain.FormatOption
import com.furinlab.eightman.uuid_gen.domain.FormatOptions
import com.furinlab.eightman.uuid_gen.domain.UuidVersion
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("user_prefs")

class UserPreferencesRepository(private val context: Context) {
    data class PreferencesState(
        val defaultVersion: UuidVersion,
        val formatOptions: FormatOptions
    )

    val preferencesState: Flow<PreferencesState> = context.dataStore.data.map { prefs ->
        val defaultVersion = UuidVersion.fromRaw(prefs[DEFAULT_VERSION] ?: UuidVersion.V7.raw)
        val optionsRaw = prefs[FORMAT_OPTIONS] ?: FormatOptions.DEFAULT.raw
        PreferencesState(defaultVersion, FormatOptions(optionsRaw))
    }

    suspend fun setDefaultVersion(version: UuidVersion) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_VERSION] = version.raw
        }
    }

    suspend fun setFormatOption(option: FormatOption, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[FORMAT_OPTIONS] ?: FormatOptions.DEFAULT.raw
            val updated = FormatOptions(current).toggle(option, enabled)
            prefs[FORMAT_OPTIONS] = updated.raw
        }
    }

    companion object {
        private val DEFAULT_VERSION = stringPreferencesKey("default_version")
        private val FORMAT_OPTIONS = longPreferencesKey("format_options")
    }
}
