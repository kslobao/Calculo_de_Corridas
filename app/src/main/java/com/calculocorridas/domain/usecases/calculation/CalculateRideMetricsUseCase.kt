package com.calculocorridas.domain.usecases.calculation

import com.calculocorridas.domain.engine.RideCalculationEngine
import com.calculocorridas.domain.entities.VehicleProfile
import javax.inject.Inject

class CalculateRideMetricsUseCase @Inject constructor(
    private val engine: RideCalculationEngine
) {
    operator fun invoke(
        rawValue: Double,
        distanceKm: Double,
        durationMin: Double,
        vehicle: VehicleProfile
    ): RideCalculationEngine.Metrics =
        engine.calculate(rawValue, distanceKm, durationMin, vehicle)
}
