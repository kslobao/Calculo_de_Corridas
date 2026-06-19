package com.calculocorridas.data.network.dto

import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.domain.entities.SelectorConfig
import com.calculocorridas.domain.entities.SelectorPattern
import com.calculocorridas.domain.entities.SelectorType
import com.google.gson.annotations.SerializedName

data class SelectorConfigDto(
    @SerializedName("version") val version: Int,
    @SerializedName("updated_at") val updatedAt: String,
    @SerializedName("apps") val apps: Map<String, AppSelectorsDto>
) {
    fun toDomain() = SelectorConfig(
        version = version,
        updatedAt = updatedAt,
        apps = apps.mapValues { it.value.toDomain() }
    )
}

data class AppSelectorsDto(
    @SerializedName("price_patterns") val pricePatterns: List<SelectorPatternDto> = emptyList(),
    @SerializedName("distance_patterns") val distancePatterns: List<SelectorPatternDto> = emptyList(),
    @SerializedName("time_patterns") val timePatterns: List<SelectorPatternDto> = emptyList(),
    @SerializedName("origin_patterns") val originPatterns: List<SelectorPatternDto> = emptyList(),
    @SerializedName("destination_patterns") val destinationPatterns: List<SelectorPatternDto> = emptyList(),
    @SerializedName("category_patterns") val categoryPatterns: List<SelectorPatternDto> = emptyList()
) {
    fun toDomain() = AppSelectors(
        pricePatterns = pricePatterns.map { it.toDomain() },
        distancePatterns = distancePatterns.map { it.toDomain() },
        timePatterns = timePatterns.map { it.toDomain() },
        originPatterns = originPatterns.map { it.toDomain() },
        destinationPatterns = destinationPatterns.map { it.toDomain() },
        categoryPatterns = categoryPatterns.map { it.toDomain() }
    )
}

data class SelectorPatternDto(
    @SerializedName("type") val type: String,
    @SerializedName("value") val value: String,
    @SerializedName("priority") val priority: Int = 0
) {
    fun toDomain() = SelectorPattern(
        type = SelectorType.fromKey(type),
        value = value,
        priority = priority
    )
}
