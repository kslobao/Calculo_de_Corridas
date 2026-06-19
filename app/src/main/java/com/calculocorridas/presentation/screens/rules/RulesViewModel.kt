package com.calculocorridas.presentation.screens.rules

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.domain.entities.Rule
import com.calculocorridas.domain.repositories.RuleRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import javax.inject.Inject

data class RulesUiState(
    val rules: List<Rule> = emptyList(),
    val isLoading: Boolean = true
)

@HiltViewModel
class RulesViewModel @Inject constructor(
    private val ruleRepository: RuleRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RulesUiState())
    val uiState: StateFlow<RulesUiState> = _uiState.asStateFlow()

    init {
        ruleRepository.observeAll()
            .onEach { _uiState.value = _uiState.value.copy(rules = it, isLoading = false) }
            .catch { _uiState.value = _uiState.value.copy(isLoading = false) }
            .launchIn(viewModelScope)
    }

    fun deleteRule(id: Long) = viewModelScope.launch { ruleRepository.delete(id) }

    fun toggleRule(id: Long, enabled: Boolean) =
        viewModelScope.launch { ruleRepository.setEnabled(id, enabled) }

    fun saveRule(rule: Rule) = viewModelScope.launch { ruleRepository.save(rule) }
}
