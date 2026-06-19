package com.calculocorridas.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.calculocorridas.data.database.dao.RideDao
import com.calculocorridas.data.database.dao.RuleDao
import com.calculocorridas.data.database.dao.SelectorCacheDao
import com.calculocorridas.data.database.entities.RideEntity
import com.calculocorridas.data.database.entities.RuleEntity
import com.calculocorridas.data.database.entities.SelectorCacheEntity

@Database(
    entities = [
        RideEntity::class,
        RuleEntity::class,
        SelectorCacheEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun rideDao(): RideDao
    abstract fun ruleDao(): RuleDao
    abstract fun selectorCacheDao(): SelectorCacheDao

    companion object {
        const val DATABASE_NAME = "calculo_corridas.db"
    }
}
