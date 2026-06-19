package com.calculocorridas.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.calculocorridas.data.analytics.AnalyticsManager
import com.calculocorridas.data.datastore.UserPreferences
import com.calculocorridas.data.datastore.VehiclePreferences
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.engine.RideCalculationEngine
import com.calculocorridas.domain.engine.RuleEngine
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.repositories.RuleRepository
import com.calculocorridas.selectors.SelectorConfigHolder
import com.calculocorridas.services.accessibility.parsers.BaseParser
import com.calculocorridas.services.accessibility.parsers.IFoodParser
import com.calculocorridas.services.accessibility.parsers.InDriveParser
import com.calculocorridas.services.accessibility.parsers.NinetyNineParser
import com.calculocorridas.services.accessibility.parsers.ParsedRide
import com.calculocorridas.services.accessibility.parsers.UberParser
import com.calculocorridas.services.overlay.OverlayService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class RideAccessibilityService : AccessibilityService() {

    @Inject lateinit var uberParser: UberParser
    @Inject lateinit var ninetyNineParser: NinetyNineParser
    @Inject lateinit var inDriveParser: InDriveParser
    @Inject lateinit var iFoodParser: IFoodParser
    @Inject lateinit var calculationEngine: RideCalculationEngine
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var rideRepository: RideRepository
    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var selectorConfigHolder: SelectorConfigHolder
    @Inject lateinit var vehiclePreferences: VehiclePreferences
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var analyticsManager: AnalyticsManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null

    private val parsers: Map<String, BaseParser> by lazy {
        mapOf(
            AppSource.UBER.packageName       to uberParser,
            AppSource.NINETY_NINE.packageName to ninetyNineParser,
            AppSource.INDRIVE.packageName     to inDriveParser,
            AppSource.IFOOD.packageName       to iFoodParser
        )
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        if (packageName !in parsers.keys) return

        val eventType = event.eventType
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
        ) return

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            val root = rootInActiveWindow ?: return@launch
            processEvent(packageName, root)
        }
    }

    private suspend fun processEvent(packageName: String, root: AccessibilityNodeInfo) {
        val appSource = AppSource.fromPackage(packageName) ?: return
        val parser = parsers[packageName] ?: return
        val selectors = selectorConfigHolder.getFor(appSource.key) ?: return

        val enabled = when (appSource) {
            AppSource.UBER        -> userPreferences.monitorUber.first()
            AppSource.NINETY_NINE -> userPreferences.monitor99.first()
            AppSource.INDRIVE     -> userPreferences.monitorInDrive.first()
            AppSource.IFOOD       -> userPreferences.monitorIFood.first()
        }
        if (!enabled) return

        val parsed = parser.parse(root, selectors) ?: return
        dispatchRide(appSource, parsed)
    }

    private suspend fun dispatchRide(source: AppSource, parsed: ParsedRide) {
        val vehicle = vehiclePreferences.activeVehicle.first()
        val metrics = calculationEngine.calculate(
            rawValue = parsed.value,
            distanceKm = parsed.distanceKm,
            durationMin = parsed.durationMin,
            vehicle = vehicle
        )

        val rules = ruleRepository.getEnabled()
        val ride = Ride(
            appSource    = source,
            rawValue     = parsed.value,
            distanceKm   = parsed.distanceKm,
            durationMin  = parsed.durationMin,
            origin       = parsed.origin,
            destination  = parsed.destination,
            category     = parsed.category,
            valuePerKm   = metrics.valuePerKm,
            valuePerHour = metrics.valuePerHour,
            fuelCost     = metrics.fuelCost,
            netProfit    = metrics.netProfit,
            classification = ruleEngine.evaluate(
                Ride(
                    appSource = source, rawValue = parsed.value,
                    distanceKm = parsed.distanceKm, durationMin = parsed.durationMin,
                    valuePerKm = metrics.valuePerKm, valuePerHour = metrics.valuePerHour,
                    fuelCost = metrics.fuelCost, netProfit = metrics.netProfit,
                    classification = com.calculocorridas.domain.engine.RideClassification.GOOD
                ),
                rules
            )
        )

        rideRepository.save(ride)
        analyticsManager.logRideDetected(source, parsed.value, parsed.distanceKm, ride.classification)

        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_RIDE
            putExtra(OverlayService.EXTRA_RIDE_VALUE,    parsed.value)
            putExtra(OverlayService.EXTRA_RIDE_DISTANCE, parsed.distanceKm)
            putExtra(OverlayService.EXTRA_RIDE_DURATION, parsed.durationMin)
            putExtra(OverlayService.EXTRA_VALUE_PER_KM,  metrics.valuePerKm)
            putExtra(OverlayService.EXTRA_VALUE_PER_HOUR,metrics.valuePerHour)
            putExtra(OverlayService.EXTRA_NET_PROFIT,    metrics.netProfit)
            putExtra(OverlayService.EXTRA_CLASSIFICATION,ride.classification.key)
            putExtra(OverlayService.EXTRA_APP_SOURCE,    source.displayName)
        }
        startService(overlayIntent)
    }

    override fun onInterrupt() { scope.cancel() }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
    }

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}
