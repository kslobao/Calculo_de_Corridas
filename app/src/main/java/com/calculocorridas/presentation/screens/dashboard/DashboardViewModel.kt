package com.calculocorridas.presentation.screens.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.domain.entities.DashboardStats
import com.calculocorridas.domain.usecases.ride.GetDashboardStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

enum class DashboardPeriod { TODAY, WEEK, MONTH }

data class DashboardUiState(
    val stats: DashboardStats? = null,
    val period: DashboardPeriod = DashboardPeriod.TODAY,
    val isLoading: Boolean = false
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getStatsUseCase: GetDashboardStatsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init { loadStats(DashboardPeriod.TODAY) }

    fun setPeriod(period: DashboardPeriod) {
        _uiState.value = _uiState.value.copy(period = period)
        loadStats(period)
    }

    private fun loadStats(period: DashboardPeriod) {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val (from, to) = periodRange(period)
            val stats = getStatsUseCase(from, to)
            _uiState.value = _uiState.value.copy(stats = stats, isLoading = false)
        }
    }

    private fun periodRange(period: DashboardPeriod): Pair<Long, Long> {
        val cal = Calendar.getInstance()
        val to = cal.timeInMillis
        when (period) {
            DashboardPeriod.TODAY -> cal.set(Calendar.HOUR_OF_DAY, 0)
                .also { cal.set(Calendar.MINUTE, 0); cal.set(Calendar.SECOND, 0); cal.set(Calendar.MILLISECOND, 0) }
            DashboardPeriod.WEEK  -> cal.add(Calendar.DAY_OF_YEAR, -7)
            DashboardPeriod.MONTH -> cal.add(Calendar.MONTH, -1)
        }
        return cal.timeInMillis to to
    }
}
