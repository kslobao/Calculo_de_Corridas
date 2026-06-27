package com.calculocorridas.services.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.annotation.RequiresApi
import com.calculocorridas.data.analytics.AnalyticsManager
import com.calculocorridas.data.datastore.UserPreferences
import com.calculocorridas.data.datastore.VehiclePreferences
import com.calculocorridas.domain.entities.AppSource
import com.calculocorridas.domain.engine.RideCalculationEngine
import com.calculocorridas.domain.engine.RuleEngine
import com.calculocorridas.domain.entities.Ride
import com.calculocorridas.domain.repositories.RideRepository
import com.calculocorridas.domain.repositories.RuleRepository
import com.calculocorridas.inspector.AccessibilityInspector
import com.calculocorridas.ocr.OcrManager
import com.calculocorridas.ocr.cropSafe
import com.calculocorridas.selectors.SelectorConfigHolder
import com.calculocorridas.services.accessibility.parsers.BaseParser
import com.calculocorridas.services.accessibility.parsers.IFoodParser
import com.calculocorridas.services.accessibility.parsers.InDriveParser
import com.calculocorridas.services.accessibility.parsers.NinetyNineParser
import com.calculocorridas.services.accessibility.parsers.ParsedRide
import com.calculocorridas.services.accessibility.parsers.UberParser
import com.calculocorridas.services.overlay.OverlayService
import com.calculocorridas.utils.countNodes
import com.calculocorridas.utils.dumpTree
import com.calculocorridas.utils.findAllTexts
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
    @Inject lateinit var inspector: AccessibilityInspector
    @Inject lateinit var ocrManager: OcrManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var debounceJob: Job? = null

    // Buffer de textos capturados de TYPE_VIEW_TEXT_CHANGED (antes de acessar a árvore)
    private val textBuffer    = mutableMapOf<String, MutableSet<String>>()
    // Último evento por pacote (capturado antes do recycle)
    private val lastEvent     = mutableMapOf<String, AccessibilityInspector.CapturedEvent>()

    private val parsers: Map<String, BaseParser> by lazy {
        mapOf(
            AppSource.UBER.packageName       to uberParser,
            AppSource.NINETY_NINE.packageName to ninetyNineParser,
            AppSource.INDRIVE.packageName     to inDriveParser,
            AppSource.IFOOD.packageName       to iFoodParser
        )
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.i(TAG, "AccessibilityService conectado. Monitorando: ${parsers.keys}")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val packageName = event.packageName?.toString() ?: return
        val eventType   = event.eventType

        // Notificações: capturar de qualquer pacote — podem ser heads-up do Uber
        if (eventType == AccessibilityEvent.TYPE_NOTIFICATION_STATE_CHANGED) {
            val texts = event.text?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } } ?: emptyList()
            if (texts.isNotEmpty()) Log.d(TAG, "[NOTIF] $packageName: $texts")
            if (packageName == AppSource.UBER.packageName && texts.isNotEmpty()) {
                handleNotificationTexts(texts)
            }
            return
        }

        if (packageName !in parsers.keys) return

        // Nova janela = limpa buffer (contexto mudou)
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            textBuffer.remove(packageName)
            Log.d(TAG, "[$packageName] Nova janela — buffer limpo")
        }

        // Texto de view individual: acumula para tentar parsear depois
        if (eventType == AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) {
            val texts = event.text?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } } ?: emptyList()
            if (texts.isNotEmpty()) {
                val buf = textBuffer.getOrPut(packageName) { mutableSetOf() }
                buf.addAll(texts)
                Log.d(TAG, "[ViewText] $packageName acumulou: $texts (buffer=${buf.size})")
            }
        }

        // Debounce apenas para eventos que indicam mudança visível
        if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
            eventType != AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED &&
            eventType != AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED) return

        // Captura evento ANTES do debounce (será reciclado ao retornar)
        val captured = AccessibilityInspector.CapturedEvent.from(event)
        lastEvent[packageName] = captured

        val directTexts = event.text?.mapNotNull { it?.toString()?.takeIf { s -> s.isNotBlank() } } ?: emptyList()
        Log.d(TAG, "Evento pkg=$packageName tipo=$eventType direto=${directTexts.size}")

        // Dump imediato (sem debounce) quando "dump all events" está ativo para este app
        val appKey = AppSource.fromPackage(packageName)?.key
        if (appKey != null) {
            scope.launch {
                if (inspector.prefs.dumpAllEventsFor(appKey)) {
                    val trigger = "all_events_$appKey"
                    if (inspector.checkAndRecordRateLimit(appKey)) {
                        val allWin = windows ?: emptyList()
                        val buf    = textBuffer[packageName]?.toList() ?: emptyList()
                        val dump   = inspector.capture(appKey, packageName, trigger,
                            captured, allWin, buf, 0, null)
                        inspector.saveDump(dump)
                    }
                }
            }
        }

        debounceJob?.cancel()
        debounceJob = scope.launch {
            delay(DEBOUNCE_MS)
            processPackage(packageName)
        }
    }

    private suspend fun processPackage(packageName: String) {
        val appSource = AppSource.fromPackage(packageName) ?: return
        val parser    = parsers[packageName] ?: return
        val selectors = selectorConfigHolder.getFor(appSource.key)

        val enabled = when (appSource) {
            AppSource.UBER        -> userPreferences.monitorUber.first()
            AppSource.NINETY_NINE -> userPreferences.monitor99.first()
            AppSource.INDRIVE     -> userPreferences.monitorInDrive.first()
            AppSource.IFOOD       -> userPreferences.monitorIFood.first()
        }
        if (!enabled) { Log.d(TAG, "[$appSource] monitoramento desativado"); return }

        // --- Abordagem 1: árvore de acessibilidade (todas as janelas) ---
        val allWindows = windows ?: emptyList()
        Log.d(TAG, "Total janelas: ${allWindows.size}")
        allWindows.forEachIndexed { i, w ->
            val root  = w.root
            val pkg   = root?.packageName?.toString() ?: "null"
            val nodes = root?.countNodes() ?: 0
            val texts = root?.findAllTexts()?.size ?: 0
            Log.d(TAG, "  [$i] pkg=$pkg type=${w.type} nós=$nodes textos=$texts")
        }

        val appRoots = allWindows.mapNotNull { it.root }
            .filter { it.packageName?.toString() == packageName }

        val capturedEvent = lastEvent[packageName]
        val allWindowsForInspector = allWindows

        if (appRoots.isNotEmpty()) {
            val bestRoot = appRoots.maxByOrNull { it.findAllTexts().size } ?: appRoots.first()
            bestRoot.refresh()
            val treeTexts = bestRoot.findAllTexts()
            Log.d(TAG, "[$appSource] Árvore: ${bestRoot.countNodes()} nós, ${treeTexts.size} textos: ${treeTexts.take(15)}")

            if (treeTexts.isEmpty()) {
                Log.w(TAG, "[$appSource] DUMP DA ÁRVORE (sem texto):")
                bestRoot.dumpTree().take(60).forEach { Log.w(TAG, it) }
            }

            val parsed = parser.parse(bestRoot, selectors)
            if (parsed != null) {
                Log.i(TAG, "[$appSource] CORRIDA VIA ÁRVORE: R$${parsed.value} · ${parsed.distanceKm}km · ${parsed.durationMin}min")
                textBuffer.remove(packageName)
                if (inspector.shouldDump("ride_detected")) {
                    val dump = inspector.capture(appSource.key, packageName, "ride_detected",
                        capturedEvent, allWindowsForInspector, emptyList(),
                        0, "R$${parsed.value} ${parsed.distanceKm}km ${parsed.durationMin}min")
                    inspector.saveDump(dump)
                }
                dispatchRide(appSource, parsed)
                return
            }
        } else {
            val fallback = rootInActiveWindow
            if (fallback != null) {
                fallback.refresh()
                val treeTexts = fallback.findAllTexts()
                Log.d(TAG, "[$appSource] Fallback rootInActiveWindow: ${treeTexts.size} textos")
                val parsed = parser.parse(fallback, selectors)
                if (parsed != null) {
                    Log.i(TAG, "[$appSource] CORRIDA VIA FALLBACK: R$${parsed.value}")
                    textBuffer.remove(packageName)
                    dispatchRide(appSource, parsed)
                    return
                }
            }
        }

        // --- Abordagem 2: textos acumulados de TYPE_VIEW_TEXT_CHANGED ---
        val buffered = textBuffer[packageName]?.toList() ?: emptyList()
        Log.d(TAG, "[$appSource] Textos no buffer: ${buffered.size}: $buffered")
        if (buffered.isNotEmpty()) {
            val parsed = parser.parseFromTexts(buffered, selectors)
            if (parsed != null) {
                Log.i(TAG, "[$appSource] CORRIDA VIA BUFFER: R$${parsed.value} · ${parsed.distanceKm}km · ${parsed.durationMin}min")
                textBuffer.remove(packageName)
                if (inspector.shouldDump("ride_detected")) {
                    val dump = inspector.capture(appSource.key, packageName, "ride_detected",
                        capturedEvent, allWindowsForInspector, buffered,
                        0, "R$${parsed.value} ${parsed.distanceKm}km ${parsed.durationMin}min")
                    inspector.saveDump(dump)
                }
                dispatchRide(appSource, parsed)
                return
            }
            Log.w(TAG, "[$appSource] Buffer com ${buffered.size} textos mas parse falhou")
        }

        // --- Abordagem 3 (path 4): OCR local via ML Kit — apenas Uber, API 30+ ---
        if (appSource == AppSource.UBER && Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (ocrManager.shouldAttemptOcr()) {
                Log.d(TAG, "[$appSource] Tentando OCR (path 4) — aguardando 500ms...")
                delay(500L) // debounce 300ms + 500ms = ~800ms total
                val screenshot = takeScreenshotSuspend()
                if (screenshot != null) {
                    val metrics = resources.displayMetrics
                    val freshWindows = windows ?: emptyList()
                    val cardBounds = ocrManager.detectCardBounds(
                        freshWindows, metrics.widthPixels, metrics.heightPixels
                    )
                    val card = screenshot.cropSafe(cardBounds, recycleSrc = true)
                    val ocrResult = ocrManager.processImage(card)
                    card.recycle()
                    if (ocrResult != null) {
                        Log.i(TAG, "[$appSource] CORRIDA VIA OCR: R$${ocrResult.value} · ${ocrResult.distanceKm}km · ${ocrResult.durationMin}min")
                        textBuffer.remove(packageName)
                        dispatchRide(appSource, ocrResult)
                        return
                    }
                }
            } else {
                Log.d(TAG, "[$appSource] OCR bloqueado por rate limit ou modo econômico")
            }
        }

        // --- Parse falhou em todas as abordagens: dump de diagnóstico ---
        Log.w(TAG, "[$appSource] Nenhuma abordagem funcionou — aguardando próximo evento")
        if (inspector.shouldDump("parse_failed")) {
            val dump = inspector.capture(appSource.key, packageName, "parse_failed",
                capturedEvent, allWindowsForInspector, buffered, 0, null)
            inspector.saveDump(dump)
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

    private fun handleNotificationTexts(texts: List<String>) {
        scope.launch {
            Log.i(TAG, "[NOTIF-UBER] ${texts.joinToString(" | ")}")
            if (!texts.any { it.contains(Regex("""R\$\s*[\d.,]+""")) }) return@launch
            val enabled = userPreferences.monitorUber.first()
            if (!enabled) return@launch
            val selectors = selectorConfigHolder.getFor("uber")
            val parsed = uberParser.parseFromTexts(texts, selectors)
            if (parsed != null) {
                Log.i(TAG, "[NOTIF-UBER] CORRIDA DETECTADA: R$${parsed.value} · ${parsed.distanceKm}km")
                dispatchRide(AppSource.UBER, parsed)
            } else {
                Log.w(TAG, "[NOTIF-UBER] Notificação com R$ mas parse falhou — textos: $texts")
            }
        }
    }

    private suspend fun processEvent(packageName: String, root: AccessibilityNodeInfo) {
        val appSource = AppSource.fromPackage(packageName) ?: return
        val parser    = parsers[packageName] ?: return
        val selectors = selectorConfigHolder.getFor(appSource.key)
        root.refresh()
        val parsed = parser.parse(root, selectors)
        if (parsed != null) {
            textBuffer.remove(packageName)
            dispatchRide(appSource, parsed)
        }
    }

    private suspend fun dispatchRide(source: AppSource, parsed: ParsedRide) {
        val vehicle = vehiclePreferences.activeVehicle.first()
        val metrics = calculationEngine.calculate(
            rawValue   = parsed.value,
            distanceKm = parsed.distanceKm,
            durationMin = parsed.durationMin,
            vehicle    = vehicle
        )

        Log.i(TAG, "Métricas: R$/km=${metrics.valuePerKm} R$/h=${metrics.valuePerHour} lucro=${metrics.netProfit}")

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
                    appSource    = source, rawValue = parsed.value,
                    distanceKm   = parsed.distanceKm, durationMin = parsed.durationMin,
                    valuePerKm   = metrics.valuePerKm, valuePerHour = metrics.valuePerHour,
                    fuelCost     = metrics.fuelCost, netProfit    = metrics.netProfit,
                    classification = com.calculocorridas.domain.engine.RideClassification.GOOD
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

    companion object {
        private const val DEBOUNCE_MS = 300L
    }
}
