package com.calculocorridas.selectors

import com.calculocorridas.domain.entities.AppSelectors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectorConfigHolder @Inject constructor() {

    private val _configs = MutableStateFlow<Map<String, AppSelectors>>(emptyMap())
    val configs: StateFlow<Map<String, AppSelectors>> = _configs.asStateFlow()

    fun update(appKey: String, selectors: AppSelectors) {
        _configs.value = _configs.value.toMutableMap().also { it[appKey] = selectors }
    }

    fun updateAll(all: Map<String, AppSelectors>) {
        _configs.value = all
    }

    fun getFor(appKey: String): AppSelectors? = _configs.value[appKey]
}
