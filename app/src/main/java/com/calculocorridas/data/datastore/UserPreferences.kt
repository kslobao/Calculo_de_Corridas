package com.calculocorridas.data.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.userDataStore: DataStore<Preferences> by preferencesDataStore("user_prefs")

@Singleton
class UserPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.userDataStore

    object Keys {
        val OVERLAY_TRANSPARENCY  = floatPreferencesKey("overlay_transparency")
        val OVERLAY_AUTO_CLOSE_S  = intPreferencesKey("overlay_auto_close_s")
        val MONITOR_UBER          = booleanPreferencesKey("monitor_uber")
        val MONITOR_99            = booleanPreferencesKey("monitor_99")
        val MONITOR_INDRIVE       = booleanPreferencesKey("monitor_indrive")
        val MONITOR_IFOOD         = booleanPreferencesKey("monitor_ifood")
        val MIN_VALUE_PER_KM      = doublePreferencesKey("min_value_per_km")
        val MIN_VALUE_PER_HOUR    = doublePreferencesKey("min_value_per_hour")
        val ONBOARDING_DONE       = booleanPreferencesKey("onboarding_done")
        val ACTIVE_VEHICLE_ID     = stringPreferencesKey("active_vehicle_id")
        val LAST_INTERSTITIAL_MS  = stringPreferencesKey("last_interstitial_ms")
        val CONSENT_GIVEN         = booleanPreferencesKey("consent_given")
    }

    val overlayTransparency: Flow<Float> = dataStore.data.map { it[Keys.OVERLAY_TRANSPARENCY] ?: 0.95f }
    val overlayAutoCloseSeconds: Flow<Int> = dataStore.data.map { it[Keys.OVERLAY_AUTO_CLOSE_S] ?: 10 }
    val monitorUber: Flow<Boolean> = dataStore.data.map { it[Keys.MONITOR_UBER] ?: true }
    val monitor99: Flow<Boolean> = dataStore.data.map { it[Keys.MONITOR_99] ?: true }
    val monitorInDrive: Flow<Boolean> = dataStore.data.map { it[Keys.MONITOR_INDRIVE] ?: true }
    val monitorIFood: Flow<Boolean> = dataStore.data.map { it[Keys.MONITOR_IFOOD] ?: true }
    val minValuePerKm: Flow<Double> = dataStore.data.map { it[Keys.MIN_VALUE_PER_KM] ?: 2.00 }
    val minValuePerHour: Flow<Double> = dataStore.data.map { it[Keys.MIN_VALUE_PER_HOUR] ?: 30.00 }
    val onboardingDone: Flow<Boolean> = dataStore.data.map { it[Keys.ONBOARDING_DONE] ?: false }
    val consentGiven: Flow<Boolean> = dataStore.data.map { it[Keys.CONSENT_GIVEN] ?: false }

    suspend fun setOverlayTransparency(value: Float) = dataStore.edit { it[Keys.OVERLAY_TRANSPARENCY] = value }
    suspend fun setOverlayAutoClose(seconds: Int) = dataStore.edit { it[Keys.OVERLAY_AUTO_CLOSE_S] = seconds }
    suspend fun setMonitorUber(enabled: Boolean) = dataStore.edit { it[Keys.MONITOR_UBER] = enabled }
    suspend fun setMonitor99(enabled: Boolean) = dataStore.edit { it[Keys.MONITOR_99] = enabled }
    suspend fun setMonitorInDrive(enabled: Boolean) = dataStore.edit { it[Keys.MONITOR_INDRIVE] = enabled }
    suspend fun setMonitorIFood(enabled: Boolean) = dataStore.edit { it[Keys.MONITOR_IFOOD] = enabled }
    suspend fun setMinValuePerKm(value: Double) = dataStore.edit { it[Keys.MIN_VALUE_PER_KM] = value }
    suspend fun setMinValuePerHour(value: Double) = dataStore.edit { it[Keys.MIN_VALUE_PER_HOUR] = value }
    suspend fun setOnboardingDone() = dataStore.edit { it[Keys.ONBOARDING_DONE] = true }
    suspend fun setConsentGiven(given: Boolean) = dataStore.edit { it[Keys.CONSENT_GIVEN] = given }
    suspend fun setLastInterstitialMs(ms: Long) = dataStore.edit { it[Keys.LAST_INTERSTITIAL_MS] = ms.toString() }
    suspend fun getLastInterstitialMs(): Long =
        dataStore.data.map { it[Keys.LAST_INTERSTITIAL_MS]?.toLongOrNull() ?: 0L }.first()
}
