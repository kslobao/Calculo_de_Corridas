package com.calculocorridas.data.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.calculocorridas.data.database.entities.RideEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RideDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: RideEntity): Long

    @Query("SELECT * FROM rides ORDER BY created_at DESC")
    fun observeAll(): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE app_source = :source ORDER BY created_at DESC")
    fun observeByApp(source: String): Flow<List<RideEntity>>

    @Query("SELECT * FROM rides WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): RideEntity?

    @Query("SELECT * FROM rides ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    suspend fun getPage(limit: Int, offset: Int): List<RideEntity>

    @Query("SELECT * FROM rides WHERE created_at >= :fromEpochMs ORDER BY created_at DESC")
    suspend fun getFrom(fromEpochMs: Long): List<RideEntity>

    @Query("SELECT * FROM rides WHERE created_at BETWEEN :from AND :to ORDER BY created_at DESC")
    suspend fun getBetween(from: Long, to: Long): List<RideEntity>

    @Query("DELETE FROM rides WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM rides WHERE created_at < :epochMs")
    suspend fun deleteOlderThan(epochMs: Long)

    @Query("SELECT COUNT(*) FROM rides")
    suspend fun count(): Int

    @Query("SELECT COUNT(*) FROM rides WHERE created_at BETWEEN :from AND :to")
    suspend fun countBetween(from: Long, to: Long): Int

    @Query("""
        SELECT strftime('%s', date(created_at / 1000, 'unixepoch')) * 1000 as epochDay,
               SUM(raw_value) as totalValue,
               COUNT(*) as rideCount
        FROM rides
        WHERE created_at BETWEEN :from AND :to
        GROUP BY epochDay
        ORDER BY epochDay ASC
    """)
    suspend fun getEarningsByDay(from: Long, to: Long): List<DayEarningRow>

    @Query("SELECT AVG(value_per_km) FROM rides WHERE created_at BETWEEN :from AND :to")
    suspend fun avgValuePerKm(from: Long, to: Long): Double?

    @Query("SELECT AVG(value_per_hour) FROM rides WHERE created_at BETWEEN :from AND :to")
    suspend fun avgValuePerHour(from: Long, to: Long): Double?

    @Query("SELECT SUM(raw_value) FROM rides WHERE created_at BETWEEN :from AND :to")
    suspend fun totalEarnings(from: Long, to: Long): Double?

    @Query("SELECT COUNT(*) FROM rides WHERE classification = :classification AND created_at BETWEEN :from AND :to")
    suspend fun countByClassification(classification: String, from: Long, to: Long): Int

    @Query("SELECT COUNT(*) FROM rides WHERE accepted = 1 AND created_at BETWEEN :from AND :to")
    suspend fun countAccepted(from: Long, to: Long): Int

    @Query("SELECT app_source, COUNT(*) as cnt FROM rides WHERE created_at BETWEEN :from AND :to GROUP BY app_source")
    suspend fun countByApp(from: Long, to: Long): List<AppCountRow>
}

data class DayEarningRow(val epochDay: Long, val totalValue: Double, val rideCount: Int)
data class AppCountRow(val app_source: String, val cnt: Int)
