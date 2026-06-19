package com.calculocorridas.domain.usecases.ride

import com.calculocorridas.domain.entities.DashboardStats
import com.calculocorridas.domain.repositories.RideRepository
import javax.inject.Inject

class GetDashboardStatsUseCase @Inject constructor(
    private val repository: RideRepository
) {
    suspend operator fun invoke(fromEpochMs: Long, toEpochMs: Long): DashboardStats =
        repository.getDashboardStats(fromEpochMs, toEpochMs)
}
