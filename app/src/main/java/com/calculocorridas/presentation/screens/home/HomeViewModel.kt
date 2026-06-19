package com.calculocorridas.presentation.screens.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.licensing.LicenseValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isServiceActive: Boolean = false,
    val lastRide: Ride? = null,
    val todayEarnings: Double = 0.0,
    val avgValuePerKm: Double = 0.0,
    val totalRidesToday: Int = 0,
    val isPro: Boolean = false
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val rideRepository: RideRepository,
    private val licenseValidator: LicenseValidator
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        observeRides()
        observeLicense()
        viewModelScope.launch { licenseValidator.loadCached() }
    }

    private fun observeRides() {
        rideRepository.observeAll()
            .onEach { rides ->
                val today = System.currentTimeMillis().let { now ->
                    val start = now - (now % 86_400_000)
                    rides.filter { it.createdAt >= start }
                }
                _uiState.value = _uiState.value.copy(
                    lastRide = rides.firstOrNull(),
                    todayEarnings = today.sumOf { it.rawValue },
                    avgValuePerKm = if (today.isEmpty()) 0.0 else today.map { it.valuePerKm }.average(),
                    totalRidesToday = today.size
                )
            }
            .catch { }
            .launchIn(viewModelScope)
    }

    private fun observeLicense() {
        licenseValidator.license
            .onEach { _uiState.value = _uiState.value.copy(isPro = it.isValid) }
            .launchIn(viewModelScope)
    }

    fun setServiceActive(active: Boolean) {
        _uiState.value = _uiState.value.copy(isServiceActive = active)
    }
}
