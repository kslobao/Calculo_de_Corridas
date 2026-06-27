package com.calculocorridas.presentation.screens.developer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculocorridas.BuildConfig
import com.calculocorridas.presentation.viewmodels.InspectorViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeveloperSettingsScreen(
    viewModel: InspectorViewModel = hiltViewModel(),
    onBack: () -> Unit,
    onNavigateInspector: () -> Unit
) {
    val state by viewModel.settingsState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Desenvolvedor", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Warning banner
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                )
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.BugReport, contentDescription = null,
                        tint = MaterialTheme.colorScheme.error)
                    Column {
                        Text("Opções para Desenvolvedores",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error)
                        Text("Essas opções geram dumps de acessibilidade para diagnóstico. " +
                            "Nenhum dado é coletado sem ativação explícita.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                }
            }

            if (!BuildConfig.DEBUG) {
                DevSection("MODO DESENVOLVEDOR") {
                    DevSwitchRow(
                        label       = "Ativar Modo Desenvolvedor",
                        description = "Habilita funcionalidades de diagnóstico",
                        checked     = state.developerMode,
                        onChecked   = viewModel::setDeveloperMode
                    )
                }
            }

            val showInspector = BuildConfig.DEBUG || state.developerMode

            if (showInspector) {
                DevSection("ACCESSIBILITY INSPECTOR") {
                    DevSwitchRow(
                        label       = "Ativar Accessibility Inspector",
                        description = "Captura dumps da árvore de acessibilidade dos apps",
                        checked     = state.inspectorEnabled,
                        onChecked   = viewModel::setInspectorEnabled,
                        highlight   = true
                    )
                }

                if (state.inspectorEnabled) {
                    DevSection("QUANDO GERAR DUMP") {
                        DevSwitchRow(
                            label       = "Dump automático quando parser falhar",
                            description = "Salva árvore completa quando nenhum campo for extraído",
                            checked     = state.autoDumpOnFailure,
                            onChecked   = viewModel::setAutoDumpOnFailure
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label       = "Dump em toda oferta detectada",
                            description = "Salva também quando parse funciona (útil para comparação)",
                            checked     = state.dumpOnRideDetected,
                            onChecked   = viewModel::setDumpOnRideDetected
                        )
                    }

                    DevSection("O QUE SALVAR") {
                        DevSwitchRow(
                            label       = "Salvar extras completos",
                            description = "Inclui Bundle.extras de cada nó (pode ser grande)",
                            checked     = state.saveFullExtras,
                            onChecked   = viewModel::setSaveFullExtras
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label       = "Salvar árvore completa",
                            description = "JSON com hierarquia de nós (recomendado)",
                            checked     = state.saveFullTree,
                            onChecked   = viewModel::setSaveFullTree
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label       = "Manter histórico de dumps",
                            description = "Acumula dumps para comparação de versões",
                            checked     = state.keepHistory,
                            onChecked   = viewModel::setKeepHistory
                        )
                    }

                    DevSection("DUMP EM TEMPO REAL (por app)") {
                        Text(
                            "Captura um dump a cada evento de acessibilidade dos apps selecionados, " +
                            "sem aguardar parse falhar. Útil para reverse-engineering.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label     = "Uber Driver",
                            description = "Dump em todos os eventos do Uber",
                            checked   = state.dumpAllEventsUber,
                            onChecked = viewModel::setDumpAllEventsUber
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label     = "99 Motorista",
                            description = "Dump em todos os eventos do 99",
                            checked   = state.dumpAllEvents99,
                            onChecked = viewModel::setDumpAllEvents99
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label     = "inDrive",
                            description = "Dump em todos os eventos do inDrive",
                            checked   = state.dumpAllEventsInDrive,
                            onChecked = viewModel::setDumpAllEventsInDrive
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevSwitchRow(
                            label     = "iFood Entregador",
                            description = "Dump em todos os eventos do iFood",
                            checked   = state.dumpAllEventsIFood,
                            onChecked = viewModel::setDumpAllEventsIFood
                        )
                    }

                    DevSection("RATE LIMIT") {
                        DevStepperRow(
                            label       = "Intervalo mínimo entre dumps",
                            description = "Minutos",
                            value       = state.rateLimitMinutes,
                            onDecrement = { viewModel.setRateLimitMinutes(state.rateLimitMinutes - 1) },
                            onIncrement = { viewModel.setRateLimitMinutes(state.rateLimitMinutes + 1) },
                            minValue    = 0,
                            maxValue    = 60
                        )
                        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                        DevStepperRow(
                            label       = "Máximo de dumps por dia",
                            description = "Número máximo",
                            value       = state.rateLimitMaxPerDay,
                            onDecrement = { viewModel.setRateLimitMaxPerDay(state.rateLimitMaxPerDay - 10) },
                            onIncrement = { viewModel.setRateLimitMaxPerDay(state.rateLimitMaxPerDay + 10) },
                            minValue    = 10,
                            maxValue    = 500
                        )
                    }

                    DevSection("INSPECTOR") {
                        DevNavRow(
                            label       = "Abrir Inspector",
                            description = "Visualizar, comparar e exportar dumps",
                            onClick     = onNavigateInspector
                        )
                    }
                }

                DevSection("BUILD INFO") {
                    BuildInfoRow("Variante", if (BuildConfig.DEBUG) "DEBUG" else "RELEASE")
                    BuildInfoRow("Versão", "${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                    BuildInfoRow("App ID", BuildConfig.APPLICATION_ID)
                    BuildInfoRow("API", BuildConfig.API_BASE_URL)
                }
            } else {
                Text(
                    "Ative o Modo Desenvolvedor para acessar as opções de diagnóstico.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}

@Composable
private fun DevSection(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            title,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
            modifier = Modifier.padding(bottom = 4.dp)
        )
        Card(
            shape  = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) { content() }
        }
    }
}

@Composable
private fun DevSwitchRow(
    label: String,
    description: String,
    checked: Boolean,
    onChecked: (Boolean) -> Unit,
    highlight: Boolean = false
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                label,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (highlight) FontWeight.SemiBold else FontWeight.Normal,
                color = if (highlight && checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
        Switch(checked = checked, onCheckedChange = onChecked)
    }
}

@Composable
private fun DevNavRow(label: String, description: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        IconButton(onClick = onClick) {
            Icon(Icons.Default.ChevronRight, contentDescription = null,
                tint = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun DevStepperRow(
    label: String,
    description: String,
    value: Int,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    minValue: Int = 0,
    maxValue: Int = Int.MAX_VALUE
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(description, style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            FilledTonalIconButton(
                onClick  = onDecrement,
                enabled  = value > minValue
            ) { Icon(Icons.Default.Remove, contentDescription = "Diminuir") }
            Text(
                value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
            FilledTonalIconButton(
                onClick  = onIncrement,
                enabled  = value < maxValue
            ) { Icon(Icons.Default.Add, contentDescription = "Aumentar") }
        }
    }
}

@Composable
private fun BuildInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        Text(value, style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium)
    }
}
