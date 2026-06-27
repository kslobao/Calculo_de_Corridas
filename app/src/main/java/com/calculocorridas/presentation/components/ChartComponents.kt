package com.calculocorridas.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.calculocorridas.domain.entities.DayEarning
import com.calculocorridas.presentation.theme.PrimaryLight

@Composable
fun EarningsBarChart(
    earnings: List<DayEarning>,
    modifier: Modifier = Modifier
) {
    if (earnings.isEmpty()) return

    val maxValue = earnings.maxOf { it.totalValue }.toFloat().coerceAtLeast(1f)
    val barShape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        earnings.forEach { day ->
            val heightFraction = (day.totalValue.toFloat() / maxValue).coerceIn(0.02f, 1f)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(heightFraction)
                    .clip(barShape)
                    .background(PrimaryLight)
            )
        }
    }
}
