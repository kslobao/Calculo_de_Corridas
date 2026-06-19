package com.calculocorridas.utils

import android.view.accessibility.AccessibilityNodeInfo
import java.text.NumberFormat
import java.util.Locale

fun Double.toCurrency(): String =
    NumberFormat.getCurrencyInstance(Locale("pt", "BR")).format(this)

fun Double.toKmString(): String = String.format(Locale.US, "%.1f km", this)

fun Double.toTimeString(): String {
    val totalMin = this.toInt()
    val h   = totalMin / 60
    val min = totalMin % 60
    return if (h > 0) "${h}h ${min}min" else "${min}min"
}

fun Double.roundTo(decimals: Int): Double {
    var multiplier = 1.0
    repeat(decimals) { multiplier *= 10 }
    return kotlin.math.round(this * multiplier) / multiplier
}

fun AccessibilityNodeInfo?.findText(): String? {
    this ?: return null
    if (!text.isNullOrBlank()) return text.toString()
    if (!contentDescription.isNullOrBlank()) return contentDescription.toString()
    for (i in 0 until childCount) {
        val child = getChild(i)
        val found = child.findText()
        if (!found.isNullOrBlank()) return found
    }
    return null
}

fun AccessibilityNodeInfo.findAllTexts(): List<String> {
    val results = mutableListOf<String>()
    if (!text.isNullOrBlank()) results.add(text.toString())
    if (!contentDescription.isNullOrBlank()) results.add(contentDescription.toString())
    for (i in 0 until childCount) {
        results.addAll(getChild(i)?.findAllTexts() ?: emptyList())
    }
    return results
}

fun AccessibilityNodeInfo.findNodeById(viewId: String): AccessibilityNodeInfo? {
    val nodes = findAccessibilityNodeInfosByViewId(viewId)
    return nodes?.firstOrNull()
}
