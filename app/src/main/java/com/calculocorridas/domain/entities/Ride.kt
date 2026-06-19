package com.calculocorridas.domain.entities

import com.calculocorridas.domain.engine.RideClassification

data class Ride(
    val id: Long = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val appSource: AppSource,
    val rawValue: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val origin: String? = null,
    val destination: String? = null,
    val category: String? = null,
    val city: String? = null,
    val valuePerKm: Double,
    val valuePerHour: Double,
    val fuelCost: Double,
    val netProfit: Double,
    val classification: RideClassification,
    val accepted: AcceptanceStatus = AcceptanceStatus.NOT_RECORDED
)

enum class AcceptanceStatus(val value: Int) {
    NOT_RECORDED(0),
    ACCEPTED(1),
    DECLINED(2);

    companion object {
        fun fromValue(value: Int): AcceptanceStatus =
            entries.find { it.value == value } ?: NOT_RECORDED
    }
}
