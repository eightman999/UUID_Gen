package com.furinlab.eightman.uuid_gen.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.furinlab.eightman.uuid_gen.domain.model.UuidFormatOptions
import com.furinlab.eightman.uuid_gen.domain.model.UuidVersion
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_prefs")

@Singleton
class UserPreferencesRepository @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val preferences: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        UserPreferences(
            defaultVersion = UuidVersion.fromString(prefs[DEFAULT_VERSION]),
            formatOptions = UuidFormatOptions(
                hyphenated = prefs[HYPHENATED] ?: true,
                uppercase = prefs[UPPERCASE] ?: false,
                braces = prefs[BRACES] ?: false,
            ),
        )
    }

    suspend fun updateDefaultVersion(version: UuidVersion) {
        context.dataStore.edit { prefs ->
            prefs[DEFAULT_VERSION] = version.displayName
        }
    }

    suspend fun updateFormat(options: UuidFormatOptions) {
        context.dataStore.edit { prefs ->
            prefs[HYPHENATED] = options.hyphenated
            prefs[UPPERCASE] = options.uppercase
            prefs[BRACES] = options.braces
        }
    }

    data class UserPreferences(
        val defaultVersion: UuidVersion,
        val formatOptions: UuidFormatOptions,
    )

    companion object {
        private val DEFAULT_VERSION = stringPreferencesKey("default_version")
        private val HYPHENATED = booleanPreferencesKey("hyphenated")
        private val UPPERCASE = booleanPreferencesKey("uppercase")
        private val BRACES = booleanPreferencesKey("braces")
    }
}
