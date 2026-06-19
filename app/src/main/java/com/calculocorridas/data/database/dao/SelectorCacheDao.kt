package com.calculocorridas.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calculocorridas.data.database.entities.SelectorCacheEntity

@Dao
interface SelectorCacheDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(entities: List<SelectorCacheEntity>)

    @Query("SELECT * FROM selector_cache WHERE app_key = :appKey AND is_active = 1 ORDER BY priority DESC")
    suspend fun getByApp(appKey: String): List<SelectorCacheEntity>

    @Query("SELECT * FROM selector_cache WHERE app_key = :appKey AND pattern_type = :patternType AND is_active = 1 ORDER BY priority DESC")
    suspend fun getByAppAndType(appKey: String, patternType: String): List<SelectorCacheEntity>

    @Query("SELECT MAX(version) FROM selector_cache")
    suspend fun getCurrentVersion(): Int?

    @Query("SELECT MAX(version) - 1 FROM selector_cache WHERE version < (SELECT MAX(version) FROM selector_cache)")
    suspend fun getPreviousVersion(): Int?

    @Query("DELETE FROM selector_cache WHERE version = (SELECT MAX(version) FROM selector_cache)")
    suspend fun deleteCurrentVersion()

    @Query("UPDATE selector_cache SET is_active = 0 WHERE version != :version")
    suspend fun deactivateAllExcept(version: Int)

    @Query("DELETE FROM selector_cache WHERE version < :version - 1")
    suspend fun deleteOldVersions(version: Int)
}
