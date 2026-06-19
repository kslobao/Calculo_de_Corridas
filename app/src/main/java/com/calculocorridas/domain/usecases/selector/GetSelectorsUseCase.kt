package com.calculocorridas.domain.usecases.selector

import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.domain.entities.SelectorConfig
import com.calculocorridas.domain.repositories.SelectorRepository
import javax.inject.Inject

class GetSelectorsUseCase @Inject constructor(
    private val repository: SelectorRepository
) {
    suspend fun config(): SelectorConfig? = repository.getCached()

    suspend fun forApp(appKey: String): AppSelectors? = repository.getForApp(appKey)
}
