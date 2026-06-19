package com.calculocorridas.di

import android.content.Context
import androidx.room.Room
import com.calculocorridas.data.database.AppDatabase
import com.calculocorridas.data.database.dao.RideDao
import com.calculocorridas.data.database.dao.RuleDao
import com.calculocorridas.data.database.dao.SelectorCacheDao
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
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.DATABASE_NAME)
            .fallbackToDestructiveMigration()
            .build()

    @Provides fun provideRideDao(db: AppDatabase): RideDao = db.rideDao()
    @Provides fun provideRuleDao(db: AppDatabase): RuleDao = db.ruleDao()
    @Provides fun provideSelectorCacheDao(db: AppDatabase): SelectorCacheDao = db.selectorCacheDao()
}
