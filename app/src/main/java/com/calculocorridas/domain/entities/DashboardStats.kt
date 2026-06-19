package com.calculocorridas.domain.entities

data class DashboardStats(
    val totalEarnings: Double,
    val averageValuePerKm: Double,
    val averageValuePerHour: Double,
    val totalRides: Int,
    val acceptedRides: Int,
    val excellentRides: Int,
    val goodRides: Int,
    val poorRides: Int,
    val earningsByDay: List<DayEarning>,
    val ridesByApp: Map<AppSource, Int>
) {
    val acceptanceRate: Double
        get() = if (totalRides > 0) (acceptedRides.toDouble() / totalRides) * 100 else 0.0
}

data class DayEarning(
    val epochDay: Long,
    val totalValue: Double,
    val rideCount: Int
)
