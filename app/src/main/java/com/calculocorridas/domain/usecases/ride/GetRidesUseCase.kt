package com.calculocorridas.domain.usecases.ride

import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetRidesUseCase @Inject constructor(
    private val repository: RideRepository
) {
    fun all(): Flow<List<Ride>> = repository.observeAll()

    suspend fun page(limit: Int, offset: Int): List<Ride> =
        repository.getPage(limit, offset)

    suspend fun recent(days: Int): List<Ride> =
        repository.getRecent(days)
}
