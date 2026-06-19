package com.calculocorridas.presentation.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.data.datastore.UserPreferences
import com.calculocorridas.data.datastore.VehiclePreferences
import com.calculocorridas.domain.entities.FuelType
import com.calculocorridas.domain.entities.VehicleProfile
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SettingsUiState(
    val vehicleName: String = "Meu Veículo",
    val fuelConsumption: Double = 12.0,
    val fuelType: FuelType = FuelType.GASOLINE,
    val fuelPrice: Double = 6.20,
    val minValuePerKm: Double = 2.00,
    val minValuePerHour: Double = 30.00,
    val monitorUber: Boolean = true,
    val monitor99: Boolean = true,
    val monitorInDrive: Boolean = true,
    val monitorIFood: Boolean = true,
    val overlayTransparency: Float = 0.95f,
    val overlayAutoClose: Int = 10
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPrefs: UserPreferences,
    private val vehiclePrefs: VehiclePreferences
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        combine(
            vehiclePrefs.activeVehicle,
            userPrefs.minValuePerKm,
            userPrefs.minValuePerHour,
            userPrefs.monitorUber,
            userPrefs.monitor99
        ) { vehicle, minKm, minHour, uber, nn ->
            _state.value = _state.value.copy(
                vehicleName = vehicle.name,
                fuelConsumption = vehicle.fuelConsumptionKmPerLiter,
                fuelType = vehicle.fuelType,
                fuelPrice = vehicle.fuelPricePerLiter,
                minValuePerKm = minKm,
                minValuePerHour = minHour,
                monitorUber = uber,
                monitor99 = nn
            )
        }.launchIn(viewModelScope)

        combine(userPrefs.monitorInDrive, userPrefs.monitorIFood,
            userPrefs.overlayTransparency, userPrefs.overlayAutoCloseSeconds
        ) { inDrive, iFood, transparency, autoClose ->
            _state.value = _state.value.copy(
                monitorInDrive = inDrive,
                monitorIFood = iFood,
                overlayTransparency = transparency,
                overlayAutoClose = autoClose
            )
        }.launchIn(viewModelScope)
    }

    fun saveVehicle() = viewModelScope.launch {
        vehiclePrefs.save(VehicleProfile(
            name = _state.value.vehicleName,
            fuelConsumptionKmPerLiter = _state.value.fuelConsumption,
            fuelType = _state.value.fuelType,
            fuelPricePerLiter = _state.value.fuelPrice,
            isActive = true
        ))
    }

    fun setFuelConsumption(v: Double) { _state.value = _state.value.copy(fuelConsumption = v) }
    fun setFuelType(t: FuelType) { _state.value = _state.value.copy(fuelType = t) }
    fun setFuelPrice(v: Double) { _state.value = _state.value.copy(fuelPrice = v) }
    fun setMinKm(v: Double) = viewModelScope.launch { userPrefs.setMinValuePerKm(v); _state.value = _state.value.copy(minValuePerKm = v) }
    fun setMinHour(v: Double) = viewModelScope.launch { userPrefs.setMinValuePerHour(v); _state.value = _state.value.copy(minValuePerHour = v) }
    fun setMonitorUber(v: Boolean) = viewModelScope.launch { userPrefs.setMonitorUber(v) }
    fun setMonitor99(v: Boolean) = viewModelScope.launch { userPrefs.setMonitor99(v) }
    fun setMonitorInDrive(v: Boolean) = viewModelScope.launch { userPrefs.setMonitorInDrive(v) }
    fun setMonitorIFood(v: Boolean) = viewModelScope.launch { userPrefs.setMonitorIFood(v) }
}
