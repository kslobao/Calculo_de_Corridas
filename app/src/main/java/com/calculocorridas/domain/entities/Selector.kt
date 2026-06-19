package com.calculocorridas.domain.entities

data class SelectorConfig(
    val version: Int,
    val updatedAt: String,
    val apps: Map<String, AppSelectors>
)

data class AppSelectors(
    val pricePatterns: List<SelectorPattern>,
    val distancePatterns: List<SelectorPattern>,
    val timePatterns: List<SelectorPattern>,
    val originPatterns: List<SelectorPattern>,
    val destinationPatterns: List<SelectorPattern>,
    val categoryPatterns: List<SelectorPattern>
)

data class SelectorPattern(
    val type: SelectorType,
    val value: String,
    val priority: Int = 0
)

enum class SelectorType(val key: String) {
    ACCESSIBILITY_ID("accessibility_id"),
    REGEX("regex"),
    CONTENT_DESC("content_desc"),
    CLASS_NAME("class_name");

    companion object {
        fun fromKey(key: String): SelectorType =
            entries.find { it.key == key } ?: REGEX
    }
}
