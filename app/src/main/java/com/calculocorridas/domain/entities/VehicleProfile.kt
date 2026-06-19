package com.calculocorridas.domain.entities

data class VehicleProfile(
    val id: Long = 0,
    val name: String,
    val fuelConsumptionKmPerLiter: Double,
    val fuelType: FuelType,
    val fuelPricePerLiter: Double,
    val isActive: Boolean = false
) {
    val costPerKm: Double
        get() = fuelPricePerLiter / fuelConsumptionKmPerLiter
}

enum class FuelType(val label: String) {
    GASOLINE("Gasolina"),
    ETHANOL("Etanol"),
    DIESEL("Diesel"),
    CNG("GNV");
}
