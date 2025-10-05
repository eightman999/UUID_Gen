package com.furinlab.eightman.uuid_gen.di

import android.content.Context
import androidx.room.Room
import com.furinlab.eightman.uuid_gen.data.local.UuidDatabase
import com.furinlab.eightman.uuid_gen.data.repository.DefaultUuidRepository
import com.furinlab.eightman.uuid_gen.data.repository.UuidRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DataModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): UuidDatabase =
        Room.databaseBuilder(context, UuidDatabase::class.java, "uuid_gen.db").build()

    @Provides
    fun provideUuidItemDao(database: UuidDatabase) = database.uuidItemDao()

    @Provides
    fun provideBonusSlotDao(database: UuidDatabase) = database.bonusSlotDao()

    @Provides
    fun provideEntitlementDao(database: UuidDatabase) = database.entitlementDao()

    @Provides
    @Singleton
    fun provideRepository(default: DefaultUuidRepository): UuidRepository = default
}
