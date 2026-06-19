package com.calculocorridas.utils

import java.util.regex.Pattern

object TimeParser {
    private val H_M_PATTERN = Pattern.compile("""(\d+)\s*h\s*(\d+)\s*min""", Pattern.CASE_INSENSITIVE)
    private val H_PATTERN   = Pattern.compile("""(\d+)\s*h(?:or[as]?)?""", Pattern.CASE_INSENSITIVE)
    private val MIN_PATTERN = Pattern.compile("""(\d+)\s*min""", Pattern.CASE_INSENSITIVE)

    fun parseMinutes(text: String): Double? {
        H_M_PATTERN.matcher(text).let { m ->
            if (m.find()) {
                val h   = m.group(1)?.toDoubleOrNull() ?: 0.0
                val min = m.group(2)?.toDoubleOrNull() ?: 0.0
                return h * 60 + min
            }
        }
        H_PATTERN.matcher(text).let { m ->
            if (m.find()) return (m.group(1)?.toDoubleOrNull() ?: 0.0) * 60
        }
        MIN_PATTERN.matcher(text).let { m ->
            if (m.find()) return m.group(1)?.toDoubleOrNull()
        }
        return null
    }
}
