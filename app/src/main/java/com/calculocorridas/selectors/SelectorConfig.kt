package com.calculocorridas.selectors

import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.domain.entities.SelectorPattern
import com.calculocorridas.domain.entities.SelectorType
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

    // Retorna do cache remoto ou usa padrões regex embutidos como fallback
    fun getFor(appKey: String): AppSelectors = _configs.value[appKey] ?: FALLBACK_SELECTORS[appKey] ?: GENERIC_FALLBACK

    companion object {
        private fun regexPatterns(vararg patterns: String): List<SelectorPattern> =
            patterns.mapIndexed { i, p ->
                SelectorPattern(SelectorType.REGEX, p, 100 - i * 10)
            }

        private val PRICE_PATTERNS    = regexPatterns("""R\$\s*([\d.,]+)""", """(\d{1,3}(?:[.,]\d{3})*[.,]\d{2})""")
        private val DISTANCE_PATTERNS = regexPatterns("""([\d.,]+)\s*km""", """([\d.,]+)\s*quilômetro""")
        private val TIME_PATTERNS     = regexPatterns("""(\d+)\s*min""", """(\d+)\s*h\s*(\d+)\s*min""")

        private val GENERIC_FALLBACK = AppSelectors(
            pricePatterns       = PRICE_PATTERNS,
            distancePatterns    = DISTANCE_PATTERNS,
            timePatterns        = TIME_PATTERNS,
            originPatterns      = emptyList(),
            destinationPatterns = emptyList(),
            categoryPatterns    = emptyList()
        )

        val FALLBACK_SELECTORS: Map<String, AppSelectors> = mapOf(
            "uber"    to GENERIC_FALLBACK,
            "99"      to GENERIC_FALLBACK,
            "indrive" to GENERIC_FALLBACK,
            "ifood"   to GENERIC_FALLBACK
        )
    }
}
