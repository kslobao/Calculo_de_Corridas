package com.calculocorridas.data.repositories

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calculocorridas.data.DeviceRegistrar
import com.calculocorridas.data.network.ApiService
import com.calculocorridas.data.network.dto.LicenseCheckRequest
import com.calculocorridas.data.network.dto.SubscriptionValidateRequest
import com.calculocorridas.domain.entities.License
import com.calculocorridas.domain.entities.LicenseFeatures
import com.calculocorridas.domain.entities.LicensePlan
import com.calculocorridas.domain.repositories.LicenseRepository
import com.google.gson.Gson
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import javax.inject.Inject
import javax.inject.Singleton

private val Context.licenseDataStore: DataStore<Preferences> by preferencesDataStore("license_prefs")

@Singleton
class LicenseRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService,
    private val deviceRegistrar: DeviceRegistrar,
    private val gson: Gson
) : LicenseRepository {

    private val dataStore = context.licenseDataStore
    private val KEY_LICENSE_JSON = stringPreferencesKey("license_json")
    private val KEY_CACHED_AT    = longPreferencesKey("license_cached_at")

    override suspend fun checkRemote(purchaseToken: String?): Result<License> = runCatching {
        deviceRegistrar.ensureRegistered()
        val response = api.checkLicense(LicenseCheckRequest(purchaseToken = purchaseToken))
        if (response.isSuccessful) {
            response.body()?.toDomain() ?: error("Empty license response")
        } else {
            error("HTTP ${response.code()}")
        }
    }

    override suspend fun validateSubscription(
        productId: String,
        purchaseToken: String
    ): Result<License?> = runCatching {
        deviceRegistrar.ensureRegistered()
        val response = api.validateSubscription(
            SubscriptionValidateRequest(productId = productId, purchaseToken = purchaseToken)
        )
        when {
            response.isSuccessful  -> response.body()?.toDomain()
            response.code() == 422 -> null
            else                   -> error("HTTP ${response.code()}")
        }
    }

    override suspend fun getCached(): License? {
        val prefs = dataStore.data.first()
        val json = prefs[KEY_LICENSE_JSON] ?: return null
        return runCatching { gson.fromJson(json, LicenseJson::class.java).toDomain() }.getOrNull()
    }

    override suspend fun saveCached(license: License) {
        val json = gson.toJson(LicenseJson.fromDomain(license))
        dataStore.edit { prefs ->
            prefs[KEY_LICENSE_JSON] = json
            prefs[KEY_CACHED_AT]    = System.currentTimeMillis()
        }
    }

    override suspend fun invalidate() {
        dataStore.edit { it.remove(KEY_LICENSE_JSON) }
    }

    private data class LicenseJson(
        val active: Boolean,
        val plan: String,
        val expiresAt: Long,
        val adsFree: Boolean,
        val unlimitedHistory: Boolean,
        val cloudBackup: Boolean,
        val export: Boolean,
        val multiVehicle: Boolean
    ) {
        fun toDomain() = License(
            active    = active,
            plan      = if (plan == "PRO") LicensePlan.PRO else LicensePlan.FREE,
            expiresAt = expiresAt,
            features  = LicenseFeatures(adsFree, unlimitedHistory, cloudBackup, export, multiVehicle)
        )

        companion object {
            fun fromDomain(l: License) = LicenseJson(
                active           = l.active,
                plan             = l.plan.name,
                expiresAt        = l.expiresAt,
                adsFree          = l.features.adsFree,
                unlimitedHistory = l.features.unlimitedHistory,
                cloudBackup      = l.features.cloudBackup,
                export           = l.features.export,
                multiVehicle     = l.features.multiVehicle
            )
        }
    }
}
