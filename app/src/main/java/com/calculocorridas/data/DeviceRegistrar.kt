package com.calculocorridas.data

import android.content.Context
import android.provider.Settings
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.calculocorridas.data.network.ApiService
import com.calculocorridas.data.network.dto.DeviceRegisterRequest
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "CalcCorridas"

private val Context.registrationDataStore: DataStore<Preferences>
    by preferencesDataStore("device_registration")

@Singleton
class DeviceRegistrar @Inject constructor(
    @ApplicationContext private val context: Context,
    private val api: ApiService
) {
    private val dataStore = context.registrationDataStore
    private val KEY_REGISTERED = booleanPreferencesKey("device_registered")
    private val KEY_DEVICE_ID  = stringPreferencesKey("device_id_uuid")

    suspend fun ensureRegistered() {
        val alreadyRegistered = dataStore.data.first()[KEY_REGISTERED] == true
        if (alreadyRegistered) {
            Log.d(TAG, "DeviceRegistrar: dispositivo já registrado anteriormente")
            return
        }

        val token = sha256(androidId + context.packageName)
        val version = appVersion()
        Log.d(TAG, "DeviceRegistrar: enviando registro — pkg=${context.packageName} version=$version token=${token.take(8)}...")

        val response = runCatching {
            api.registerDevice(DeviceRegisterRequest(token, context.packageName, version))
        }.onFailure {
            Log.e(TAG, "DeviceRegistrar: erro de rede ao registrar", it)
        }.getOrNull() ?: return

        Log.d(TAG, "DeviceRegistrar: resposta HTTP ${response.code()} — body=${response.body()}")

        if (response.isSuccessful) {
            val uuid = response.body()?.deviceId
            dataStore.edit {
                it[KEY_REGISTERED] = true
                if (uuid != null) it[KEY_DEVICE_ID] = uuid
            }
            Log.i(TAG, "DeviceRegistrar: dispositivo registrado com sucesso ($uuid)")
        } else {
            Log.w(TAG, "DeviceRegistrar: falha HTTP ${response.code()} — ${response.errorBody()?.string()}")
        }
    }

    /** UUID retornado pelo backend no registro, ou null se dispositivo antigo sem UUID salvo. */
    suspend fun getDeviceId(): String? = dataStore.data.first()[KEY_DEVICE_ID]

    /** SHA-256(androidId + packageName) — mesmo valor do Bearer token. */
    fun getDeviceToken(): String = sha256(androidId + context.packageName)

    // Invalidate so next call re-registers (e.g. after reinstall or account change)
    suspend fun invalidate() {
        dataStore.edit {
            it.remove(KEY_REGISTERED)
            it.remove(KEY_DEVICE_ID)
        }
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
