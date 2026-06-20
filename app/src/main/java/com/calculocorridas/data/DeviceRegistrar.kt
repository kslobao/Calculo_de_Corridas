package com.calculocorridas.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import com.calculocorridas.data.network.ApiService
import com.calculocorridas.data.network.dto.DeviceRegisterRequest
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private val Context.registrationDataStore: DataStore<Preferences>
    by preferencesDataStore("device_registration")

@Singleton
class DeviceRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService
) {
    private val dataStore = context.registrationDataStore
    private val KEY_REGISTERED = booleanPreferencesKey("device_registered")

    suspend fun ensureRegistered() {
        val alreadyRegistered = dataStore.data.first()[KEY_REGISTERED] == true
        if (alreadyRegistered) return

        val token = sha256(androidId + context.packageName)
        val version = appVersion()

        val response = runCatching {
            api.registerDevice(DeviceRegisterRequest(token, context.packageName, version))
        }.getOrNull() ?: return

        if (response.isSuccessful) {
            dataStore.edit { it[KEY_REGISTERED] = true }
        }
    }

    // Invalidate so next call re-registers (e.g. after reinstall or account change)
    suspend fun invalidate() {
        dataStore.edit { it.remove(KEY_REGISTERED) }
    }

    private val androidId: String
        get() = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
            ?: "unknown"

    private fun appVersion(): String = runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "1.0.0"
    }.getOrDefault("1.0.0")

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}
