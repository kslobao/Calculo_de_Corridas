package com.calculocorridas.utils

import java.util.regex.Pattern

object DistanceParser {
    private val KM_PATTERN  = Pattern.compile("""([\d.,]+)\s*km""", Pattern.CASE_INSENSITIVE)
    private val MT_PATTERN  = Pattern.compile("""([\d.,]+)\s*m\b""", Pattern.CASE_INSENSITIVE)

    fun parse(text: String): Double? {
        KM_PATTERN.matcher(text).let { m ->
            if (m.find()) return normalizeDouble(m.group(1))
        }
        MT_PATTERN.matcher(text).let { m ->
            if (m.find()) {
                val meters = normalizeDouble(m.group(1)) ?: return null
                return meters / 1000.0
            }
        }
        return null
    }

    private fun normalizeDouble(raw: String?): Double? = raw?.let {
        runCatching { it.replace(",", ".").toDouble() }.getOrNull()
    }
}
