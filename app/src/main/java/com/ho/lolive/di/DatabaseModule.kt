package com.ho.lolive.di

import android.content.Context
import androidx.room.Room
import com.ho.lolive.data.local.LoliveDatabase
import com.ho.lolive.data.local.dao.LiveRoomDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context,
    ): LoliveDatabase {
        return Room.databaseBuilder(context, LoliveDatabase::class.java, "lolive.db")
            .addMigrations(LoliveDatabase.MIGRATION_1_2)
            .addMigrations(LoliveDatabase.MIGRATION_2_3)
            .fallbackToDestructiveMigrationOnDowngrade()
            .build()
    }

    @Provides
    fun provideLiveRoomDao(database: LoliveDatabase): LiveRoomDao = database.liveRoomDao()
}
