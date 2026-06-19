package com.calculocorridas.domain.repositories

import com.calculocorridas.domain.entities.Rule
import kotlinx.coroutines.flow.Flow

interface RuleRepository {
    fun observeAll(): Flow<List<Rule>>
    suspend fun getAll(): List<Rule>
    suspend fun getEnabled(): List<Rule>
    suspend fun save(rule: Rule): Long
    suspend fun update(rule: Rule)
    suspend fun delete(id: Long)
    suspend fun setEnabled(id: Long, enabled: Boolean)
}
