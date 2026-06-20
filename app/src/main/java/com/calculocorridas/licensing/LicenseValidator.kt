package com.calculocorridas.licensing

import com.calculocorridas.domain.entities.License
import com.calculocorridas.domain.entities.LicenseFeatures
import com.calculocorridas.domain.entities.LicensePlan
import com.calculocorridas.domain.repositories.LicenseRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LicenseValidator @Inject constructor(
    private val licenseRepository: LicenseRepository
) {
    private val _license = MutableStateFlow(freeLicense())
    val license: StateFlow<License> = _license.asStateFlow()

    val isPro: Boolean get() = _license.value.isValid && _license.value.plan == LicensePlan.PRO
    val features: LicenseFeatures get() = _license.value.features

    suspend fun refresh(purchaseToken: String? = null) {
        val result = licenseRepository.checkRemote(purchaseToken)
        if (result.isSuccess) {
            val l = result.getOrThrow()
            licenseRepository.saveCached(l)
            _license.value = l
        } else {
            licenseRepository.getCached()?.let { _license.value = it }
        }
    }

    // Called after a successful Google Play purchase or to restore purchases.
    // Uses POST /api/v1/subscription/validate which validates directly with Google Play API.
    suspend fun validateAndActivate(productId: String, purchaseToken: String): Boolean {
        val result = licenseRepository.validateSubscription(productId, purchaseToken)
        if (result.isSuccess) {
            val license = result.getOrNull()
            if (license != null) {
                licenseRepository.saveCached(license)
                _license.value = license
                return license.isPro()
            }
        }
        return false
    }

    suspend fun loadCached() {
        licenseRepository.getCached()?.let { _license.value = it }
    }

    private fun License.isPro() = isValid && plan == LicensePlan.PRO

    private fun freeLicense() = License(
        active    = false,
        plan      = LicensePlan.FREE,
        expiresAt = 0L,
        features  = LicenseFeatures.free()
    )
}
