package com.calculocorridas.services.accessibility.parsers

import android.view.accessibility.AccessibilityNodeInfo
import com.calculocorridas.domain.entities.AppSelectors
import com.calculocorridas.selectors.PatternMatcher
import com.calculocorridas.utils.CurrencyParser
import com.calculocorridas.utils.DistanceParser
import com.calculocorridas.utils.TimeParser

data class ParsedRide(
    val value: Double,
    val distanceKm: Double,
    val durationMin: Double,
    val origin: String?,
    val destination: String?,
    val category: String?
)

abstract class BaseParser(
    protected val matcher: PatternMatcher
) {
    abstract val appKey: String

    fun parseFromTexts(texts: List<String>, selectors: AppSelectors): ParsedRide? {
        val valueText = matcher.matchFromTexts(texts, selectors.pricePatterns)
        val distText  = matcher.matchFromTexts(texts, selectors.distancePatterns)
        val timeText  = matcher.matchFromTexts(texts, selectors.timePatterns)

        val value    = valueText?.let { CurrencyParser.parse(it) } ?: return null
        val distance = distText?.let { DistanceParser.parse(it) } ?: return null
        val duration = timeText?.let { TimeParser.parseMinutes(it) } ?: return null

        if (value <= 0 || distance <= 0 || duration <= 0) return null

        return ParsedRide(value, distance, duration, origin = null, destination = null, category = null)
    }

    fun parse(root: AccessibilityNodeInfo, selectors: AppSelectors): ParsedRide? {
        val valueText    = matcher.match(root, selectors.pricePatterns)
        val distText     = matcher.match(root, selectors.distancePatterns)
        val timeText     = matcher.match(root, selectors.timePatterns)
        val origin       = matcher.match(root, selectors.originPatterns)
        val destination  = matcher.match(root, selectors.destinationPatterns)
        val category     = matcher.match(root, selectors.categoryPatterns)

        val value    = valueText?.let { CurrencyParser.parse(it) } ?: return null
        val distance = distText?.let { DistanceParser.parse(it) } ?: return null
        val duration = timeText?.let { TimeParser.parseMinutes(it) } ?: return null

        if (value <= 0 || distance <= 0 || duration <= 0) return null

        return ParsedRide(
            value = value,
            distanceKm = distance,
            durationMin = duration,
            origin = origin,
            destination = destination,
            category = category
        )
    }
}
