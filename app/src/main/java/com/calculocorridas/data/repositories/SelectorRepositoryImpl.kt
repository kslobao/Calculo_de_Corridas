package com.calculocorridas.data.repositories

import com.calculocorridas.data.database.dao.SelectorCacheDao
import com.calculocorridas.data.database.entities.SelectorCacheEntity
import com.calculocorridas.data.network.ApiService
import com.calculocorridas.data.network.dto.ParserFailureRequest
import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.domain.entities.SelectorConfig
import com.calculocorridas.domain.entities.SelectorPattern
import com.calculocorridas.domain.entities.SelectorType
import com.calculocorridas.domain.repositories.SelectorRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SelectorRepositoryImpl @Inject constructor(
    private val api: ApiService,
    private val dao: SelectorCacheDao
) : SelectorRepository {

    override suspend fun getRemote(): Result<SelectorConfig> = runCatching {
        val currentVersion = dao.getCurrentVersion() ?: 0
        val response = api.getSelectors(currentVersion)
        when {
            response.code() == 304 -> getCached() ?: error("No cached selectors")
            response.isSuccessful  -> response.body()?.toDomain() ?: error("Empty response")
            else                   -> error("HTTP ${response.code()}")
        }
    }

    override suspend fun getCached(): SelectorConfig? {
        val version = dao.getCurrentVersion() ?: return null
        val appKeys = listOf("uber", "99", "indrive", "ifood")
        val appSelectors = appKeys.associateWith { key -> buildAppSelectors(key) }
        return SelectorConfig(version = version, updatedAt = "", apps = appSelectors)
    }

    override suspend fun saveCached(config: SelectorConfig) {
        val entities = mutableListOf<SelectorCacheEntity>()
        config.apps.forEach { (appKey, selectors) ->
            entities.addAll(toEntities(config.version, appKey, "price", selectors.pricePatterns))
            entities.addAll(toEntities(config.version, appKey, "distance", selectors.distancePatterns))
            entities.addAll(toEntities(config.version, appKey, "time", selectors.timePatterns))
            entities.addAll(toEntities(config.version, appKey, "origin", selectors.originPatterns))
            entities.addAll(toEntities(config.version, appKey, "destination", selectors.destinationPatterns))
            entities.addAll(toEntities(config.version, appKey, "category", selectors.categoryPatterns))
        }
        dao.insertAll(entities)
        dao.deactivateAllExcept(config.version)
        dao.deleteOldVersions(config.version)
    }

    override suspend fun getForApp(appKey: String): AppSelectors? {
        if (dao.getCurrentVersion() == null) return null
        return buildAppSelectors(appKey)
    }

    override suspend fun getCurrentVersion(): Int = dao.getCurrentVersion() ?: 0

    override suspend fun rollbackToPreviousVersion(): Boolean {
        val prev = dao.getPreviousVersion() ?: return false
        dao.deleteCurrentVersion()
        dao.deactivateAllExcept(prev)
        return true
    }

    override suspend fun reportParserFailure(appKey: String, patternType: String, selectorVersion: Int) {
        runCatching {
            api.reportParserFailure(
                ParserFailureRequest(
                    appKey = appKey,
                    selectorVersion = selectorVersion,
                    failedPattern = patternType,
                    eventType = "TYPE_WINDOW_CONTENT_CHANGED",
                    timestamp = System.currentTimeMillis()
                )
            )
        }
    }

    private suspend fun buildAppSelectors(appKey: String) = AppSelectors(
        pricePatterns       = patternsFor(appKey, "price"),
        distancePatterns    = patternsFor(appKey, "distance"),
        timePatterns        = patternsFor(appKey, "time"),
        originPatterns      = patternsFor(appKey, "origin"),
        destinationPatterns = patternsFor(appKey, "destination"),
        categoryPatterns    = patternsFor(appKey, "category")
    )

    private suspend fun patternsFor(appKey: String, type: String): List<SelectorPattern> =
        dao.getByAppAndType(appKey, type).map {
            SelectorPattern(SelectorType.fromKey(it.selectorType), it.pattern, it.priority)
        }

    private fun toEntities(
        version: Int, appKey: String, patternType: String, patterns: List<SelectorPattern>
    ): List<SelectorCacheEntity> = patterns.map { p ->
        SelectorCacheEntity(
            version = version,
            appKey = appKey,
            patternType = patternType,
            pattern = p.value,
            selectorType = p.type.key,
            priority = p.priority
        )
    }
}
