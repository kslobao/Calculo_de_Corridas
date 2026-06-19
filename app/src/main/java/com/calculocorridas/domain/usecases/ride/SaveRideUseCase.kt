package com.calculocorridas.domain.usecases.ride

import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import javax.inject.Inject

class SaveRideUseCase @Inject constructor(
    private val repository: RideRepository
) {
    suspend operator fun invoke(ride: Ride): Long =
        repository.save(ride)
}
