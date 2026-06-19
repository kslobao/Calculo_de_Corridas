package com.calculocorridas.domain.engine

enum class RideClassification(val key: String) {
    EXCELLENT("excellent"),
    GOOD("good"),
    POOR("poor");

    companion object {
        fun fromKey(key: String): RideClassification =
            entries.find { it.key == key } ?: POOR
    }
}
