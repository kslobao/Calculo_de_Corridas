package com.calculocorridas.presentation.components

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.presentation.theme.GreenExcellent
import com.calculocorridas.presentation.theme.RedPoor
import com.calculocorridas.presentation.theme.YellowGood
import com.calculocorridas.utils.toCurrency
import com.calculocorridas.utils.toKmString
import com.calculocorridas.utils.toTimeString
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun RideCard(ride: Ride, modifier: Modifier = Modifier) {
    val borderColor = when (ride.classification) {
        RideClassification.EXCELLENT -> GreenExcellent
        RideClassification.GOOD      -> YellowGood
        RideClassification.POOR      -> RedPoor
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .border(1.dp, borderColor, RoundedCornerShape(12.dp)),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    ClassificationBadge(ride.classification)
                    Text(
                        text = ride.appSource.displayName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Text(
                    text = SimpleDateFormat("HH:mm", Locale("pt", "BR")).format(Date(ride.createdAt)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }

            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column {
                    Text(ride.rawValue.toCurrency(), style = MaterialTheme.typography.titleLarge)
                    Text(
                        "${ride.distanceKm.toKmString()} • ${ride.durationMin.toTimeString()}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("R$/km: ${ride.valuePerKm.toCurrency()}", style = MaterialTheme.typography.bodyMedium)
                    Text("Lucro: ${ride.netProfit.toCurrency()}", style = MaterialTheme.typography.bodyMedium)
                }
            }

            if (!ride.origin.isNullOrBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "De: ${ride.origin}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    maxLines = 1
                )
            }
        }
    }
}
