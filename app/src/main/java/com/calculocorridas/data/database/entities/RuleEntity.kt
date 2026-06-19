package com.calculocorridas.data.database.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.entities.Rule
import com.calculocorridas.domain.entities.RuleAction
import com.calculocorridas.domain.entities.RuleCondition
import com.calculocorridas.domain.entities.RuleField
import com.calculocorridas.domain.entities.RuleOperator

@Entity(tableName = "rules")
data class RuleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    @ColumnInfo(name = "condition_field") val conditionField: String,
    @ColumnInfo(name = "condition_operator") val conditionOperator: String,
    @ColumnInfo(name = "condition_threshold") val conditionThreshold: Double,
    val action: String,
    val enabled: Int = 1,
    val priority: Int = 0,
    @ColumnInfo(name = "app_filter") val appFilter: String? = null
) {
    fun toDomain(): Rule = Rule(
        id = id,
        name = name,
        condition = RuleCondition(
            field = RuleField.valueOf(conditionField),
            operator = RuleOperator.valueOf(conditionOperator),
            threshold = conditionThreshold
        ),
        action = RuleAction.valueOf(action),
        enabled = enabled == 1,
        priority = priority,
        appFilter = appFilter?.split(",")?.mapNotNull { AppSource.fromKey(it.trim()) }?.toSet()
    )

    companion object {
        fun fromDomain(rule: Rule) = RuleEntity(
            id = rule.id,
            name = rule.name,
            conditionField = rule.condition.field.name,
            conditionOperator = rule.condition.operator.name,
            conditionThreshold = rule.condition.threshold,
            action = rule.action.name,
            enabled = if (rule.enabled) 1 else 0,
            priority = rule.priority,
            appFilter = rule.appFilter?.joinToString(",") { it.key }
        )
    }
}
