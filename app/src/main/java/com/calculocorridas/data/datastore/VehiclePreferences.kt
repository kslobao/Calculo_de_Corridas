package com.calculocorridas.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calculocorridas.domain.entities.FuelType
import com.calculocorridas.domain.entities.VehicleProfile
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.vehicleDataStore: DataStore<Preferences> by preferencesDataStore("vehicle_prefs")

@Singleton
class VehiclePreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.vehicleDataStore

    object Keys {
        val VEHICLE_NAME         = stringPreferencesKey("vehicle_name")
        val FUEL_CONSUMPTION     = doublePreferencesKey("fuel_consumption_km_per_l")
        val FUEL_TYPE            = stringPreferencesKey("fuel_type")
        val FUEL_PRICE           = doublePreferencesKey("fuel_price_per_liter")
    }

    val activeVehicle: Flow<VehicleProfile> = dataStore.data.map { prefs ->
        VehicleProfile(
            name = prefs[Keys.VEHICLE_NAME] ?: "Meu Veículo",
            fuelConsumptionKmPerLiter = prefs[Keys.FUEL_CONSUMPTION] ?: 12.0,
            fuelType = FuelType.valueOf(prefs[Keys.FUEL_TYPE] ?: FuelType.GASOLINE.name),
            fuelPricePerLiter = prefs[Keys.FUEL_PRICE] ?: 6.20,
            isActive = true
        )
    }

    suspend fun save(vehicle: VehicleProfile) = dataStore.edit { prefs ->
        prefs[Keys.VEHICLE_NAME]     = vehicle.name
        prefs[Keys.FUEL_CONSUMPTION] = vehicle.fuelConsumptionKmPerLiter
        prefs[Keys.FUEL_TYPE]        = vehicle.fuelType.name
        prefs[Keys.FUEL_PRICE]       = vehicle.fuelPricePerLiter
    }
}
