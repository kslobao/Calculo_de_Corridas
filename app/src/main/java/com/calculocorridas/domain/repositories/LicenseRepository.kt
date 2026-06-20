package com.calculocorridas.domain.repositories

import com.calculocorridas.domain.entities.License

interface LicenseRepository {
    suspend fun checkRemote(purchaseToken: String?): Result<License>
    suspend fun validateSubscription(productId: String, purchaseToken: String): Result<License?>
    suspend fun getCached(): License?
    suspend fun saveCached(license: License)
    suspend fun invalidate()
}
