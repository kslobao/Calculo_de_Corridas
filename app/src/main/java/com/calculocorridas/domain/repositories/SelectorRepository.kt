package com.calculocorridas.domain.repositories

import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.domain.entities.SelectorConfig

interface SelectorRepository {
    suspend fun getRemote(): Result<SelectorConfig>
    suspend fun getCached(): SelectorConfig?
    suspend fun saveCached(config: SelectorConfig)
    suspend fun getForApp(appKey: String): AppSelectors?
    suspend fun getCurrentVersion(): Int
    suspend fun rollbackToPreviousVersion(): Boolean
    suspend fun reportParserFailure(appKey: String, patternType: String, selectorVersion: Int)
}
