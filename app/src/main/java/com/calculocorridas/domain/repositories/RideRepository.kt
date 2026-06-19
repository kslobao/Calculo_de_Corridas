package com.calculocorridas.domain.repositories

import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.entities.DashboardStats
import com.calculocorridas.domain.entities.Ride
import kotlinx.coroutines.flow.Flow

interface RideRepository {
    suspend fun save(ride: Ride): Long
    fun observeAll(): Flow<List<Ride>>
    fun observeByApp(source: AppSource): Flow<List<Ride>>
    suspend fun getById(id: Long): Ride?
    suspend fun getPage(limit: Int, offset: Int): List<Ride>
    suspend fun getRecent(limitDays: Int): List<Ride>
    suspend fun delete(id: Long)
    suspend fun deleteOlderThan(epochMs: Long)
    suspend fun getDashboardStats(fromEpochMs: Long, toEpochMs: Long): DashboardStats
    suspend fun count(): Int
}
