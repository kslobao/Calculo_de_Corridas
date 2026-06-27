package com.calculocorridas.inspector.models

// ── EventSnapshot ─────────────────────────────────────────────────────────────

data class EventSnapshot(
    // ── Identificação ────────────────────────────────────────────────────────
    val eventType: Int,
    val eventTypeName: String,
    val eventTime: Long,            // SystemClock.uptimeMillis() no momento do evento
    val packageName: String?,
    val className: String?,

    // ── Texto do evento ──────────────────────────────────────────────────────
    val texts: List<String>,        // event.getText()
    val contentDescription: String?,
    val beforeText: String?,        // texto antes da mudança (TYPE_VIEW_TEXT_CHANGED)

    // ── Tipo de mudança (bitmask) ─────────────────────────────────────────────
    val contentChangeTypes: Int,
    val contentChangeTypeNames: List<String>, // nomes legíveis dos bits ativos
    val windowChangeTypes: Int,
    val windowChangeTypeNames: List<String>,  // API 28+

    // ── Action que causou o evento ────────────────────────────────────────────
    val action: Int,
    val actionName: String?,

    // ── Granularidade de movimento ────────────────────────────────────────────
    val movementGranularity: Int,
    val movementGranularityName: String?,

    // ── Informações de lista / paginação ──────────────────────────────────────
    val itemCount: Int,
    val currentItemIndex: Int,
    val fromIndex: Int,
    val toIndex: Int,
    val addedCount: Int,
    val removedCount: Int,

    // ── Scroll ────────────────────────────────────────────────────────────────
    val scrollX: Int,
    val scrollY: Int,
    val maxScrollX: Int,
    val maxScrollY: Int,

    // ── Nó de origem (snapshot parcial — não substitui o dump da janela) ──────
    val sourceClassName: String?,
    val sourceViewId: String?,
    val sourceText: String?,
    val sourceContentDescription: String?
)

// ── WindowSnapshot ────────────────────────────────────────────────────────────

data class WindowSnapshot(
    val windowId: Int,
    val type: Int,
    val typeName: String,
    val packageName: String?,
    val title: String?,            // título da janela, se definido
    val layer: Int,
    val isActive: Boolean,
    val isFocused: Boolean,
    val isAccessibilityFocused: Boolean,
    val isInPictureInPictureMode: Boolean, // API 26+
    val bounds: RectSnapshot?,
    val totalNodes: Int,
    val totalTexts: Int,
    val rootNode: NodeSnapshot?
)

// ── AccessibilityDump ─────────────────────────────────────────────────────────

data class AccessibilityDump(
    val id: String,
    val timestamp: Long,
    val timestampFormatted: String,
    val appSource: String,
    val packageName: String,
    val trigger: String,           // "parse_failed"|"ride_detected"|"manual"|"all_events"|"reverse_engineering"
    val selectorVersion: Int,
    val event: EventSnapshot?,
    val windows: List<WindowSnapshot>,
    val totalNodes: Int,
    val totalTexts: Int,
    val parserResult: String?,
    val bufferedTexts: List<String>,
    val androidSdkInt: Int,
    val inspectorVersion: Int       // versão do schema do dump, para compatibilidade futura
) {
    fun primaryWindow(): WindowSnapshot? =
        windows.firstOrNull { it.packageName == packageName }
}

// ── Metadados para listagem ────────────────────────────────────────────────────

data class DumpMeta(
    val prefix: String,
    val appSource: String,
    val timestamp: Long,
    val timestampFormatted: String,
    val trigger: String,
    val totalNodes: Int,
    val totalTexts: Int,
    val parserResult: String?,
    val sizeBytes: Long
)

// ── Diff ──────────────────────────────────────────────────────────────────────

data class DiffResult(
    val oldId: String,
    val newId: String,
    val addedViewIds: List<String>,
    val removedViewIds: List<String>,
    val textChanges: List<TextChange>,
    val oldNodeCount: Int,
    val newNodeCount: Int,
    val oldTextCount: Int,
    val newTextCount: Int,
    val structureChanged: Boolean
) {
    val hasDifferences: Boolean
        get() = addedViewIds.isNotEmpty() || removedViewIds.isNotEmpty() || textChanges.isNotEmpty()
}

data class TextChange(
    val viewId: String?,
    val className: String?,
    val oldText: String?,
    val newText: String?
)
