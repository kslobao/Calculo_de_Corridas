package com.calculocorridas.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.calculocorridas.domain.entities.AcceptanceStatus
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.domain.entities.Ride

@Entity(
    tableName = "rides",
    indices = [
        Index("created_at"),
        Index("app_source")
    ]
)
data class RideEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "app_source") val appSource: String,
    @ColumnInfo(name = "raw_value") val rawValue: Double,
    @ColumnInfo(name = "distance_km") val distanceKm: Double,
    @ColumnInfo(name = "duration_min") val durationMin: Double,
    val origin: String?,
    val destination: String?,
    val category: String?,
    val city: String?,
    @ColumnInfo(name = "value_per_km") val valuePerKm: Double,
    @ColumnInfo(name = "value_per_hour") val valuePerHour: Double,
    @ColumnInfo(name = "fuel_cost") val fuelCost: Double,
    @ColumnInfo(name = "net_profit") val netProfit: Double,
    val classification: String,
    val accepted: Int = 0
) {
    fun toDomain(): Ride = Ride(
        id = id,
        createdAt = createdAt,
        appSource = AppSource.fromKey(appSource) ?: AppSource.UBER,
        rawValue = rawValue,
        distanceKm = distanceKm,
        durationMin = durationMin,
        origin = origin,
        destination = destination,
        category = category,
        city = city,
        valuePerKm = valuePerKm,
        valuePerHour = valuePerHour,
        fuelCost = fuelCost,
        netProfit = netProfit,
        classification = RideClassification.fromKey(classification),
        accepted = AcceptanceStatus.fromValue(accepted)
    )

    companion object {
        fun fromDomain(ride: Ride) = RideEntity(
            id = ride.id,
            createdAt = ride.createdAt,
            appSource = ride.appSource.key,
            rawValue = ride.rawValue,
            distanceKm = ride.distanceKm,
            durationMin = ride.durationMin,
            origin = ride.origin,
            destination = ride.destination,
            category = ride.category,
            city = ride.city,
            valuePerKm = ride.valuePerKm,
            valuePerHour = ride.valuePerHour,
            fuelCost = ride.fuelCost,
            netProfit = ride.netProfit,
            classification = ride.classification.key,
            accepted = ride.accepted.value
        )
    }
}
