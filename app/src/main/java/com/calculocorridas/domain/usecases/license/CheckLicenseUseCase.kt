package com.calculocorridas.domain.usecases.license

import com.calculocorridas.domain.entities.License
import com.calculocorridas.domain.entities.LicenseFeatures
import com.calculocorridas.domain.entities.LicensePlan
import com.calculocorridas.domain.repositories.LicenseRepository
import javax.inject.Inject

class CheckLicenseUseCase @Inject constructor(
    private val repository: LicenseRepository
) {
    suspend operator fun invoke(purchaseToken: String? = null): License {
        val remote = repository.checkRemote(purchaseToken)
        return if (remote.isSuccess) {
            val license = remote.getOrThrow()
            repository.saveCached(license)
            license
        } else {
            repository.getCached() ?: freeLicense()
        }
    }

    private fun freeLicense() = License(
        active = false,
        plan = LicensePlan.FREE,
        expiresAt = 0L,
        features = LicenseFeatures.free()
    )
}
