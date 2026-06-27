package com.calculocorridas.inspector.models

data class RectSnapshot(val left: Int, val top: Int, val right: Int, val bottom: Int) {
    override fun toString() = "$left,$top,$right,$bottom (${right - left}x${bottom - top})"
}

data class ActionSnapshot(
    val id: Int,
    val standardName: String,   // ex: "CLICK", "SCROLL_FORWARD"
    val label: String?          // label personalizado do app, se houver
)

data class RangeInfoSnapshot(
    val type: Int,      // 0=int, 1=float, 2=percent
    val typeName: String,
    val min: Float,
    val max: Float,
    val current: Float
)

data class CollectionInfoSnapshot(
    val rowCount: Int,
    val columnCount: Int,
    val isHierarchical: Boolean,
    val selectionMode: Int
)

data class CollectionItemInfoSnapshot(
    val rowIndex: Int,
    val rowSpan: Int,
    val columnIndex: Int,
    val columnSpan: Int,
    val isHeading: Boolean,
    val isSelected: Boolean
)

data class NodeSnapshot(
    // ── Posição na árvore ────────────────────────────────────────────────────
    val depth: Int,
    val siblingIndex: Int,         // índice entre irmãos

    // ── Identidade ──────────────────────────────────────────────────────────
    val className: String?,
    val packageName: String?,
    val viewIdFull: String?,       // viewIdResourceName completo, ex: "com.ubercab.driver:id/fare"
    val viewId: String?,           // só a parte depois de '/'

    // ── Texto / conteúdo ─────────────────────────────────────────────────────
    val text: String?,
    val contentDescription: String?,
    val hintText: String?,         // API 26+
    val paneTitle: String?,        // API 28+
    val stateDescription: String?, // API 30+
    val tooltipText: String?,      // API 28+
    val error: String?,            // texto de erro (ex: campo inválido)
    val uniqueId: String?,         // API 33+

    // ── Seleção de texto ─────────────────────────────────────────────────────
    val textSelectionStart: Int,
    val textSelectionEnd: Int,
    val inputType: Int,            // android.text.InputType bitmask

    // ── toString() do nó — CRÍTICO para apps React Native / Compose ─────────
    val nodeToString: String?,

    // ── Geometria ────────────────────────────────────────────────────────────
    val boundsInScreen: RectSnapshot?,
    val boundsInParent: RectSnapshot?,
    val drawingOrder: Int,         // API 24+ — z-order de desenho

    // ── Live region ──────────────────────────────────────────────────────────
    val liveRegion: Int,           // 0=none, 1=polite, 2=assertive

    // ── Flags booleanos ──────────────────────────────────────────────────────
    val visibleToUser: Boolean,
    val enabled: Boolean,
    val clickable: Boolean,
    val longClickable: Boolean,
    val contextClickable: Boolean, // API 23+
    val focusable: Boolean,
    val focused: Boolean,
    val accessibilityFocused: Boolean,
    val checkable: Boolean,
    val checked: Boolean,
    val scrollable: Boolean,
    val editable: Boolean,
    val selected: Boolean,
    val password: Boolean,
    val dismissable: Boolean,
    val multiLine: Boolean,
    val importantForAccessibility: Boolean, // API 24+
    val screenReaderFocusable: Boolean,     // API 28+

    // ── Estrutura ────────────────────────────────────────────────────────────
    val childCount: Int,
    val labelForViewId: String?,   // este nó é label para outro
    val labeledByViewId: String?,  // este nó é labelado por outro

    // ── Collections (listas, grades) ─────────────────────────────────────────
    val collectionInfo: CollectionInfoSnapshot?,
    val collectionItemInfo: CollectionItemInfoSnapshot?,

    // ── Range (sliders, progress bars) ───────────────────────────────────────
    val rangeInfo: RangeInfoSnapshot?,

    // ── Actions ──────────────────────────────────────────────────────────────
    val actions: List<ActionSnapshot>,

    // ── Extras (Bundle) — tudo que o app colocou ─────────────────────────────
    val extras: Map<String, String>,

    // ── Filhos ───────────────────────────────────────────────────────────────
    val children: List<NodeSnapshot>
) {
    val hasText: Boolean
        get() = !text.isNullOrBlank() || !contentDescription.isNullOrBlank() ||
            !hintText.isNullOrBlank() || !stateDescription.isNullOrBlank() ||
            !error.isNullOrBlank()

    fun allTexts(): List<String> = buildList {
        text?.takeIf { it.isNotBlank() }?.let { add("text=$it") }
        contentDescription?.takeIf { it.isNotBlank() }?.let { add("desc=$it") }
        hintText?.takeIf { it.isNotBlank() }?.let { add("hint=$it") }
        stateDescription?.takeIf { it.isNotBlank() }?.let { add("state=$it") }
        error?.takeIf { it.isNotBlank() }?.let { add("error=$it") }
        children.forEach { addAll(it.allTexts()) }
    }

    fun allViewIds(): List<String> = buildList {
        viewId?.takeIf { it.isNotBlank() }?.let { add(it) }
        children.forEach { addAll(it.allViewIds()) }
    }
}
