package com.calculocorridas.domain.usecases.selector

import com.calculocorridas.domain.repositories.SelectorRepository
import javax.inject.Inject

class SyncSelectorsUseCase @Inject constructor(
    private val repository: SelectorRepository
) {
    suspend operator fun invoke(): Result<Unit> {
        return repository.getRemote().map { config ->
            repository.saveCached(config)
        }
    }
}
