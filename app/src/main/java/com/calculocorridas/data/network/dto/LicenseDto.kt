package com.calculocorridas.data.network.dto

import com.calculocorridas.domain.entities.License
import com.calculocorridas.domain.entities.LicenseFeatures
import com.calculocorridas.domain.entities.LicensePlan
import com.google.gson.annotations.SerializedName
import java.text.SimpleDateFormat
import java.util.Locale

// ─── License check ───────────────────────────────────────────────────────────

data class LicenseCheckRequest(
    @SerializedName("device_id")      val deviceId: String,
    @SerializedName("package_name")   val packageName: String,
    @SerializedName("purchase_token") val purchaseToken: String?
)

data class LicenseResponseDto(
    @SerializedName("active")     val active: Boolean,
    @SerializedName("plan")       val plan: String,
    @SerializedName("expires_at") val expiresAt: String?,
    @SerializedName("features")   val features: LicenseFeaturesDto?,
    @SerializedName("reason")     val reason: String? = null,
    @SerializedName("message")    val message: String? = null
) {
    fun toDomain(): License {
        val expiresEpoch = expiresAt?.let {
            runCatching {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)?.time ?: 0L
            }.getOrDefault(0L)
        } ?: 0L

        return License(
            active    = active,
            plan      = if (plan.equals("pro", ignoreCase = true)) LicensePlan.PRO else LicensePlan.FREE,
            expiresAt = expiresEpoch,
            features  = features?.toDomain() ?: LicenseFeatures.free()
        )
    }
}

data class LicenseFeaturesDto(
    @SerializedName("ads_free")          val adsFree: Boolean = false,
    @SerializedName("unlimited_history") val unlimitedHistory: Boolean = false,
    @SerializedName("cloud_backup")      val cloudBackup: Boolean = false,
    @SerializedName("export")            val export: Boolean = false,
    @SerializedName("multi_vehicle")     val multiVehicle: Boolean = false
) {
    fun toDomain() = LicenseFeatures(
        adsFree          = adsFree,
        unlimitedHistory = unlimitedHistory,
        cloudBackup      = cloudBackup,
        export           = export,
        multiVehicle     = multiVehicle
    )
}

// ─── Subscription validate (restore purchase / new purchase) ─────────────────

data class SubscriptionValidateRequest(
    @SerializedName("product_id")     val productId: String,
    @SerializedName("purchase_token") val purchaseToken: String
)

data class SubscriptionValidateResponse(
    @SerializedName("valid")      val valid: Boolean,
    @SerializedName("plan")       val plan: String? = null,
    @SerializedName("source")     val source: String? = null,
    @SerializedName("expires_at") val expiresAt: String? = null,
    @SerializedName("features")   val features: LicenseFeaturesDto? = null,
    @SerializedName("reason")     val reason: String? = null
) {
    fun toDomain(): License? {
        if (!valid || plan == null) return null
        val expiresEpoch = expiresAt?.let {
            runCatching {
                SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).parse(it)?.time ?: 0L
            }.getOrDefault(0L)
        } ?: 0L
        return License(
            active    = valid,
            plan      = if (plan.equals("pro", ignoreCase = true)) LicensePlan.PRO else LicensePlan.FREE,
            expiresAt = expiresEpoch,
            features  = features?.toDomain() ?: LicenseFeatures.free()
        )
    }
}

// ─── Parser report ───────────────────────────────────────────────────────────

data class ParserReportRequest(
    @SerializedName("app_key")          val appKey: String,
    @SerializedName("selector_version") val selectorVersion: Int,
    @SerializedName("success")          val success: Boolean,
    @SerializedName("raw_texts")        val rawTexts: List<String>? = null,
    @SerializedName("error_message")    val errorMessage: String? = null,
    @SerializedName("app_version")      val appVersion: String? = null
)
