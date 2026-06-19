package com.calculocorridas.presentation.screens.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.usecases.ride.GetRidesUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import javax.inject.Inject

data class HistoryUiState(
    val rides: List<Ride> = emptyList(),
    val filteredRides: List<Ride> = emptyList(),
    val selectedApp: AppSource? = null,
    val isLoading: Boolean = true
)

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val getRidesUseCase: GetRidesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(HistoryUiState())
    val uiState: StateFlow<HistoryUiState> = _uiState.asStateFlow()

    init {
        getRidesUseCase.all()
            .onEach { rides ->
                _uiState.value = _uiState.value.copy(
                    rides = rides,
                    filteredRides = applyFilter(rides, _uiState.value.selectedApp),
                    isLoading = false
                )
            }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun setFilter(source: AppSource?) {
        _uiState.value = _uiState.value.copy(
            selectedApp = source,
            filteredRides = applyFilter(_uiState.value.rides, source)
        )
    }

    private fun applyFilter(rides: List<Ride>, source: AppSource?): List<Ride> =
        if (source == null) rides else rides.filter { it.appSource == source }
}
