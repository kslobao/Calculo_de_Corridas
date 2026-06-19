package com.calculocorridas.presentation.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.calculocorridas.domain.entities.DayEarning
import com.calculocorridas.presentation.theme.PrimaryLight
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottomAxis
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStartAxis
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.common.shape.Shape

@Composable
fun EarningsBarChart(
    earnings: List<DayEarning>,
    modifier: Modifier = Modifier
) {
    if (earnings.isEmpty()) return

    val modelProducer = remember { CartesianChartModelProducer() }
    val values = earnings.map { it.totalValue.toFloat() }

    remember(earnings) {
        modelProducer.tryRunTransaction {
            columnSeries { series(values) }
        }
    }

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberColumnCartesianLayer(
                columnProvider = ColumnCartesianLayer.ColumnProvider.series(
                    ColumnCartesianLayer.Column(
                        fill = fill(PrimaryLight),
                        shape = Shape.rounded(topLeftPercent = 4, topRightPercent = 4)
                    )
                )
            ),
            startAxis = rememberStartAxis(),
            bottomAxis = rememberBottomAxis()
        ),
        modelProducer = modelProducer,
        modifier = modifier
            .fillMaxWidth()
            .height(160.dp)
    )
}
