package com.calculocorridas.presentation.screens.developer

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.CompareArrows
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Difference
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculocorridas.inspector.models.AccessibilityDump
import com.calculocorridas.inspector.models.DiffResult
import com.calculocorridas.inspector.models.DumpMeta
import com.calculocorridas.presentation.viewmodels.InspectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InspectorScreen(
    viewModel: InspectorViewModel = hiltViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var confirmDeleteAll by remember { mutableStateOf(false) }
    var compareMode by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.refreshDumps() }

    LaunchedEffect(uiState.exportMessage) {
        uiState.exportMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearExportMessage()
        }
    }

    if (uiState.selectedDump != null) {
        DumpDetailScreen(
            dump        = uiState.selectedDump!!,
            txt         = uiState.selectedTxt,
            dumps       = uiState.dumps,
            diffResult  = uiState.diffResult,
            compareDump = uiState.compareDump,
            onBack      = { viewModel.clearSelection() },
            onShare     = { viewModel.shareDump(uiState.selectedDump!!.id) },
            onDelete    = {
                viewModel.deleteDump(uiState.selectedDump!!.id)
                viewModel.clearSelection()
            },
            onCompare   = { prefix -> viewModel.selectCompare(prefix) }
        )
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Accessibility Inspector", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refreshDumps() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Atualizar")
                    }
                    IconButton(onClick = { viewModel.exportAllZip() }) {
                        Icon(Icons.Default.Archive, contentDescription = "Exportar ZIP")
                    }
                    IconButton(onClick = { confirmDeleteAll = true }) {
                        Icon(Icons.Default.DeleteSweep, contentDescription = "Apagar tudo",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        if (uiState.dumps.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Search, contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                    Text("Nenhum dump disponível",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Ative o Inspector e abra o Uber Driver ou outro app monitorado",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
                }
            }
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding).padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // Stats bar
            item {
                Card(
                    shape  = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        StatChip("Total", "${uiState.dumps.size}")
                        StatChip("Uber", "${uiState.dumps.count { it.appSource == "uber" }}")
                        StatChip("99", "${uiState.dumps.count { it.appSource == "99" }}")
                        StatChip("inDrive", "${uiState.dumps.count { it.appSource == "indrive" }}")
                        StatChip("iFood", "${uiState.dumps.count { it.appSource == "ifood" }}")
                    }
                }
            }

            items(uiState.dumps, key = { it.prefix }) { meta ->
                DumpCard(
                    meta    = meta,
                    onClick = { viewModel.selectDump(meta.prefix) },
                    onShare = { viewModel.shareDump(meta.prefix) },
                    onDelete = { viewModel.deleteDump(meta.prefix) }
                )
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }

    if (confirmDeleteAll) {
        AlertDialog(
            onDismissRequest = { confirmDeleteAll = false },
            title  = { Text("Apagar todos os dumps?") },
            text   = { Text("Esta ação não pode ser desfeita.") },
            confirmButton = {
                TextButton(onClick = { viewModel.deleteAll(); confirmDeleteAll = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)) {
                    Text("Apagar tudo")
                }
            },
            dismissButton = { TextButton(onClick = { confirmDeleteAll = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DumpCard(meta: DumpMeta, onClick: () -> Unit, onShare: () -> Unit, onDelete: () -> Unit) {
    val triggerColor = when (meta.trigger) {
        "parse_failed"   -> MaterialTheme.colorScheme.error
        "ride_detected"  -> Color(0xFF2E7D32)
        "manual"         -> MaterialTheme.colorScheme.primary
        else             -> MaterialTheme.colorScheme.secondary
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable { onClick() },
        shape    = RoundedCornerShape(12.dp),
        colors   = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    AppBadge(meta.appSource)
                    Surface(shape = RoundedCornerShape(4.dp), color = triggerColor.copy(alpha = 0.15f)) {
                        Text(meta.trigger, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall, color = triggerColor)
                    }
                }
                Row {
                    IconButton(onClick = onShare, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar", modifier = Modifier.size(16.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar",
                            modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                    }
                }
            }
            Text(meta.timestampFormatted, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                InfoLabel("Nós", "${meta.totalNodes}")
                InfoLabel("Textos", "${meta.totalTexts}")
                InfoLabel("Parser", if (meta.parserResult != null) "OK" else "FALHOU",
                    color = if (meta.parserResult != null) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error)
                InfoLabel("Tam", "${meta.sizeBytes / 1024}KB")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DumpDetailScreen(
    dump: AccessibilityDump,
    txt: String?,
    dumps: List<DumpMeta>,
    diffResult: DiffResult?,
    compareDump: AccessibilityDump?,
    onBack: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
    onCompare: (String) -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showComparePicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(dump.appSource.uppercase(), fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium)
                        Text(dump.timestampFormatted, style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showComparePicker = true }) {
                        Icon(Icons.AutoMirrored.Filled.CompareArrows, contentDescription = "Comparar")
                    }
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Compartilhar")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Apagar",
                            tint = MaterialTheme.colorScheme.error)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding)) {
            TabRow(selectedTabIndex = selectedTab) {
                Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) { Text("Resumo", modifier = Modifier.padding(vertical = 12.dp)) }
                Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) { Text("Árvore TXT", modifier = Modifier.padding(vertical = 12.dp)) }
                Tab(selected = selectedTab == 2, onClick = { selectedTab = 2 }, enabled = diffResult != null) {
                    Text("Diff${if (diffResult != null) " ●" else ""}", modifier = Modifier.padding(vertical = 12.dp))
                }
            }

            when (selectedTab) {
                0 -> DumpSummaryTab(dump)
                1 -> DumpTxtTab(txt)
                2 -> if (diffResult != null && compareDump != null) DiffTab(dump, compareDump, diffResult)
            }
        }
    }

    if (showComparePicker) {
        AlertDialog(
            onDismissRequest = { showComparePicker = false },
            title = { Text("Selecionar dump para comparar") },
            text = {
                LazyColumn(modifier = Modifier.height(300.dp)) {
                    items(dumps.filter { it.prefix != dump.id }) { meta ->
                        Row(
                            modifier = Modifier.fillMaxWidth().clickable {
                                onCompare(meta.prefix)
                                showComparePicker = false
                                selectedTab = 2
                            }.padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AppBadge(meta.appSource)
                            Column {
                                Text(meta.timestampFormatted, style = MaterialTheme.typography.bodySmall)
                                Text("nós=${meta.totalNodes} textos=${meta.totalTexts}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            }
                        }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showComparePicker = false }) { Text("Cancelar") } }
        )
    }
}

@Composable
private fun DumpSummaryTab(dump: AccessibilityDump) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            InfoCard("METADADOS") {
                InfoRow("App",       "${dump.appSource} (${dump.packageName})")
                InfoRow("Trigger",   dump.trigger)
                InfoRow("Seletores", "v${dump.selectorVersion}")
                InfoRow("Parser",    dump.parserResult ?: "null")
                InfoRow("Timestamp", dump.timestampFormatted)
            }
        }
        item {
            InfoCard("EVENTO") {
                dump.event?.let {
                    InfoRow("Tipo",  "${it.eventTypeName} (${it.eventType})")
                    InfoRow("Pkg",   it.packageName ?: "-")
                    InfoRow("Class", it.className ?: "-")
                    if (it.texts.isNotEmpty()) InfoRow("Textos", it.texts.joinToString(" | "))
                } ?: Text("Nenhum evento capturado", style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
        if (dump.bufferedTexts.isNotEmpty()) {
            item {
                InfoCard("BUFFER TYPE_VIEW_TEXT_CHANGED (${dump.bufferedTexts.size})") {
                    dump.bufferedTexts.forEachIndexed { i, t ->
                        Text("[$i] $t", style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace)
                    }
                }
            }
        }
        item {
            InfoCard("JANELAS (${dump.windows.size})") {
                dump.windows.forEach { w ->
                    val isFocus = w.packageName == dump.packageName
                    Row(
                        modifier = Modifier.fillMaxWidth().then(
                            if (isFocus) Modifier.background(
                                MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                RoundedCornerShape(6.dp)
                            ).padding(4.dp) else Modifier
                        ),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text("${w.typeName} — ${w.packageName}",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = if (isFocus) FontWeight.Bold else FontWeight.Normal)
                            Text("nós=${w.totalNodes} textos=${w.totalTexts} layer=${w.layer}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                        }
                        if (isFocus) Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                        ) {
                            Text("FOCO", modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Spacer(Modifier.height(4.dp))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun DumpTxtTab(txt: String?) {
    if (txt == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Arquivo TXT não encontrado", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
        }
        return
    }
    Column(
        modifier = Modifier.fillMaxSize()
            .verticalScroll(rememberScrollState())
            .horizontalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Text(txt, style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
            fontFamily = FontFamily.Monospace, lineHeight = 16.sp)
    }
}

@Composable
private fun DiffTab(old: AccessibilityDump, new: AccessibilityDump, diff: DiffResult) {
    LazyColumn(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            InfoCard("COMPARAÇÃO") {
                InfoRow("Dump A (antigo)", "${old.appSource} ${old.timestampFormatted}")
                InfoRow("Dump B (novo)",   "${new.appSource} ${new.timestampFormatted}")
                InfoRow("Nós A → B",       "${diff.oldNodeCount} → ${diff.newNodeCount}")
                InfoRow("Textos A → B",    "${diff.oldTextCount} → ${diff.newTextCount}")
                InfoRow("Estrutura",       if (diff.structureChanged) "ALTERADA" else "igual")
            }
        }
        if (diff.addedViewIds.isNotEmpty()) {
            item {
                InfoCard("ADICIONADOS (${diff.addedViewIds.size})", titleColor = Color(0xFF2E7D32)) {
                    diff.addedViewIds.forEach { id ->
                        Text("+ $id", style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace, color = Color(0xFF2E7D32))
                    }
                }
            }
        }
        if (diff.removedViewIds.isNotEmpty()) {
            item {
                InfoCard("REMOVIDOS (${diff.removedViewIds.size})", titleColor = MaterialTheme.colorScheme.error) {
                    diff.removedViewIds.forEach { id ->
                        Text("- $id", style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        if (diff.textChanges.isNotEmpty()) {
            item {
                InfoCard("TEXTOS ALTERADOS (${diff.textChanges.size})", titleColor = MaterialTheme.colorScheme.tertiary) {
                    diff.textChanges.forEach { change ->
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text("${change.className} ${change.viewId ?: ""}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                            Text("- ${change.oldText}", style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace, color = MaterialTheme.colorScheme.error)
                            Text("+ ${change.newText}", style = MaterialTheme.typography.bodySmall,
                                fontFamily = FontFamily.Monospace, color = Color(0xFF2E7D32))
                        }
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
        if (!diff.hasDifferences) {
            item {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    Text("Nenhuma diferença encontrada nos IDs e textos.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                }
            }
        }
        item { Spacer(Modifier.height(16.dp)) }
    }
}

// ── Shared composables ──────────────────────────────────────────────────────

@Composable
private fun InfoCard(
    title: String,
    titleColor: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelSmall, color = titleColor,
            modifier = Modifier.padding(bottom = 4.dp))
        Card(shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.weight(0.35f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(0.65f),
            maxLines = 3, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun AppBadge(appSource: String) {
    val color = when (appSource) {
        "uber"    -> Color(0xFF000000)
        "99"      -> Color(0xFFFFD700)
        "indrive" -> Color(0xFF1DB954)
        "ifood"   -> Color(0xFFEA1D2C)
        else      -> MaterialTheme.colorScheme.primary
    }
    Surface(shape = RoundedCornerShape(6.dp), color = color.copy(alpha = 0.15f)) {
        Text(appSource.uppercase(),
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
            style = MaterialTheme.typography.labelSmall,
            color = color, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StatChip(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onPrimaryContainer)
        Text(label, style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
    }
}

@Composable
private fun InfoLabel(label: String, value: String, color: Color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = color)
        Text(label, style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f))
    }
}
