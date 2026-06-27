package com.calculocorridas.presentation.viewmodels

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.calculocorridas.data.inspector.InspectorPreferences
import com.calculocorridas.inspector.AccessibilityInspector
import com.calculocorridas.inspector.models.AccessibilityDump
import com.calculocorridas.inspector.models.DiffResult
import com.calculocorridas.inspector.models.DumpMeta
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

data class InspectorSettingsState(
    val developerMode: Boolean       = false,
    val inspectorEnabled: Boolean    = false,
    val autoDumpOnFailure: Boolean   = true,
    val dumpOnRideDetected: Boolean  = false,
    val dumpAllEventsUber: Boolean   = false,
    val dumpAllEvents99: Boolean     = false,
    val dumpAllEventsInDrive: Boolean = false,
    val dumpAllEventsIFood: Boolean  = false,
    val saveFullExtras: Boolean      = false,
    val saveFullTree: Boolean        = true,
    val keepHistory: Boolean         = true,
    val rateLimitMinutes: Int        = 1,
    val rateLimitMaxPerDay: Int      = 50
)

data class InspectorUiState(
    val dumps: List<DumpMeta>          = emptyList(),
    val selectedDump: AccessibilityDump? = null,
    val selectedTxt: String?           = null,
    val compareDump: AccessibilityDump? = null,
    val diffResult: DiffResult?        = null,
    val isLoading: Boolean             = false,
    val exportMessage: String?         = null,
    val isReverseEngineering: Boolean  = false,
    val reverseEngineeringSeconds: Int = 0
)

@HiltViewModel
class InspectorViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val inspector: AccessibilityInspector,
    private val prefs: InspectorPreferences
) : ViewModel() {

    private val _settingsState = MutableStateFlow(InspectorSettingsState())
    val settingsState: StateFlow<InspectorSettingsState> = _settingsState.asStateFlow()

    private val _uiState = MutableStateFlow(InspectorUiState())
    val uiState: StateFlow<InspectorUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                prefs.developerMode,
                prefs.inspectorEnabled,
                prefs.autoDumpOnFailure,
                prefs.dumpOnRideDetected,
                prefs.saveFullExtras
            ) { arr -> arr }.collectLatest { arr ->
                _settingsState.value = _settingsState.value.copy(
                    developerMode      = arr[0] as Boolean,
                    inspectorEnabled   = arr[1] as Boolean,
                    autoDumpOnFailure  = arr[2] as Boolean,
                    dumpOnRideDetected = arr[3] as Boolean,
                    saveFullExtras     = arr[4] as Boolean
                )
            }
        }
        viewModelScope.launch {
            combine(prefs.saveFullTree, prefs.keepHistory) { tree, hist -> Pair(tree, hist) }
                .collectLatest { (tree, hist) ->
                    _settingsState.value = _settingsState.value.copy(
                        saveFullTree = tree,
                        keepHistory  = hist
                    )
                }
        }
        viewModelScope.launch {
            combine(
                prefs.dumpAllEventsUber,
                prefs.dumpAllEvents99,
                prefs.dumpAllEventsInDrive,
                prefs.dumpAllEventsIFood
            ) { arr -> arr }.collectLatest { arr ->
                _settingsState.value = _settingsState.value.copy(
                    dumpAllEventsUber    = arr[0] as Boolean,
                    dumpAllEvents99      = arr[1] as Boolean,
                    dumpAllEventsInDrive = arr[2] as Boolean,
                    dumpAllEventsIFood   = arr[3] as Boolean
                )
            }
        }
        viewModelScope.launch {
            combine(prefs.rateLimitMinutes, prefs.rateLimitMaxPerDay) { min, max -> Pair(min, max) }
                .collectLatest { (min, max) ->
                    _settingsState.value = _settingsState.value.copy(
                        rateLimitMinutes   = min,
                        rateLimitMaxPerDay = max
                    )
                }
        }
    }

    // ── Settings ──────────────────────────────────────────────────────────────

    fun setDeveloperMode(v: Boolean)       = viewModelScope.launch { prefs.setDeveloperMode(v) }
    fun setInspectorEnabled(v: Boolean)    = viewModelScope.launch { prefs.setInspectorEnabled(v) }
    fun setAutoDumpOnFailure(v: Boolean)   = viewModelScope.launch { prefs.setAutoDumpOnFailure(v) }
    fun setDumpOnRideDetected(v: Boolean)  = viewModelScope.launch { prefs.setDumpOnRideDetected(v) }
    fun setDumpAllEventsUber(v: Boolean)   = viewModelScope.launch { prefs.setDumpAllEventsUber(v) }
    fun setDumpAllEvents99(v: Boolean)     = viewModelScope.launch { prefs.setDumpAllEvents99(v) }
    fun setDumpAllEventsInDrive(v: Boolean) = viewModelScope.launch { prefs.setDumpAllEventsInDrive(v) }
    fun setDumpAllEventsIFood(v: Boolean)  = viewModelScope.launch { prefs.setDumpAllEventsIFood(v) }
    fun setSaveFullExtras(v: Boolean)      = viewModelScope.launch { prefs.setSaveFullExtras(v) }
    fun setSaveFullTree(v: Boolean)        = viewModelScope.launch { prefs.setSaveFullTree(v) }
    fun setKeepHistory(v: Boolean)         = viewModelScope.launch { prefs.setKeepHistory(v) }
    fun setRateLimitMinutes(v: Int)        = viewModelScope.launch { prefs.setRateLimitMinutes(v.coerceIn(0, 60)) }
    fun setRateLimitMaxPerDay(v: Int)      = viewModelScope.launch { prefs.setRateLimitMaxPerDay(v.coerceIn(1, 500)) }

    // ── Dump list ─────────────────────────────────────────────────────────────

    fun refreshDumps() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        viewModelScope.launch {
            val dumps = inspector.listDumps()
            _uiState.value = _uiState.value.copy(dumps = dumps, isLoading = false)
        }
    }

    fun selectDump(prefix: String) {
        viewModelScope.launch {
            val dump = inspector.readDump(prefix)
            val txt  = inspector.readTxt(prefix)
            _uiState.value = _uiState.value.copy(
                selectedDump  = dump,
                selectedTxt   = txt,
                compareDump   = null,
                diffResult    = null
            )
        }
    }

    fun selectCompare(prefix: String) {
        viewModelScope.launch {
            val compare = inspector.readDump(prefix) ?: return@launch
            val selected = _uiState.value.selectedDump ?: return@launch
            val diff = inspector.diff(selected, compare)
            _uiState.value = _uiState.value.copy(
                compareDump = compare,
                diffResult  = diff
            )
        }
    }

    fun clearSelection() {
        _uiState.value = _uiState.value.copy(
            selectedDump = null,
            selectedTxt  = null,
            compareDump  = null,
            diffResult   = null
        )
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun deleteDump(prefix: String) {
        inspector.delete(prefix)
        refreshDumps()
    }

    fun deleteAll() {
        inspector.deleteAll()
        refreshDumps()
    }

    // ── Export ────────────────────────────────────────────────────────────────

    fun shareLastDump() {
        viewModelScope.launch {
            val last = inspector.listDumps().firstOrNull() ?: run {
                _uiState.value = _uiState.value.copy(exportMessage = "Nenhum dump disponível")
                return@launch
            }
            shareFile(last.prefix)
        }
    }

    fun shareDump(prefix: String) = viewModelScope.launch { shareFile(prefix) }

    private fun shareFile(prefix: String) {
        val selected = _uiState.value.selectedDump
        val dump = if (selected?.id == prefix) selected else inspector.readDump(prefix) ?: return
        // Share txt version (more readable)
        val txt = inspector.readTxt(prefix) ?: return
        val file = java.io.File(context.cacheDir, "$prefix.txt").also { it.writeText(txt) }
        val uri  = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Accessibility Dump — ${dump.appSource} ${dump.timestampFormatted}")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(intent, "Compartilhar dump").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        })
    }

    fun exportAllZip() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            runCatching {
                val zipFile = inspector.exportZip()
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", zipFile)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "application/zip"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "Accessibility Inspector Export")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(intent, "Exportar ZIP").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
            }.onFailure {
                _uiState.value = _uiState.value.copy(exportMessage = "Erro ao exportar: ${it.message}")
            }
            _uiState.value = _uiState.value.copy(isLoading = false)
        }
    }

    fun clearExportMessage() {
        _uiState.value = _uiState.value.copy(exportMessage = null)
    }

    // ── Reverse Engineering mode ──────────────────────────────────────────────

    fun startReverseEngineering(packageName: String) {
        _uiState.value = _uiState.value.copy(isReverseEngineering = true, reverseEngineeringSeconds = 300)
        viewModelScope.launch {
            var remaining = 300
            while (remaining > 0) {
                kotlinx.coroutines.delay(1000)
                remaining--
                _uiState.value = _uiState.value.copy(reverseEngineeringSeconds = remaining)
            }
            _uiState.value = _uiState.value.copy(isReverseEngineering = false)
            refreshDumps()
        }
    }

    fun stopReverseEngineering() {
        _uiState.value = _uiState.value.copy(isReverseEngineering = false)
        refreshDumps()
    }
}
