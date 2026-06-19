package com.calculocorridas.domain.entities

data class License(
    val active: Boolean,
    val plan: LicensePlan,
    val expiresAt: Long,
    val features: LicenseFeatures,
    val cachedAt: Long = System.currentTimeMillis()
) {
    val isExpired: Boolean
        get() = expiresAt > 0 && System.currentTimeMillis() > expiresAt

    val isValid: Boolean
        get() = active && !isExpired
}

enum class LicensePlan { FREE, PRO }

data class LicenseFeatures(
    val adsFree: Boolean = false,
    val unlimitedHistory: Boolean = false,
    val cloudBackup: Boolean = false,
    val export: Boolean = false,
    val multiVehicle: Boolean = false
) {
    companion object {
        fun free() = LicenseFeatures()
        fun pro() = LicenseFeatures(
            adsFree = true,
            unlimitedHistory = true,
            cloudBackup = true,
            export = true,
            multiVehicle = true
        )
    }
}
