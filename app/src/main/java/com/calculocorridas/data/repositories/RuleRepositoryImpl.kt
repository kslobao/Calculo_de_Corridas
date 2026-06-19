package com.calculocorridas.data.repositories

import com.calculocorridas.data.database.dao.RuleDao
import com.calculocorridas.data.database.entities.RuleEntity
import com.calculocorridas.domain.entities.Rule
import com.calculocorridas.domain.repositories.RuleRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RuleRepositoryImpl @Inject constructor(
    private val dao: RuleDao
) : RuleRepository {

    override fun observeAll(): Flow<List<Rule>> =
        dao.observeAll().map { list -> list.map { it.toDomain() } }

    override suspend fun getAll(): List<Rule> =
        dao.getAll().map { it.toDomain() }

    override suspend fun getEnabled(): List<Rule> =
        dao.getEnabled().map { it.toDomain() }

    override suspend fun save(rule: Rule): Long =
        dao.insert(RuleEntity.fromDomain(rule))

    override suspend fun update(rule: Rule) =
        dao.update(RuleEntity.fromDomain(rule))

    override suspend fun delete(id: Long) =
        dao.deleteById(id)

    override suspend fun setEnabled(id: Long, enabled: Boolean) =
        dao.setEnabled(id, if (enabled) 1 else 0)
}
