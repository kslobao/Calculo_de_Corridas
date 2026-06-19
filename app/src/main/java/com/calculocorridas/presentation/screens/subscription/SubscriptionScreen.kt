package com.calculocorridas.presentation.screens.subscription

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.calculocorridas.data.billing.ProductIds
import com.calculocorridas.data.billing.SubscriptionState
import com.calculocorridas.presentation.theme.GreenExcellent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = hiltViewModel(),
    onNavigateBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Assinar PRO", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.surface)
            )
        }
    ) { padding ->
        if (state.billingState is SubscriptionState.Subscribed) {
            Column(
                Modifier.fillMaxSize().padding(padding),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(Icons.Default.CheckCircle, null, tint = GreenExcellent, modifier = Modifier.size(64.dp))
                Spacer(Modifier.height(16.dp))
                Text("Você já é PRO!", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                TextButton(onClick = onNavigateBack) { Text("Voltar") }
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("⭐ CALCULO PRO", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)

            val benefits = listOf(
                "Sem anúncios",
                "Histórico ilimitado",
                "Backup em nuvem",
                "Exportação CSV/Excel",
                "Múltiplos veículos",
                "Estatísticas avançadas",
                "Regras ilimitadas",
                "Atualizações prioritárias"
            )
            benefits.forEach { benefit ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("✓", color = GreenExcellent, fontWeight = FontWeight.Bold)
                    Text(benefit, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(8.dp))

            PlanOption(
                title = "Mensal",
                price = "R$ 9,90/mês",
                selected = state.selectedProductId == ProductIds.MONTHLY_PRO,
                onClick = { viewModel.selectProduct(ProductIds.MONTHLY_PRO) }
            )

            PlanOption(
                title = "Anual",
                price = "R$ 79,90/ano",
                badge = "33% OFF",
                selected = state.selectedProductId == ProductIds.YEARLY_PRO,
                onClick = { viewModel.selectProduct(ProductIds.YEARLY_PRO) }
            )

            Spacer(Modifier.height(8.dp))

            if (state.isVerifying || state.billingState is SubscriptionState.Loading) {
                CircularProgressIndicator()
            } else {
                Button(
                    onClick = { viewModel.purchase(context as android.app.Activity) },
                    modifier = Modifier.fillMaxWidth().height(52.dp)
                ) { Text("ASSINAR AGORA", fontWeight = FontWeight.Bold) }
            }

            TextButton(onClick = { viewModel.restorePurchases() }) {
                Text("Restaurar compra anterior", style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

@Composable
private fun PlanOption(
    title: String,
    price: String,
    badge: String? = null,
    selected: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .border(2.dp, borderColor, RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Column(horizontalAlignment = Alignment.End) {
                Text(price, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                badge?.let {
                    Text(it, style = MaterialTheme.typography.labelSmall, color = GreenExcellent, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
