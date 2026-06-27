package com.calculocorridas.inspector

import android.content.Context
import android.graphics.Rect
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.view.accessibility.AccessibilityWindowInfo
import com.calculocorridas.data.inspector.InspectorPreferences
import com.calculocorridas.inspector.models.AccessibilityDump
import com.calculocorridas.inspector.models.ActionSnapshot
import com.calculocorridas.inspector.models.CollectionInfoSnapshot
import com.calculocorridas.inspector.models.CollectionItemInfoSnapshot
import com.calculocorridas.inspector.models.DiffResult
import com.calculocorridas.inspector.models.DumpMeta
import com.calculocorridas.inspector.models.EventSnapshot
import com.calculocorridas.inspector.models.NodeSnapshot
import com.calculocorridas.inspector.models.RangeInfoSnapshot
import com.calculocorridas.inspector.models.RectSnapshot
import com.calculocorridas.inspector.models.TextChange
import com.calculocorridas.inspector.models.WindowSnapshot
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG           = "Inspector"
private const val SCHEMA_VERSION = 2
private val DATE_FILE    = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
private val DATE_DISPLAY = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.US)
private val DATE_DAY     = SimpleDateFormat("yyyy-MM-dd",       Locale.US)
private val GSON: Gson   = GsonBuilder().setPrettyPrinting().serializeNulls().create()

private fun contentChangeTypeNames(mask: Int): List<String> = buildList {
    if (mask and 0x1   != 0) add("SUBTREE")
    if (mask and 0x2   != 0) add("TEXT")
    if (mask and 0x4   != 0) add("CONTENT_DESCRIPTION")
    if (mask and 0x8   != 0) add("UNDEFINED")
    if (mask and 0x10  != 0) add("CHILDREN_BATCH")
    if (mask and 0x20  != 0) add("MOVEMENT_GRANULARITY")
    if (mask and 0x40  != 0) add("STATE_DESCRIPTION")
    if (mask and 0x80  != 0) add("DRAG_STARTED")
    if (mask and 0x100 != 0) add("DRAG_DROPPED")
    if (mask and 0x200 != 0) add("DRAG_CANCELLED")
    if (mask and 0x400 != 0) add("SCROLLED")
    if (mask and 0x800 != 0) add("APPEARANCE")
    if (mask and 0x1000 != 0) add("VISIBLE")
    if (mask and 0x2000 != 0) add("PANE_DISAPPEARED")
    if (mask and 0x4000 != 0) add("PANE_APPEARED")
    if (mask and 0x8000 != 0) add("PANE_TITLE")
}

private fun windowChangeTypeNames(mask: Int): List<String> = buildList {
    if (mask and 0x1   != 0) add("ADDED")
    if (mask and 0x2   != 0) add("REMOVED")
    if (mask and 0x4   != 0) add("TITLE")
    if (mask and 0x8   != 0) add("LAYER")
    if (mask and 0x10  != 0) add("BOUNDS")
    if (mask and 0x20  != 0) add("ACTIVE")
    if (mask and 0x40  != 0) add("ACCESSIBILITY_FOCUSED")
    if (mask and 0x80  != 0) add("FOCUSED")
    if (mask and 0x100 != 0) add("PARENT")
    if (mask and 0x200 != 0) add("CHILDREN")
    if (mask and 0x400 != 0) add("PIP")
}

private fun granularityName(g: Int) = when (g) {
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER -> "CHAR"
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD      -> "WORD"
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE      -> "LINE"
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PARAGRAPH -> "PARAGRAPH"
    AccessibilityNodeInfo.MOVEMENT_GRANULARITY_PAGE      -> "PAGE"
    else -> "GRANULARITY($g)"
}

@Singleton
class AccessibilityInspector @Inject constructor(
    @ApplicationContext private val context: Context,
    val prefs: InspectorPreferences
) {
    private val dumpDir: File
        get() = File(context.getExternalFilesDir(null), "accessibility_dumps").also { it.mkdirs() }

    // ── Rate limiter ──────────────────────────────────────────────────────────
    private val lastDumpMs    = mutableMapOf<String, Long>()   // appKey → ms
    private val dailyCounts   = mutableMapOf<String, Int>()    // "yyyy-MM-dd" → count

    suspend fun shouldDump(trigger: String): Boolean = prefs.shouldDump(trigger)

    suspend fun checkAndRecordRateLimit(appKey: String): Boolean {
        val minMs   = prefs.rateLimitMinutes.collect().toLong() * 60_000L
        val maxDay  = prefs.rateLimitMaxPerDay.collect()
        val now     = System.currentTimeMillis()
        val today   = DATE_DAY.format(Date())

        val last = lastDumpMs[appKey] ?: 0L
        if (now - last < minMs) {
            Log.d(TAG, "[$appKey] Rate limit: aguardando ${(minMs - (now - last)) / 1000}s")
            return false
        }
        val count = dailyCounts.getOrDefault(today, 0)
        if (count >= maxDay) {
            Log.d(TAG, "[$appKey] Rate limit: limite diário ($maxDay) atingido")
            return false
        }
        lastDumpMs[appKey] = now
        dailyCounts[today]  = count + 1
        return true
    }

    // ── Capture ───────────────────────────────────────────────────────────────

    fun capture(
        appSource: String,
        packageName: String,
        trigger: String,
        event: CapturedEvent?,
        windows: List<AccessibilityWindowInfo>?,
        bufferedTexts: List<String>,
        selectorVersion: Int,
        parsedResult: String?
    ): AccessibilityDump {
        val now = System.currentTimeMillis()
        val id  = "${appSource}_${DATE_FILE.format(Date(now))}"

        val windowSnapshots = windows?.mapNotNull { w ->
            runCatching { snapshotWindow(w) }.getOrNull()
        } ?: emptyList()

        return AccessibilityDump(
            id                 = id,
            timestamp          = now,
            timestampFormatted = DATE_DISPLAY.format(Date(now)),
            appSource          = appSource,
            packageName        = packageName,
            trigger            = trigger,
            selectorVersion    = selectorVersion,
            event              = event?.toSnapshot(),
            windows            = windowSnapshots,
            totalNodes         = windowSnapshots.sumOf { it.totalNodes },
            totalTexts         = windowSnapshots.sumOf { it.totalTexts },
            parserResult       = parsedResult,
            bufferedTexts      = bufferedTexts,
            androidSdkInt      = Build.VERSION.SDK_INT,
            inspectorVersion   = SCHEMA_VERSION
        )
    }

    // ── Save ──────────────────────────────────────────────────────────────────

    suspend fun saveDump(dump: AccessibilityDump): String = withContext(Dispatchers.IO) {
        try {
            val prefix = dump.id
            File(dumpDir, "$prefix.json").writeText(GSON.toJson(dump))
            File(dumpDir, "$prefix.txt").writeText(dump.toTxt())
            updateMetadata(dump, prefix)
            Log.i(TAG, "Dump salvo: $prefix (nós=${dump.totalNodes} textos=${dump.totalTexts})")
            prefix
        } catch (e: Exception) {
            Log.e(TAG, "Erro ao salvar dump", e)
            ""
        }
    }

    // ── List / Read ───────────────────────────────────────────────────────────

    fun listDumps(): List<DumpMeta> = runCatching {
        dumpDir.listFiles { f -> f.name.endsWith(".json") && f.name != "metadata.json" }
            ?.sortedByDescending { it.lastModified() }
            ?.mapNotNull { file ->
                runCatching {
                    val d = GSON.fromJson(file.readText(), AccessibilityDump::class.java)
                    DumpMeta(
                        prefix             = file.nameWithoutExtension,
                        appSource          = d.appSource,
                        timestamp          = d.timestamp,
                        timestampFormatted = d.timestampFormatted,
                        trigger            = d.trigger,
                        totalNodes         = d.totalNodes,
                        totalTexts         = d.totalTexts,
                        parserResult       = d.parserResult,
                        sizeBytes          = file.length()
                    )
                }.getOrNull()
            } ?: emptyList()
    }.getOrDefault(emptyList())

    fun readDump(prefix: String): AccessibilityDump? = runCatching {
        GSON.fromJson(File(dumpDir, "$prefix.json").readText(), AccessibilityDump::class.java)
    }.getOrNull()

    fun readTxt(prefix: String): String? = runCatching {
        File(dumpDir, "$prefix.txt").readText()
    }.getOrNull()

    // ── Diff ──────────────────────────────────────────────────────────────────

    fun diff(old: AccessibilityDump, new: AccessibilityDump): DiffResult {
        val oldIds = old.windows.flatMap { it.rootNode?.allViewIds() ?: emptyList() }.toSet()
        val newIds = new.windows.flatMap { it.rootNode?.allViewIds() ?: emptyList() }.toSet()
        val oldTxt = buildTextMap(old)
        val newTxt = buildTextMap(new)
        val changes = oldTxt.mapNotNull { (k, v) ->
            val nv = newTxt[k]; if (nv != null && nv != v) {
                val p = k.split("|"); TextChange(p.getOrNull(0), p.getOrNull(1), v, nv)
            } else null
        }
        return DiffResult(
            oldId          = old.id,
            newId          = new.id,
            addedViewIds   = (newIds - oldIds).toList(),
            removedViewIds = (oldIds - newIds).toList(),
            textChanges    = changes,
            oldNodeCount   = old.totalNodes,
            newNodeCount   = new.totalNodes,
            oldTextCount   = old.totalTexts,
            newTextCount   = new.totalTexts,
            structureChanged = old.totalNodes != new.totalNodes
        )
    }

    // ── Export ZIP ────────────────────────────────────────────────────────────

    suspend fun exportZip(): File = withContext(Dispatchers.IO) {
        val zip = File(context.cacheDir, "inspector_${DATE_FILE.format(Date())}.zip")
        ZipOutputStream(zip.outputStream().buffered()).use { out ->
            dumpDir.listFiles()?.forEach { f ->
                out.putNextEntry(ZipEntry(f.name))
                f.inputStream().use { it.copyTo(out) }
                out.closeEntry()
            }
            out.putNextEntry(ZipEntry("README.txt"))
            out.write(buildReadme().toByteArray())
            out.closeEntry()
        }
        zip
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    fun delete(prefix: String) { File(dumpDir, "$prefix.json").delete(); File(dumpDir, "$prefix.txt").delete() }
    fun deleteAll() { dumpDir.listFiles()?.forEach { it.delete() } }

    // ── Node capture — COMPLETO ───────────────────────────────────────────────

    private fun snapshotNode(
        node: AccessibilityNodeInfo?,
        depth: Int,
        siblingIndex: Int,
        saveFullTree: Boolean
    ): NodeSnapshot? {
        node ?: return null

        // Refresh antes de ler — detecta se houve mudança
        runCatching { node.refresh() }

        val boundsScreen = Rect().also { node.getBoundsInScreen(it) }.toSnap()
        val boundsParent = Rect().also { node.getBoundsInParent(it) }.toSnap()

        val extras = bundleToMap(node.extras)

        val actions = node.actionList?.map { a ->
            ActionSnapshot(
                id           = a.id,
                standardName = actionName(a.id),
                label        = a.label?.toString()
            )
        } ?: emptyList()

        val rangeInfo = node.rangeInfo?.let {
            RangeInfoSnapshot(
                type     = it.type,
                typeName = when (it.type) { 0 -> "INT"; 1 -> "FLOAT"; 2 -> "PERCENT"; else -> "UNKNOWN" },
                min      = it.min,
                max      = it.max,
                current  = it.current
            )
        }

        val collInfo = node.collectionInfo?.let {
            CollectionInfoSnapshot(
                rowCount       = it.rowCount,
                columnCount    = it.columnCount,
                isHierarchical = it.isHierarchical,
                selectionMode  = it.selectionMode
            )
        }

        val collItemInfo = node.collectionItemInfo?.let {
            CollectionItemInfoSnapshot(
                rowIndex    = it.rowIndex,
                rowSpan     = it.rowSpan,
                columnIndex = it.columnIndex,
                columnSpan  = it.columnSpan,
                isHeading   = it.isHeading,
                isSelected  = it.isSelected
            )
        }

        val children = if (saveFullTree) {
            (0 until node.childCount).mapIndexed { i, _ ->
                snapshotNode(runCatching { node.getChild(i) }.getOrNull(), depth + 1, i, true)
            }.filterNotNull()
        } else emptyList()

        return NodeSnapshot(
            depth                     = depth,
            siblingIndex              = siblingIndex,
            className                 = node.className?.toString(),
            packageName               = node.packageName?.toString(),
            viewIdFull                = node.viewIdResourceName?.toString(),
            viewId                    = node.viewIdResourceName?.substringAfter('/'),
            text                      = node.text?.toString(),
            contentDescription        = node.contentDescription?.toString(),
            hintText                  = if (Build.VERSION.SDK_INT >= 26) node.hintText?.toString() else null,
            paneTitle                 = if (Build.VERSION.SDK_INT >= 28) node.paneTitle?.toString() else null,
            stateDescription          = if (Build.VERSION.SDK_INT >= 30) node.stateDescription?.toString() else null,
            tooltipText               = if (Build.VERSION.SDK_INT >= 28) node.tooltipText?.toString() else null,
            error                     = node.error?.toString(),
            uniqueId                  = if (Build.VERSION.SDK_INT >= 33) node.uniqueId?.toString() else null,
            textSelectionStart        = node.textSelectionStart,
            textSelectionEnd          = node.textSelectionEnd,
            inputType                 = node.inputType,
            nodeToString              = runCatching { node.toString() }.getOrNull(),
            boundsInScreen            = boundsScreen,
            boundsInParent            = boundsParent,
            drawingOrder              = if (Build.VERSION.SDK_INT >= 24) node.drawingOrder else -1,
            liveRegion                = node.liveRegion,
            visibleToUser             = node.isVisibleToUser,
            enabled                   = node.isEnabled,
            clickable                 = node.isClickable,
            longClickable             = node.isLongClickable,
            contextClickable          = if (Build.VERSION.SDK_INT >= 23) node.isContextClickable else false,
            focusable                 = node.isFocusable,
            focused                   = node.isFocused,
            accessibilityFocused      = node.isAccessibilityFocused,
            checkable                 = node.isCheckable,
            checked                   = node.isChecked,
            scrollable                = node.isScrollable,
            editable                  = node.isEditable,
            selected                  = node.isSelected,
            password                  = node.isPassword,
            dismissable               = node.isDismissable,
            multiLine                 = node.isMultiLine,
            importantForAccessibility = if (Build.VERSION.SDK_INT >= 24) node.isImportantForAccessibility else true,
            screenReaderFocusable     = if (Build.VERSION.SDK_INT >= 28) node.isScreenReaderFocusable else false,
            childCount                = node.childCount,
            labelForViewId            = runCatching { node.labelFor?.viewIdResourceName?.toString() }.getOrNull(),
            labeledByViewId           = runCatching { node.labeledBy?.viewIdResourceName?.toString() }.getOrNull(),
            collectionInfo            = collInfo,
            collectionItemInfo        = collItemInfo,
            rangeInfo                 = rangeInfo,
            actions                   = actions,
            extras                    = extras,
            children                  = children
        )
    }

    private fun snapshotWindow(window: AccessibilityWindowInfo): WindowSnapshot {
        val root       = runCatching { window.root }.getOrNull()
        val rootSnap   = runCatching { snapshotNode(root, 0, 0, true) }.getOrNull()
        val totalNodes = countNodes(root)
        val totalTexts = root?.let { collectTexts(it).size } ?: 0
        val bounds     = Rect().also { runCatching { window.getBoundsInScreen(it) } }.toSnap()

        return WindowSnapshot(
            windowId               = window.id,
            type                   = window.type,
            typeName               = windowTypeName(window.type),
            packageName            = root?.packageName?.toString(),
            title                  = runCatching { window.title?.toString() }.getOrNull(),
            layer                  = window.layer,
            isActive               = window.isActive,
            isFocused              = window.isFocused,
            isAccessibilityFocused = window.isAccessibilityFocused,
            isInPictureInPictureMode = if (Build.VERSION.SDK_INT >= 26) window.isInPictureInPictureMode else false,
            bounds                 = bounds,
            totalNodes             = totalNodes,
            totalTexts             = totalTexts,
            rootNode               = rootSnap
        )
    }

    // ── TXT ───────────────────────────────────────────────────────────────────

    private fun AccessibilityDump.toTxt(): String = buildString {
        appendLine("╔══════════════════════════════════════════════════════════════╗")
        appendLine("║  ACCESSIBILITY INSPECTOR — Cálculo de Corridas v$inspectorVersion        ║")
        appendLine("╚══════════════════════════════════════════════════════════════╝")
        appendLine()
        appendLine("App:         $appSource  ($packageName)")
        appendLine("Timestamp:   $timestampFormatted")
        appendLine("Trigger:     $trigger")
        appendLine("Seletores:   v$selectorVersion")
        appendLine("Parser:      ${parserResult ?: "null"}")
        appendLine("Android SDK: $androidSdkInt")
        appendLine("Schema:      v$inspectorVersion")
        appendLine()

        event?.let { ev ->
            appendLine("────────────────────── EVENTO ──────────────────────────────────")
            appendLine("  tipo:          ${ev.eventTypeName} (${ev.eventType})")
            appendLine("  tempo:         ${ev.eventTime}")
            appendLine("  pkg:           ${ev.packageName}")
            appendLine("  className:     ${ev.className}")
            if (ev.texts.isNotEmpty())
                appendLine("  texts:         ${ev.texts}")
            ev.contentDescription?.let { appendLine("  contentDesc:   $it") }
            ev.beforeText?.let        { appendLine("  beforeText:    $it") }
            if (ev.contentChangeTypes != 0)
                appendLine("  changeTypes:   ${ev.contentChangeTypeNames} (0x${ev.contentChangeTypes.toString(16)})")
            if (ev.windowChangeTypes != 0)
                appendLine("  windowChange:  ${ev.windowChangeTypeNames} (0x${ev.windowChangeTypes.toString(16)})")
            if (ev.action != 0)
                appendLine("  action:        ${ev.actionName} (${ev.action})")
            if (ev.movementGranularity != 0)
                appendLine("  granularity:   ${ev.movementGranularityName}")
            if (ev.itemCount > 0)
                appendLine("  list:          item=${ev.currentItemIndex}/${ev.itemCount} from=${ev.fromIndex} to=${ev.toIndex} +${ev.addedCount} -${ev.removedCount}")
            if (ev.scrollX != 0 || ev.scrollY != 0)
                appendLine("  scroll:        (${ev.scrollX},${ev.scrollY}) max=(${ev.maxScrollX},${ev.maxScrollY})")
            ev.sourceViewId?.let { appendLine("  src.viewId:    $it") }
            ev.sourceText?.let   { appendLine("  src.text:      $it") }
            appendLine()
        }

        if (bufferedTexts.isNotEmpty()) {
            appendLine("────────────── BUFFER TYPE_VIEW_TEXT_CHANGED (${bufferedTexts.size}) ───────────────")
            bufferedTexts.forEachIndexed { i, t -> appendLine("  [$i] $t") }
            appendLine()
        }

        appendLine("────────────────── JANELAS (${windows.size}) ───────────────────────────────")
        windows.forEachIndexed { i, w ->
            val focus = if (w.packageName == packageName) "  ◄ FOCO" else ""
            appendLine()
            appendLine("  [Janela $i] ${w.typeName}$focus")
            appendLine("  pkg:     ${w.packageName}")
            appendLine("  título:  ${w.title ?: "-"}")
            appendLine("  layer:   ${w.layer} | active=${w.isActive} focused=${w.isFocused} pip=${w.isInPictureInPictureMode}")
            appendLine("  bounds:  ${w.bounds}")
            appendLine("  nós:     ${w.totalNodes} | textos: ${w.totalTexts}")
            appendLine()
            w.rootNode?.let {
                appendLine("  ÁRVORE:")
                appendNodeTxt(it, 4, isLast = true)
            }
        }
        appendLine()
        appendLine("────────────────── RESUMO ───────────────────────────────────────")
        appendLine("  Nós totais:    $totalNodes")
        appendLine("  Textos totais: $totalTexts")
        appendLine("╚══════════════════════════════════════════════════════════════╝")
    }

    private fun StringBuilder.appendNodeTxt(node: NodeSnapshot, indent: Int, isLast: Boolean) {
        val pad    = " ".repeat(indent)
        val branch = if (isLast) "└─ " else "├─ "
        val cls    = node.className?.substringAfterLast('.') ?: "?"
        val id     = node.viewId?.let { " [id=$it]" } ?: ""
        val draw   = if (node.drawingOrder >= 0) " z=${node.drawingOrder}" else ""

        appendLine("$pad$branch$cls$id$draw")

        val childPad = " ".repeat(indent + 3)

        // Textos
        val textLines = buildList {
            node.text?.takeIf { it.isNotBlank() }?.let              { add("text='$it'") }
            node.contentDescription?.takeIf { it.isNotBlank() }?.let { add("desc='$it'") }
            node.hintText?.takeIf { it.isNotBlank() }?.let           { add("hint='$it'") }
            node.stateDescription?.takeIf { it.isNotBlank() }?.let   { add("state='$it'") }
            node.tooltipText?.takeIf { it.isNotBlank() }?.let        { add("tooltip='$it'") }
            node.error?.takeIf { it.isNotBlank() }?.let              { add("error='$it'") }
            node.paneTitle?.takeIf { it.isNotBlank() }?.let          { add("pane='$it'") }
        }
        if (textLines.isNotEmpty()) appendLine("$childPad  ${textLines.joinToString(" | ")}")

        // Flags relevantes
        val flags = buildList {
            if (!node.visibleToUser)       add("INVISIBLE")
            if (!node.enabled)             add("DISABLED")
            if (node.clickable)            add("click")
            if (node.longClickable)        add("longClick")
            if (node.scrollable)           add("scroll")
            if (node.focusable)            add("focusable")
            if (node.focused)              add("FOCUSED")
            if (node.accessibilityFocused) add("A11Y_FOCUSED")
            if (node.checkable)            add("checkable[${node.checked}]")
            if (node.editable)             add("editable")
            if (node.password)             add("PASSWORD")
            if (node.multiLine)            add("multiLine")
            if (!node.importantForAccessibility) add("not_important")
            if (node.screenReaderFocusable) add("screenReader")
        }
        if (flags.isNotEmpty()) appendLine("$childPad  [${ flags.joinToString(", ")}]")

        // Bounds
        node.boundsInScreen?.let { appendLine("$childPad  bounds=$it") }

        // inputType
        if (node.inputType != 0) appendLine("$childPad  inputType=0x${node.inputType.toString(16)}")

        // Seleção de texto
        if (node.textSelectionStart >= 0)
            appendLine("$childPad  selection=[${node.textSelectionStart},${node.textSelectionEnd}]")

        // liveRegion
        val liveStr = when (node.liveRegion) { 1 -> "POLITE"; 2 -> "ASSERTIVE"; else -> null }
        liveStr?.let { appendLine("$childPad  liveRegion=$it") }

        // collectionInfo
        node.collectionInfo?.let { ci ->
            appendLine("$childPad  collection: rows=${ci.rowCount} cols=${ci.columnCount} hierarchical=${ci.isHierarchical}")
        }
        node.collectionItemInfo?.let { ci ->
            appendLine("$childPad  collItem: row=${ci.rowIndex}(span=${ci.rowSpan}) col=${ci.columnIndex}(span=${ci.columnSpan}) heading=${ci.isHeading}")
        }

        // rangeInfo
        node.rangeInfo?.let { ri ->
            appendLine("$childPad  range[${ri.typeName}]: min=${ri.min} max=${ri.max} cur=${ri.current}")
        }

        // Actions (só as não-triviais)
        val nonTrivialActions = node.actions.filter { it.id !in setOf(1, 2, 4, 8) /* click, focus, select, clear_focus */ }
        if (nonTrivialActions.isNotEmpty())
            appendLine("$childPad  actions: ${nonTrivialActions.map { "${it.standardName}${it.label?.let { l -> "($l)" } ?: ""}" }}")

        // Labels
        node.labelForViewId?.let   { appendLine("$childPad  labelFor=$it") }
        node.labeledByViewId?.let  { appendLine("$childPad  labeledBy=$it") }

        // Extras
        if (node.extras.isNotEmpty())
            appendLine("$childPad  extras: ${node.extras}")

        // node.toString() — truncado a 200 chars para não poluir
        node.nodeToString?.let { ts ->
            val short = if (ts.length > 300) ts.take(300) + "…" else ts
            appendLine("$childPad  toString=$short")
        }

        // Filhos
        node.children.forEachIndexed { i, child ->
            appendNodeTxt(child, indent + 3, i == node.children.lastIndex)
        }
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun bundleToMap(bundle: Bundle?): Map<String, String> {
        if (bundle == null || bundle.isEmpty) return emptyMap()
        return buildMap {
            runCatching {
                bundle.keySet().forEach { key ->
                    val v = runCatching {
                        when (val raw = bundle.get(key)) {
                            is String   -> raw
                            is Int      -> raw.toString()
                            is Boolean  -> raw.toString()
                            is Float    -> raw.toString()
                            is Double   -> raw.toString()
                            is Long     -> raw.toString()
                            is IntArray -> raw.joinToString(",", "[", "]")
                            is LongArray -> raw.joinToString(",", "[", "]")
                            is FloatArray -> raw.joinToString(",", "[", "]")
                            is Bundle   -> "Bundle(${raw.keySet().size} keys)"
                            null        -> "null"
                            else        -> raw.toString().take(200)
                        }
                    }.getOrDefault("ERROR")
                    put(key, v)
                }
            }
        }
    }

    private fun countNodes(node: AccessibilityNodeInfo?): Int {
        node ?: return 0
        var c = 1
        for (i in 0 until node.childCount) c += countNodes(runCatching { node.getChild(i) }.getOrNull())
        return c
    }

    private fun collectTexts(node: AccessibilityNodeInfo): List<String> = buildList {
        node.text?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
        node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { add(it) }
        for (i in 0 until node.childCount) addAll(collectTexts(runCatching { node.getChild(i) }.getOrNull() ?: continue))
    }

    private fun buildTextMap(dump: AccessibilityDump): Map<String, String> = buildMap {
        fun walk(node: NodeSnapshot) {
            val key = "${node.viewId ?: ""}|${node.className ?: ""}|${node.depth}"
            val t   = listOfNotNull(node.text, node.contentDescription).firstOrNull { it.isNotBlank() }
            if (t != null) put(key, t)
            node.children.forEach { walk(it) }
        }
        dump.windows.forEach { it.rootNode?.let { r -> walk(r) } }
    }

    private fun updateMetadata(dump: AccessibilityDump, prefix: String) {
        val meta = DumpMeta(
            prefix             = prefix,
            appSource          = dump.appSource,
            timestamp          = dump.timestamp,
            timestampFormatted = dump.timestampFormatted,
            trigger            = dump.trigger,
            totalNodes         = dump.totalNodes,
            totalTexts         = dump.totalTexts,
            parserResult       = dump.parserResult,
            sizeBytes          = File(dumpDir, "$prefix.json").length()
        )
        val metaFile = File(dumpDir, "metadata.json")
        val list = if (metaFile.exists())
            runCatching { GSON.fromJson(metaFile.readText(), Array<DumpMeta>::class.java)?.toMutableList() }.getOrNull() ?: mutableListOf()
        else mutableListOf()
        list.removeIf { it.prefix == prefix }
        list.add(0, meta)
        metaFile.writeText(GSON.toJson(list))
    }

    private fun buildReadme(): String = buildString {
        appendLine("Accessibility Inspector — Cálculo de Corridas (Schema v$SCHEMA_VERSION)")
        appendLine("Exportado em: ${DATE_DISPLAY.format(Date())}")
        appendLine()
        val dumps = listDumps()
        appendLine("Total de dumps: ${dumps.size}")
        dumps.forEach { d ->
            appendLine("  ${d.prefix} | app=${d.appSource} | trigger=${d.trigger} | nós=${d.totalNodes} | textos=${d.totalTexts} | parser=${d.parserResult ?: "null"}")
        }
        appendLine()
        appendLine("Estrutura dos arquivos:")
        appendLine("  .json — dump completo (todos os atributos de cada nó)")
        appendLine("  .txt  — árvore legível com identação para análise rápida")
        appendLine("  metadata.json — índice de todos os dumps")
        appendLine()
        appendLine("Campos capturados por nó:")
        appendLine("  text, contentDescription, hintText, paneTitle, stateDescription,")
        appendLine("  tooltipText, error, uniqueId, textSelection, inputType,")
        appendLine("  nodeToString (CRÍTICO para React Native/Compose),")
        appendLine("  boundsInScreen, boundsInParent, drawingOrder, liveRegion,")
        appendLine("  visibleToUser, enabled, clickable, longClickable, contextClickable,")
        appendLine("  focusable, focused, accessibilityFocused, checkable, checked,")
        appendLine("  scrollable, editable, selected, password, dismissable, multiLine,")
        appendLine("  importantForAccessibility, screenReaderFocusable,")
        appendLine("  collectionInfo, collectionItemInfo, rangeInfo,")
        appendLine("  actions (com labels), extras (Bundle completo)")
    }

    // ── Decoders ──────────────────────────────────────────────────────────────

    private fun Rect.toSnap() = RectSnapshot(left, top, right, bottom)

    private fun actionName(id: Int) = when (id) {
        AccessibilityNodeInfo.ACTION_CLICK              -> "CLICK"
        AccessibilityNodeInfo.ACTION_LONG_CLICK         -> "LONG_CLICK"
        AccessibilityNodeInfo.ACTION_FOCUS              -> "FOCUS"
        AccessibilityNodeInfo.ACTION_CLEAR_FOCUS        -> "CLEAR_FOCUS"
        AccessibilityNodeInfo.ACTION_SELECT             -> "SELECT"
        AccessibilityNodeInfo.ACTION_CLEAR_SELECTION    -> "CLEAR_SELECTION"
        AccessibilityNodeInfo.ACTION_ACCESSIBILITY_FOCUS -> "A11Y_FOCUS"
        AccessibilityNodeInfo.ACTION_CLEAR_ACCESSIBILITY_FOCUS -> "CLEAR_A11Y_FOCUS"
        AccessibilityNodeInfo.ACTION_SCROLL_FORWARD     -> "SCROLL_FORWARD"
        AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD    -> "SCROLL_BACKWARD"
        AccessibilityNodeInfo.ACTION_COPY               -> "COPY"
        AccessibilityNodeInfo.ACTION_PASTE              -> "PASTE"
        AccessibilityNodeInfo.ACTION_CUT                -> "CUT"
        AccessibilityNodeInfo.ACTION_SET_SELECTION      -> "SET_SELECTION"
        AccessibilityNodeInfo.ACTION_EXPAND             -> "EXPAND"
        AccessibilityNodeInfo.ACTION_COLLAPSE           -> "COLLAPSE"
        AccessibilityNodeInfo.ACTION_DISMISS            -> "DISMISS"
        AccessibilityNodeInfo.ACTION_SET_TEXT           -> "SET_TEXT"
        AccessibilityNodeInfo.ACTION_NEXT_AT_MOVEMENT_GRANULARITY -> "NEXT_GRANULARITY"
        AccessibilityNodeInfo.ACTION_PREVIOUS_AT_MOVEMENT_GRANULARITY -> "PREV_GRANULARITY"
        AccessibilityNodeInfo.ACTION_NEXT_HTML_ELEMENT  -> "NEXT_HTML"
        AccessibilityNodeInfo.ACTION_PREVIOUS_HTML_ELEMENT -> "PREV_HTML"
        0x01000000 -> "SHOW_ON_SCREEN"
        0x01000009 -> "SCROLL_TO_POS"
        0x02000000 -> "IME_ENTER"
        else -> "ACTION(0x${id.toString(16)})"
    }

    private fun windowTypeName(type: Int) = when (type) {
        AccessibilityWindowInfo.TYPE_APPLICATION          -> "app"
        AccessibilityWindowInfo.TYPE_INPUT_METHOD         -> "input"
        AccessibilityWindowInfo.TYPE_SYSTEM               -> "system"
        AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> "accessibility_overlay"
        AccessibilityWindowInfo.TYPE_SPLIT_SCREEN_DIVIDER -> "split_screen"
        else -> "unknown($type)"
    }

    // ── Helper para flow.first() ──────────────────────────────────────────────

    private suspend fun <T> kotlinx.coroutines.flow.Flow<T>.collect(): T = first()

    // ── CapturedEvent — preenchido no thread principal antes do recycle ────────

    data class CapturedEvent(
        val eventType: Int,
        val packageName: String?,
        val className: String?,
        val texts: List<String>,
        val contentDescription: String?,
        val beforeText: String?,
        val contentChangeTypes: Int,
        val windowChangeTypes: Int,
        val action: Int,
        val movementGranularity: Int,
        val itemCount: Int,
        val currentItemIndex: Int,
        val fromIndex: Int,
        val toIndex: Int,
        val addedCount: Int,
        val removedCount: Int,
        val scrollX: Int,
        val scrollY: Int,
        val maxScrollX: Int,
        val maxScrollY: Int,
        val eventTime: Long,
        val sourceClassName: String?,
        val sourceViewId: String?,
        val sourceText: String?,
        val sourceContentDescription: String?
    ) {
        fun toSnapshot(): EventSnapshot {
            val cct  = contentChangeTypeNames(contentChangeTypes)
            val wct  = windowChangeTypeNames(windowChangeTypes)
            val aname = when (action) {
                AccessibilityNodeInfo.ACTION_CLICK        -> "CLICK"
                AccessibilityNodeInfo.ACTION_FOCUS        -> "FOCUS"
                AccessibilityNodeInfo.ACTION_SCROLL_FORWARD -> "SCROLL_FORWARD"
                AccessibilityNodeInfo.ACTION_SCROLL_BACKWARD -> "SCROLL_BACKWARD"
                0 -> null
                else -> "ACTION($action)"
            }
            val gname = if (movementGranularity != 0) when (movementGranularity) {
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_CHARACTER -> "CHAR"
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_WORD      -> "WORD"
                AccessibilityNodeInfo.MOVEMENT_GRANULARITY_LINE      -> "LINE"
                else -> "GRANULARITY($movementGranularity)"
            } else null
            val typeName = when (eventType) {
                AccessibilityEvent.TYPE_VIEW_CLICKED               -> "VIEW_CLICKED"
                AccessibilityEvent.TYPE_VIEW_LONG_CLICKED          -> "VIEW_LONG_CLICKED"
                AccessibilityEvent.TYPE_VIEW_FOCUSED               -> "VIEW_FOCUSED"
                AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED          -> "VIEW_TEXT_CHANGED"
                AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED -> "TEXT_SELECTION_CHANGED"
                AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED       -> "WINDOW_STATE_CHANGED"
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED     -> "WINDOW_CONTENT_CHANGED"
                AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED -> "NOTIFICATION"
                AccessibilityEvent.TYPE_VIEW_SCROLLED              -> "VIEW_SCROLLED"
                AccessibilityEvent.TYPE_VIEW_SELECTED              -> "VIEW_SELECTED"
                AccessibilityEvent.TYPE_VIEW_HOVER_ENTER           -> "HOVER_ENTER"
                AccessibilityEvent.TYPE_VIEW_HOVER_EXIT            -> "HOVER_EXIT"
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_START    -> "TOUCH_START"
                AccessibilityEvent.TYPE_TOUCH_INTERACTION_END      -> "TOUCH_END"
                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_START -> "EXPLORE_START"
                AccessibilityEvent.TYPE_TOUCH_EXPLORATION_GESTURE_END   -> "EXPLORE_END"
                AccessibilityEvent.TYPE_ANNOUNCEMENT               -> "ANNOUNCEMENT"
                else -> "EVENT(${eventType.toString(16)})"
            }
            return EventSnapshot(
                eventType              = eventType,
                eventTypeName          = typeName,
                eventTime              = eventTime,
                packageName            = packageName,
                className              = className,
                texts                  = texts,
                contentDescription     = contentDescription,
                beforeText             = beforeText,
                contentChangeTypes     = contentChangeTypes,
                contentChangeTypeNames = cct,
                windowChangeTypes      = windowChangeTypes,
                windowChangeTypeNames  = wct,
                action                 = action,
                actionName             = aname,
                movementGranularity    = movementGranularity,
                movementGranularityName = gname,
                itemCount              = itemCount,
                currentItemIndex       = currentItemIndex,
                fromIndex              = fromIndex,
                toIndex                = toIndex,
                addedCount             = addedCount,
                removedCount           = removedCount,
                scrollX                = scrollX,
                scrollY                = scrollY,
                maxScrollX             = maxScrollX,
                maxScrollY             = maxScrollY,
                sourceClassName        = sourceClassName,
                sourceViewId           = sourceViewId,
                sourceText             = sourceText,
                sourceContentDescription = sourceContentDescription
            )
        }

        companion object {
            fun from(event: AccessibilityEvent): CapturedEvent {
                val src = runCatching { event.source }.getOrNull()
                return CapturedEvent(
                    eventType          = event.eventType,
                    packageName        = event.packageName?.toString(),
                    className          = event.className?.toString(),
                    texts              = event.text?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } } ?: emptyList(),
                    contentDescription = event.contentDescription?.toString(),
                    beforeText         = event.beforeText?.toString(),
                    contentChangeTypes = event.contentChangeTypes,
                    windowChangeTypes  = if (Build.VERSION.SDK_INT >= 28) event.windowChanges else 0,
                    action             = event.action,
                    movementGranularity = event.movementGranularity,
                    itemCount          = event.itemCount,
                    currentItemIndex   = event.currentItemIndex,
                    fromIndex          = event.fromIndex,
                    toIndex            = event.toIndex,
                    addedCount         = event.addedCount,
                    removedCount       = event.removedCount,
                    scrollX            = event.scrollX,
                    scrollY            = event.scrollY,
                    maxScrollX         = event.maxScrollX,
                    maxScrollY         = event.maxScrollY,
                    eventTime          = event.eventTime,
                    sourceClassName    = src?.className?.toString(),
                    sourceViewId       = src?.viewIdResourceName?.toString(),
                    sourceText         = src?.text?.toString(),
                    sourceContentDescription = src?.contentDescription?.toString()
                )
            }
        }
    }
}
