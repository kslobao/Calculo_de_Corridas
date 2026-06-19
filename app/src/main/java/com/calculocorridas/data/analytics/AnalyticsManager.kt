package com.calculocorridas.data.analytics

import android.os.Bundle
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.engine.RideClassification
import com.google.firebase.analytics.FirebaseAnalytics
import com.google.firebase.analytics.ktx.analytics
import com.google.firebase.ktx.Firebase
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AnalyticsManager @Inject constructor() {

    private val analytics: FirebaseAnalytics by lazy { Firebase.analytics }

    fun logRideDetected(source: AppSource, value: Double, distanceKm: Double, classification: RideClassification) {
        analytics.logEvent("ride_detected", Bundle().apply {
            putString("app_source", source.key)
            putDouble("value", value)
            putDouble("distance_km", distanceKm)
            putString("classification", classification.key)
        })
    }

    fun logOverlayOpened(trigger: String) {
        analytics.logEvent("overlay_opened", Bundle().apply {
            putString("trigger", trigger)
        })
    }

    fun logOverlayClosed(durationSeconds: Long) {
        analytics.logEvent("overlay_closed", Bundle().apply {
            putLong("duration_seconds", durationSeconds)
        })
    }

    fun logSubscriptionStarted(plan: String, price: String) {
        analytics.logEvent("subscription_started", Bundle().apply {
            putString("plan", plan)
            putString("price", price)
        })
    }

    fun logSubscriptionPurchased(plan: String) {
        analytics.logEvent("subscription_purchased", Bundle().apply {
            putString("plan", plan)
        })
    }

    fun logSubscriptionRestored(plan: String) {
        analytics.logEvent("subscription_restored", Bundle().apply {
            putString("plan", plan)
        })
    }

    fun logAdShown(format: String) {
        analytics.logEvent("ad_shown", Bundle().apply {
            putString("format", format)
        })
    }

    fun logRuleTriggered(ruleId: Long, classification: RideClassification) {
        analytics.logEvent("rule_triggered", Bundle().apply {
            putLong("rule_id", ruleId)
            putString("classification", classification.key)
        })
    }

    fun logSelectorSync(version: Int, success: Boolean) {
        analytics.logEvent("selector_sync", Bundle().apply {
            putInt("version", version)
            putBoolean("success", success)
        })
    }

    fun logParserFailed(appSource: String, patternType: String) {
        analytics.logEvent("parser_failed", Bundle().apply {
            putString("app_source", appSource)
            putString("pattern_type", patternType)
        })
    }
}
