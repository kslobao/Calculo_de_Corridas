package com.calculocorridas.domain.engine

import com.calculocorridas.domain.entities.VehicleProfile
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RideCalculationEngine @Inject constructor() {

    data class Metrics(
        val valuePerKm: Double,
        val valuePerHour: Double,
        val fuelCost: Double,
        val netProfit: Double
    )

    fun calculate(
        rawValue: Double,
        distanceKm: Double,
        durationMin: Double,
        vehicle: VehicleProfile
    ): Metrics {
        val valuePerKm = if (distanceKm > 0) rawValue / distanceKm else 0.0
        val durationHours = durationMin / 60.0
        val valuePerHour = if (durationHours > 0) rawValue / durationHours else 0.0
        val fuelCost = distanceKm * vehicle.costPerKm
        val netProfit = rawValue - fuelCost

        return Metrics(
            valuePerKm = valuePerKm.roundTo(2),
            valuePerHour = valuePerHour.roundTo(2),
            fuelCost = fuelCost.roundTo(2),
            netProfit = netProfit.roundTo(2)
        )
    }

    private fun Double.roundTo(decimals: Int): Double {
        var multiplier = 1.0
        repeat(decimals) { multiplier *= 10 }
        return kotlin.math.round(this * multiplier) / multiplier
    }
}
