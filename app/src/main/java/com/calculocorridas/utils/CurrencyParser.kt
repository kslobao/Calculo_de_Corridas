package com.calculocorridas.utils

import java.util.regex.Pattern

object CurrencyParser {
    private val PATTERNS = listOf(
        Pattern.compile("""R\$\s*([\d.,]+)"""),
        Pattern.compile("""([\d]+[.,][\d]{2})""")
    )

    fun parse(text: String): Double? {
        for (pattern in PATTERNS) {
            val matcher = pattern.matcher(text)
            if (matcher.find()) {
                return normalize(matcher.group(1) ?: continue)
            }
        }
        return null
    }

    private fun normalize(raw: String): Double? = runCatching {
        val cleaned = raw.trim()
        when {
            cleaned.contains(",") && cleaned.contains(".") -> {
                if (cleaned.indexOf(".") < cleaned.indexOf(",")) {
                    cleaned.replace(".", "").replace(",", ".").toDouble()
                } else {
                    cleaned.replace(",", "").toDouble()
                }
            }
            cleaned.contains(",") -> cleaned.replace(",", ".").toDouble()
            else -> cleaned.toDouble()
        }
    }.getOrNull()
}
