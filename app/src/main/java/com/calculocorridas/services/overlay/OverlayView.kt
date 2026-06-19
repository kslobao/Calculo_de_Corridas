package com.calculocorridas.services.overlay

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.utils.toCurrency
import com.calculocorridas.utils.toKmString
import com.calculocorridas.utils.toTimeString

data class OverlayData(
    val appSource: String,
    val value: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val valuePerKm: Double,
    val valuePerHour: Double,
    val netProfit: Double,
    val classification: String
)

@SuppressLint("ViewConstructor", "ClickableViewAccessibility")
class OverlayView(context: Context) : LinearLayout(context) {

    var onDismiss: (() -> Unit)? = null
    var onDrag: ((Float, Float) -> Unit)? = null

    private val tvApp:          TextView
    private val tvClassification: TextView
    private val tvValue:        TextView
    private val tvDistance:     TextView
    private val tvDuration:     TextView
    private val tvValuePerKm:   TextView
    private val tvValuePerHour: TextView
    private val tvProfit:       TextView
    private val headerBar:      View

    private var lastTouchX = 0f
    private var lastTouchY = 0f
    private var isDragging = false

    init {
        orientation = VERTICAL
        setPadding(24, 16, 24, 20)
        setBackgroundColor(Color.parseColor("#CC1A1A2E"))
        elevation = 12f

        headerBar = View(context).apply {
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, 4).also {
                it.bottomMargin = 12
            }
            setBackgroundColor(Color.GRAY)
        }
        addView(headerBar)

        tvApp = addLabel("", 12f, Color.parseColor("#AAAAAA"))
        tvClassification = addLabel("", 16f, Color.WHITE).apply {
            setPadding(0, 4, 0, 8)
        }
        tvValue        = addMetricRow("Valor:", "")
        tvDistance     = addMetricRow("KM:", "")
        tvDuration     = addMetricRow("Tempo:", "")
        tvValuePerKm   = addMetricRow("R$/KM:", "")
        tvValuePerHour = addMetricRow("R$/Hora:", "")
        tvProfit       = addMetricRow("Lucro:", "")

        setupDragAndDismiss()
    }

    private fun addLabel(text: String, sizeSp: Float, color: Int): TextView {
        val tv = TextView(context).apply {
            this.text = text
            textSize = sizeSp
            setTextColor(color)
            layoutParams = LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT)
        }
        addView(tv)
        return tv
    }

    private fun addMetricRow(label: String, value: String): TextView {
        val row = LinearLayout(context).apply {
            orientation = HORIZONTAL
            layoutParams = LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).also {
                it.topMargin = 4
            }
        }
        val labelTv = TextView(context).apply {
            text = label
            textSize = 13f
            setTextColor(Color.parseColor("#AAAAAA"))
            layoutParams = LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f)
        }
        val valueTv = TextView(context).apply {
            text = value
            textSize = 13f
            setTextColor(Color.WHITE)
        }
        row.addView(labelTv)
        row.addView(valueTv)
        addView(row)
        return valueTv
    }

    fun update(data: OverlayData) {
        tvApp.text = data.appSource.uppercase()

        val (label, color) = when (RideClassification.fromKey(data.classification)) {
            RideClassification.EXCELLENT -> "EXCELENTE" to Color.parseColor("#4CAF50")
            RideClassification.GOOD      -> "BOA"       to Color.parseColor("#FFC107")
            RideClassification.POOR      -> "RUIM"      to Color.parseColor("#F44336")
        }
        tvClassification.text = label
        tvClassification.setTextColor(color)
        headerBar.setBackgroundColor(color)

        tvValue.text        = data.value.toCurrency()
        tvDistance.text     = data.distanceKm.toKmString()
        tvDuration.text     = data.durationMin.toTimeString()
        tvValuePerKm.text   = data.valuePerKm.toCurrency()
        tvValuePerHour.text = data.valuePerHour.toCurrency()
        tvProfit.text       = data.netProfit.toCurrency()
        tvProfit.setTextColor(if (data.netProfit >= 0) Color.parseColor("#4CAF50") else Color.parseColor("#F44336"))
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupDragAndDismiss() {
        setOnLongClickListener {
            onDismiss?.invoke()
            true
        }

        setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    isDragging = false
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val dx = event.rawX - lastTouchX
                    val dy = event.rawY - lastTouchY
                    if (Math.abs(dx) > 8 || Math.abs(dy) > 8) {
                        isDragging = true
                        onDrag?.invoke(dx, dy)
                    }
                    lastTouchX = event.rawX
                    lastTouchY = event.rawY
                    true
                }
                else -> false
            }
        }
    }
}
