package com.calculocorridas.domain.engine

import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.entities.Rule
import com.calculocorridas.domain.entities.RuleField
import com.calculocorridas.domain.entities.RuleOperator
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleEngine @Inject constructor() {

    fun evaluate(ride: Ride, rules: List<Rule>): RideClassification {
        val applicableRules = rules
            .filter { it.enabled }
            .filter { it.appFilter == null || ride.appSource in it.appFilter }
            .sortedByDescending { it.priority }

        for (rule in applicableRules) {
            if (matches(ride, rule)) {
                return rule.action.classification ?: RideClassification.GOOD
            }
        }

        return defaultClassification(ride)
    }

    private fun matches(ride: Ride, rule: Rule): Boolean {
        val fieldValue = when (rule.condition.field) {
            RuleField.VALUE_PER_KM   -> ride.valuePerKm
            RuleField.VALUE_PER_HOUR -> ride.valuePerHour
            RuleField.RAW_VALUE      -> ride.rawValue
            RuleField.DISTANCE_KM    -> ride.distanceKm
            RuleField.NET_PROFIT     -> ride.netProfit
        }

        return when (rule.condition.operator) {
            RuleOperator.GREATER_THAN_OR_EQUAL -> fieldValue >= rule.condition.threshold
            RuleOperator.GREATER_THAN          -> fieldValue > rule.condition.threshold
            RuleOperator.LESS_THAN             -> fieldValue < rule.condition.threshold
            RuleOperator.LESS_THAN_OR_EQUAL    -> fieldValue <= rule.condition.threshold
            RuleOperator.EQUAL                 -> fieldValue == rule.condition.threshold
        }
    }

    private fun defaultClassification(ride: Ride): RideClassification = when {
        ride.valuePerKm >= DEFAULT_EXCELLENT_THRESHOLD -> RideClassification.EXCELLENT
        ride.valuePerKm >= DEFAULT_GOOD_THRESHOLD      -> RideClassification.GOOD
        else                                           -> RideClassification.POOR
    }

    companion object {
        const val DEFAULT_EXCELLENT_THRESHOLD = 2.50
        const val DEFAULT_GOOD_THRESHOLD = 2.00
    }
}
