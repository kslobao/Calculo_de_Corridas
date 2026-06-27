package com.calculocorridas.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.annotation.RequiresApi
import com.calculocorridas.data.analytics.AnalyticsManager
import com.calculocorridas.data.datastore.UserPreferences
import com.calculocorridas.data.datastore.VehiclePreferences
import com.calculocorridas.domain.engine.RideClassification
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.engine.RideCalculationEngine
import com.calculocorridas.domain.engine.RuleEngine
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.repositories.RuleRepository
import com.calculocorridas.ocr.OcrManager
import com.calculocorridas.ocr.cropSafe
import com.calculocorridas.services.accessibility.parsers.ParsedRide
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
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume

private const val TAG = "CalcCorridas"

@AndroidEntryPoint
class RideAccessibilityService : AccessibilityService() {

    @Inject lateinit var calculationEngine: RideCalculationEngine
    @Inject lateinit var ruleEngine: RuleEngine
    @Inject lateinit var rideRepository: RideRepository
    @Inject lateinit var ruleRepository: RuleRepository
    @Inject lateinit var vehiclePreferences: VehiclePreferences
    @Inject lateinit var userPreferences: UserPreferences
    @Inject lateinit var analyticsManager: AnalyticsManager
    @Inject lateinit var ocrManager: OcrManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var ocrJob: Job? = null

    private val monitoredPackages = setOf(
        AppSource.UBER.packageName,
        AppSource.NINETY_NINE.packageName,
        AppSource.INDRIVE.packageName,
        AppSource.IFOOD.packageName
    )

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AccessibilityService conectado (modo OCR). Monitorando: $monitoredPackages")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        val eventType   = event.eventType

        if (packageName !in monitoredPackages) return

        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) return

        val appSource = AppSource.fromPackage(packageName) ?: return

        when (eventType) {
            AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> {
                // Nova janela/card: cancela qualquer OCR pendente e inicia novo imediatamente
                ocrManager.invalidateHash()
                ocrJob?.cancel()
                ocrJob = scope.launch {
                    delay(200L) // aguarda renderização
                    triggerOcr(appSource, packageName)
                }
                Log.d(TAG, "[$appSource] STATE_CHANGED → ocrJob iniciado (200ms)")
            }
            AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED -> {
                // Conteúdo atualizado: só inicia OCR se não há job ativo (evita cancelar STATE job)
                if (ocrJob?.isActive != true) {
                    ocrJob = scope.launch {
                        delay(400L) // maior delay para amortecer rajadas de CONTENT_CHANGED
                        triggerOcr(appSource, packageName)
                    }
                    Log.d(TAG, "[$appSource] CONTENT_CHANGED → ocrJob fallback (400ms)")
                }
            }
        }
    }

    private suspend fun triggerOcr(appSource: AppSource, packageName: String) {
        val enabled = when (appSource) {
            AppSource.UBER        -> userPreferences.monitorUber.first()
            AppSource.NINETY_NINE -> userPreferences.monitor99.first()
            AppSource.INDRIVE     -> userPreferences.monitorInDrive.first()
            AppSource.IFOOD       -> userPreferences.monitorIFood.first()
        }
        if (!enabled) { Log.d(TAG, "[$appSource] monitoramento desativado"); return }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.w(TAG, "[$appSource] OCR requer Android 11+"); return
        }
        if (!ocrManager.shouldAttemptOcr()) {
            Log.d(TAG, "[$appSource] OCR bloqueado por modo econômico"); return
        }

        Log.d(TAG, "[$appSource] Iniciando OCR...")
        runOcrCapture(appSource, packageName)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun runOcrCapture(appSource: AppSource, packageName: String) {
        val screenshot = takeScreenshotSuspend() ?: return
        val metrics    = resources.displayMetrics
        val cardBounds = ocrManager.detectCardBounds(
            windows ?: emptyList(), packageName, metrics.widthPixels, metrics.heightPixels
        )
        val card      = screenshot.cropSafe(cardBounds, recycleSrc = true)
        val ocrResult = ocrManager.processImage(card)
        card.recycle()
        if (ocrResult != null) {
            Log.i(TAG, "[$appSource] CORRIDA VIA OCR: R$${ocrResult.value} · ${ocrResult.distanceKm}km · ${ocrResult.durationMin}min")
            dispatchRide(appSource, ocrResult)
        }
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private suspend fun takeScreenshotSuspend(): Bitmap? =
        suspendCancellableCoroutine { cont ->
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                mainExecutor,
                object : TakeScreenshotCallback {
                    override fun onSuccess(result: ScreenshotResult) {
                        val buffer = result.hardwareBuffer
                        val hw = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
                        buffer.close()
                        val soft = hw?.copy(Bitmap.Config.ARGB_8888, false)
                        hw?.recycle()
                        cont.resume(soft)
                    }
                    override fun onFailure(errorCode: Int) {
                        Log.w(TAG, "takeScreenshot falhou: errorCode=$errorCode")
                        cont.resume(null)
                    }
                }
            )
        }

    private suspend fun dispatchRide(source: AppSource, parsed: ParsedRide) {
        val vehicle = vehiclePreferences.activeVehicle.first()
        val metrics = calculationEngine.calculate(
            rawValue    = parsed.value,
            distanceKm  = parsed.distanceKm,
            durationMin = parsed.durationMin,
            vehicle     = vehicle
        )

        Log.i(TAG, "Métricas: R$/km=${metrics.valuePerKm} R$/h=${metrics.valuePerHour} lucro=${metrics.netProfit}")

        val rules = ruleRepository.getEnabled()
        val ride = Ride(
            appSource      = source,
            rawValue       = parsed.value,
            distanceKm     = parsed.distanceKm,
            durationMin    = parsed.durationMin,
            origin         = parsed.origin,
            destination    = parsed.destination,
            category       = parsed.category,
            valuePerKm     = metrics.valuePerKm,
            valuePerHour   = metrics.valuePerHour,
            fuelCost       = metrics.fuelCost,
            netProfit      = metrics.netProfit,
            classification = ruleEngine.evaluate(
                Ride(
                    appSource    = source,             rawValue     = parsed.value,
                    distanceKm   = parsed.distanceKm,  durationMin  = parsed.durationMin,
                    valuePerKm   = metrics.valuePerKm, valuePerHour = metrics.valuePerHour,
                    fuelCost     = metrics.fuelCost,   netProfit    = metrics.netProfit,
                    classification = RideClassification.GOOD
                ),
                rules
            )
        )

        rideRepository.save(ride)
        analyticsManager.logRideDetected(source, parsed.value, parsed.distanceKm, ride.classification)

        val overlayIntent = Intent(this, OverlayService::class.java).apply {
            action = OverlayService.ACTION_SHOW_RIDE
            putExtra(OverlayService.EXTRA_RIDE_VALUE,     parsed.value)
            putExtra(OverlayService.EXTRA_RIDE_DISTANCE,  parsed.distanceKm)
            putExtra(OverlayService.EXTRA_RIDE_DURATION,  parsed.durationMin)
            putExtra(OverlayService.EXTRA_VALUE_PER_KM,   metrics.valuePerKm)
            putExtra(OverlayService.EXTRA_VALUE_PER_HOUR, metrics.valuePerHour)
            putExtra(OverlayService.EXTRA_NET_PROFIT,     metrics.netProfit)
            putExtra(OverlayService.EXTRA_CLASSIFICATION, ride.classification.key)
            putExtra(OverlayService.EXTRA_APP_SOURCE,     source.displayName)
        }
        Log.i(TAG, "Iniciando OverlayService com classification=${ride.classification.key}")
        startForegroundService(overlayIntent)
    }

    override fun onInterrupt() { scope.cancel() }

    override fun onDestroy() {
        super.onDestroy()
        scope.cancel()
        ocrManager.destroy()
    }
}
