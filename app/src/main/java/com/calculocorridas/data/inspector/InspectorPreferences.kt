package com.calculocorridas.data.inspector

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.inspectorDataStore: DataStore<Preferences> by preferencesDataStore("inspector_prefs")

@Singleton
class InspectorPreferences @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val dataStore = context.inspectorDataStore

    object Keys {
        // Modo desenvolvedor (oculta seção em release até ativar)
        val DEVELOPER_MODE         = booleanPreferencesKey("developer_mode")

        // Inspector ligado/desligado (master switch)
        val INSPECTOR_ENABLED      = booleanPreferencesKey("inspector_enabled")

        // Quando gerar dumps
        val AUTO_DUMP_ON_FAILURE   = booleanPreferencesKey("auto_dump_on_failure")
        val DUMP_ON_RIDE_DETECTED  = booleanPreferencesKey("dump_on_ride_detected")

        // Dump em TODOS os eventos (por app)
        val DUMP_ALL_EVENTS_UBER    = booleanPreferencesKey("dump_all_events_uber")
        val DUMP_ALL_EVENTS_99      = booleanPreferencesKey("dump_all_events_99")
        val DUMP_ALL_EVENTS_INDRIVE = booleanPreferencesKey("dump_all_events_indrive")
        val DUMP_ALL_EVENTS_IFOOD   = booleanPreferencesKey("dump_all_events_ifood")

        // O que salvar
        val SAVE_FULL_EXTRAS       = booleanPreferencesKey("save_full_extras")
        val SAVE_FULL_TREE         = booleanPreferencesKey("save_full_tree")
        val KEEP_HISTORY           = booleanPreferencesKey("keep_history")

        // Rate limiting
        val RATE_LIMIT_MINUTES     = intPreferencesKey("rate_limit_minutes")  // min entre dumps
        val RATE_LIMIT_MAX_PER_DAY = intPreferencesKey("rate_limit_max_per_day")
    }

    // ── Flows ─────────────────────────────────────────────────────────────────

    val developerMode: Flow<Boolean>          = dataStore.data.map { it[Keys.DEVELOPER_MODE] ?: false }
    val inspectorEnabled: Flow<Boolean>       = dataStore.data.map { it[Keys.INSPECTOR_ENABLED] ?: false }
    val autoDumpOnFailure: Flow<Boolean>      = dataStore.data.map { it[Keys.AUTO_DUMP_ON_FAILURE] ?: true }
    val dumpOnRideDetected: Flow<Boolean>     = dataStore.data.map { it[Keys.DUMP_ON_RIDE_DETECTED] ?: false }
    val dumpAllEventsUber: Flow<Boolean>      = dataStore.data.map { it[Keys.DUMP_ALL_EVENTS_UBER] ?: false }
    val dumpAllEvents99: Flow<Boolean>        = dataStore.data.map { it[Keys.DUMP_ALL_EVENTS_99] ?: false }
    val dumpAllEventsInDrive: Flow<Boolean>   = dataStore.data.map { it[Keys.DUMP_ALL_EVENTS_INDRIVE] ?: false }
    val dumpAllEventsIFood: Flow<Boolean>     = dataStore.data.map { it[Keys.DUMP_ALL_EVENTS_IFOOD] ?: false }
    val saveFullExtras: Flow<Boolean>         = dataStore.data.map { it[Keys.SAVE_FULL_EXTRAS] ?: true }
    val saveFullTree: Flow<Boolean>           = dataStore.data.map { it[Keys.SAVE_FULL_TREE] ?: true }
    val keepHistory: Flow<Boolean>            = dataStore.data.map { it[Keys.KEEP_HISTORY] ?: true }
    val rateLimitMinutes: Flow<Int>           = dataStore.data.map { it[Keys.RATE_LIMIT_MINUTES] ?: 1 }
    val rateLimitMaxPerDay: Flow<Int>         = dataStore.data.map { it[Keys.RATE_LIMIT_MAX_PER_DAY] ?: 50 }

    // ── Setters ───────────────────────────────────────────────────────────────

    suspend fun setDeveloperMode(v: Boolean)        = dataStore.edit { it[Keys.DEVELOPER_MODE] = v }
    suspend fun setInspectorEnabled(v: Boolean)     = dataStore.edit { it[Keys.INSPECTOR_ENABLED] = v }
    suspend fun setAutoDumpOnFailure(v: Boolean)    = dataStore.edit { it[Keys.AUTO_DUMP_ON_FAILURE] = v }
    suspend fun setDumpOnRideDetected(v: Boolean)   = dataStore.edit { it[Keys.DUMP_ON_RIDE_DETECTED] = v }
    suspend fun setDumpAllEventsUber(v: Boolean)    = dataStore.edit { it[Keys.DUMP_ALL_EVENTS_UBER] = v }
    suspend fun setDumpAllEvents99(v: Boolean)      = dataStore.edit { it[Keys.DUMP_ALL_EVENTS_99] = v }
    suspend fun setDumpAllEventsInDrive(v: Boolean) = dataStore.edit { it[Keys.DUMP_ALL_EVENTS_INDRIVE] = v }
    suspend fun setDumpAllEventsIFood(v: Boolean)   = dataStore.edit { it[Keys.DUMP_ALL_EVENTS_IFOOD] = v }
    suspend fun setSaveFullExtras(v: Boolean)       = dataStore.edit { it[Keys.SAVE_FULL_EXTRAS] = v }
    suspend fun setSaveFullTree(v: Boolean)         = dataStore.edit { it[Keys.SAVE_FULL_TREE] = v }
    suspend fun setKeepHistory(v: Boolean)          = dataStore.edit { it[Keys.KEEP_HISTORY] = v }
    suspend fun setRateLimitMinutes(v: Int)         = dataStore.edit { it[Keys.RATE_LIMIT_MINUTES] = v }
    suspend fun setRateLimitMaxPerDay(v: Int)       = dataStore.edit { it[Keys.RATE_LIMIT_MAX_PER_DAY] = v }

    // ── Helpers ───────────────────────────────────────────────────────────────

    suspend fun isEnabled(): Boolean = inspectorEnabled.first()

    suspend fun shouldDump(trigger: String): Boolean {
        if (!inspectorEnabled.first()) return false
        return when (trigger) {
            "parse_failed"         -> autoDumpOnFailure.first()
            "ride_detected"        -> dumpOnRideDetected.first()
            "all_events_uber"      -> dumpAllEventsUber.first()
            "all_events_99"        -> dumpAllEvents99.first()
            "all_events_indrive"   -> dumpAllEventsInDrive.first()
            "all_events_ifood"     -> dumpAllEventsIFood.first()
            "manual", "reverse_engineering" -> true
            else -> false
        }
    }

    suspend fun dumpAllEventsFor(appKey: String): Boolean {
        if (!inspectorEnabled.first()) return false
        return when (appKey) {
            "uber"    -> dumpAllEventsUber.first()
            "99"      -> dumpAllEvents99.first()
            "indrive" -> dumpAllEventsInDrive.first()
            "ifood"   -> dumpAllEventsIFood.first()
            else      -> false
        }
    }
}
