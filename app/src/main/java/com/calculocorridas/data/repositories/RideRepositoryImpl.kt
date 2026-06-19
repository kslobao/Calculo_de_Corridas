package com.calculocorridas.data.repositories

import com.calculocorridas.data.database.dao.AppCountRow
import com.calculocorridas.data.database.dao.RideDao
import com.calculocorridas.data.database.entities.RideEntity
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.entities.DashboardStats
import com.calculocorridas.domain.entities.DayEarning
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.engine.RideClassification
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideRepositoryImpl @Inject constructor(
    private val dao: RideDao
) : RideRepository {

    override suspend fun save(ride: Ride): Long =
        dao.insert(RideEntity.fromDomain(ride))

    override fun observeAll(): Flow<List<Ride>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override fun observeByApp(source: AppSource): Flow<List<Ride>> =
        dao.observeByApp(source.key).map { list -> list.map { it.toDomain() } }

    override suspend fun getById(id: Long): Ride? =
        dao.getById(id)?.toDomain()

    override suspend fun getPage(limit: Int, offset: Int): List<Ride> =
        dao.getPage(limit, offset).map { it.toDomain() }

    override suspend fun getRecent(limitDays: Int): List<Ride> {
        val fromMs = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(limitDays.toLong())
        return dao.getFrom(fromMs).map { it.toDomain() }
    }

    override suspend fun delete(id: Long) = dao.deleteById(id)

    override suspend fun deleteOlderThan(epochMs: Long) = dao.deleteOlderThan(epochMs)

    override suspend fun count(): Int = dao.count()

    override suspend fun getDashboardStats(fromEpochMs: Long, toEpochMs: Long): DashboardStats {
        val total = dao.totalEarnings(fromEpochMs, toEpochMs) ?: 0.0
        val avgKm = dao.avgValuePerKm(fromEpochMs, toEpochMs) ?: 0.0
        val avgHour = dao.avgValuePerHour(fromEpochMs, toEpochMs) ?: 0.0
        val totalRides = dao.countBetween(fromEpochMs, toEpochMs)
        val accepted = dao.countAccepted(fromEpochMs, toEpochMs)
        val excellent = dao.countByClassification(RideClassification.EXCELLENT.key, fromEpochMs, toEpochMs)
        val good = dao.countByClassification(RideClassification.GOOD.key, fromEpochMs, toEpochMs)
        val poor = dao.countByClassification(RideClassification.POOR.key, fromEpochMs, toEpochMs)
        val byDay = dao.getEarningsByDay(fromEpochMs, toEpochMs).map {
            DayEarning(it.epochDay, it.totalValue, it.rideCount)
        }
        val byApp = dao.countByApp(fromEpochMs, toEpochMs).associate { row: AppCountRow ->
            (AppSource.fromKey(row.app_source) ?: AppSource.UBER) to row.cnt
        }

        return DashboardStats(
            totalEarnings = total,
            averageValuePerKm = avgKm,
            averageValuePerHour = avgHour,
            totalRides = totalRides,
            acceptedRides = accepted,
            excellentRides = excellent,
            goodRides = good,
            poorRides = poor,
            earningsByDay = byDay,
            ridesByApp = byApp
        )
    }
}
