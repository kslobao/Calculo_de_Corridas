package com.calculocorridas.domain.entities

import com.calculocorridas.domain.engine.RideClassification

data class Rule(
    val id: Long = 0,
    val name: String,
    val condition: RuleCondition,
    val action: RuleAction,
    val enabled: Boolean = true,
    val priority: Int = 0,
    val appFilter: Set<AppSource>? = null
)

data class RuleCondition(
    val field: RuleField,
    val operator: RuleOperator,
    val threshold: Double
)

enum class RuleField(val label: String) {
    VALUE_PER_KM("R$/km"),
    VALUE_PER_HOUR("R$/hora"),
    RAW_VALUE("Valor total"),
    DISTANCE_KM("Distância (km)"),
    NET_PROFIT("Lucro líquido");
}

enum class RuleOperator(val symbol: String) {
    GREATER_THAN_OR_EQUAL(">="),
    GREATER_THAN(">"),
    LESS_THAN("<"),
    LESS_THAN_OR_EQUAL("<="),
    EQUAL("=");
}

enum class RuleAction(val classification: RideClassification?) {
    GREEN(RideClassification.EXCELLENT),
    YELLOW(RideClassification.GOOD),
    RED(RideClassification.POOR),
    NOTIFY(null);
}
