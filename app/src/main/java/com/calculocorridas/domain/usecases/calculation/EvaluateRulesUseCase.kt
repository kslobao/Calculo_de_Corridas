package com.calculocorridas.domain.usecases.calculation

import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.domain.engine.RuleEngine
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.entities.Rule
import javax.inject.Inject

class EvaluateRulesUseCase @Inject constructor(
    private val ruleEngine: RuleEngine
) {
    operator fun invoke(ride: Ride, rules: List<Rule>): RideClassification =
        ruleEngine.evaluate(ride, rules)
}
