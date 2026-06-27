package com.calculocorridas.presentation.screens.home

import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.calculocorridas.presentation.components.RideCard
import com.calculocorridas.presentation.theme.GreenExcellent
import com.calculocorridas.presentation.theme.RedPoor
import com.calculocorridas.services.accessibility.RideAccessibilityService
import com.calculocorridas.utils.toCurrency

private fun isAccessibilityServiceEnabled(context: Context): Boolean {
    val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
    return am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
        .any { it.resolveInfo.serviceInfo.packageName == context.packageName }
}

private fun canDrawOverlays(context: Context) = Settings.canDrawOverlays(context)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = hiltViewModel(),
    onNavigateHistory: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showAccessibilityDialog by remember { mutableStateOf(false) }
    var overlayPermissionGranted by remember { mutableStateOf(canDrawOverlays(context)) }

    // Verifica estado real toda vez que o usuário volta ao app
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.setServiceActive(isAccessibilityServiceEnabled(context))
                overlayPermissionGranted = canDrawOverlays(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showAccessibilityDialog) {
        AccessibilityInstructionDialog(
            onConfirm = {
                showAccessibilityDialog = false
                // Tenta abrir direto nas configurações do serviço (funciona na maioria dos aparelhos)
                val componentName = "${context.packageName}/${RideAccessibilityService::class.java.name}"
                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    putExtra(":settings:fragment_args_key", componentName)
                    putExtra(":settings:show_fragment_args", Bundle().also {
                        it.putString(":settings:fragment_args_key", componentName)
                    })
                }
                context.startActivity(intent)
            },
            onDismiss = { showAccessibilityDialog = false }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Cálculo de Corridas", fontWeight = FontWeight.Bold) },
                actions = {
                    if (state.isPro) {
                        Box(
                            modifier = Modifier
                                .padding(end = 16.dp)
                                .background(MaterialTheme.colorScheme.secondary, RoundedCornerShape(4.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) { Text("PRO", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold) }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
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
            ServiceStatusCard(
                isActive = state.isServiceActive,
                onActivate = { showAccessibilityDialog = true },
                onDeactivate = {
                    context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    })
                }
            )

            if (!overlayPermissionGranted) {
                OverlayPermissionCard(
                    onGrant = {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:${context.packageName}")
                            )
                        )
                    }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                QuickMetricCard("Hoje", state.todayEarnings.toCurrency(), Modifier.weight(1f))
                QuickMetricCard("R$/km", state.avgValuePerKm.toCurrency(), Modifier.weight(1f))
                QuickMetricCard("Corridas", state.totalRidesToday.toString(), Modifier.weight(1f))
            }

            state.lastRide?.let { ride ->
                Text("ÚLTIMA DETECÇÃO", style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f))
                RideCard(ride = ride)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { /* toggle overlay */ },
                    modifier = Modifier.weight(1f)
                ) { Text("Overlay") }

                Button(
                    onClick = onNavigateHistory,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) { Text("Histórico") }
            }
        }
    }
}

@Composable
private fun AccessibilityInstructionDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Ativar acessibilidade", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Na próxima tela, siga os passos:")
                Spacer(Modifier.height(4.dp))
                InstructionStep("1", "Encontre \"Cálculo de Corridas\" na lista")
                InstructionStep("2", "Toque no nome do serviço")
                InstructionStep("3", "Ative o interruptor")
                InstructionStep("4", "Confirme tocando em \"OK\"")
                Spacer(Modifier.height(4.dp))
                Text(
                    "Essa permissão é necessária para detectar as ofertas de corrida.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) { Text("Ir para Configurações") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}

@Composable
private fun InstructionStep(number: String, text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimary)
        }
        Text(text, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
private fun ServiceStatusCard(
    isActive: Boolean,
    onActivate: () -> Unit,
    onDeactivate: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(if (isActive) GreenExcellent else RedPoor)
                    )
                    Text(
                        if (isActive) "SERVIÇO ATIVO" else "SERVIÇO INATIVO",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
                Text(
                    if (isActive) "Monitorando: Uber, 99, inDrive, iFood"
                    else "Toque para ativar a acessibilidade",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            if (isActive) {
                Button(
                    onClick = onDeactivate,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.errorContainer,
                        contentColor = MaterialTheme.colorScheme.onErrorContainer
                    )
                ) { Text("Desativar") }
            } else {
                Button(onClick = onActivate) { Text("Ativar") }
            }
        }
    }
}

@Composable
private fun OverlayPermissionCard(onGrant: () -> Unit) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "Permissão de overlay necessária",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "Permite exibir as métricas sobre o app de corridas",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            Spacer(Modifier.size(8.dp))
            Button(onClick = onGrant) { Text("Permitir") }
        }
    }
}

@Composable
private fun QuickMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text(label, style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}
