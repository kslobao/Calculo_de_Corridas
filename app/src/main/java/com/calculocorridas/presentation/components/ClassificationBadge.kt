package com.calculocorridas.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.presentation.theme.GreenExcellent
import com.calculocorridas.presentation.theme.RedPoor
import com.calculocorridas.presentation.theme.YellowGood

@Composable
fun ClassificationBadge(classification: RideClassification, modifier: Modifier = Modifier) {
    val (label, color) = when (classification) {
        RideClassification.EXCELLENT -> "EXCELENTE" to GreenExcellent
        RideClassification.GOOD      -> "BOA"       to YellowGood
        RideClassification.POOR      -> "RUIM"      to RedPoor
    }
    Text(
        text = label,
        color = Color.Black,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .background(color, RoundedCornerShape(4.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp)
    )
}
