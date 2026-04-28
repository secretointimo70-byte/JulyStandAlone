package com.july.offline.di

import android.content.Context
import androidx.room.Room
import com.july.offline.data.db.JulyDatabase
import com.july.offline.data.db.dao.DiagnosticsDao
import com.july.offline.data.db.dao.MessageDao
import com.july.offline.data.db.dao.SessionDao
import com.july.offline.data.db.dao.SurvivalContentDao
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
    fun provideJulyDatabase(@ApplicationContext context: Context): JulyDatabase =
        Room.databaseBuilder(
            context,
            JulyDatabase::class.java,
            "july_offline.db"
        )
        .fallbackToDestructiveMigration()
        .build()

    @Provides
    fun provideSessionDao(db: JulyDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMessageDao(db: JulyDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideDiagnosticsDao(db: JulyDatabase): DiagnosticsDao = db.diagnosticsDao()

    @Provides
    fun provideSurvivalContentDao(db: JulyDatabase): SurvivalContentDao = db.survivalContentDao()
}
