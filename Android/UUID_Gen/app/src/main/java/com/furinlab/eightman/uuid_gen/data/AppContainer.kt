package com.furinlab.eightman.uuid_gen.data

import android.content.Context
import com.furinlab.eightman.uuid_gen.data.local.UuidDatabase

class AppContainer(context: Context) {
    private val database: UuidDatabase = UuidDatabase.build(context)

    val uuidRepository: UuidRepository = UuidRepository(database.uuidDao())
    val preferencesRepository: UserPreferencesRepository = UserPreferencesRepository(context)
}
