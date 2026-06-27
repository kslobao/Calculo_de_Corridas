package com.calculocorridas.ocr

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityWindowInfo
import androidx.annotation.RequiresApi
import com.calculocorridas.data.datastore.UserPreferences
import com.calculocorridas.services.accessibility.parsers.ParsedRide
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.tasks.await
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min

private const val TAG           = "OcrManager"
private const val MAX_WIDTH_PX  = 720
private const val MAX_PIXELS    = 1_000_000          // 1 MP
private const val ECONOMIC_MS   = 1_500L             // modo econômico: mín 1.5s entre OCR
private const val HASH_SAMPLE   = 20                 // grade NxN para hash rápido

@Singleton
class OcrManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: UserPreferences
) {
    // ML Kit — backing field nullable para poder verificar sem inicializar
    private var _recognizer: com.google.mlkit.vision.text.TextRecognizer? = null
    private val recognizer: com.google.mlkit.vision.text.TextRecognizer
        get() = _recognizer ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
            .also { _recognizer = it }

    // Cache
    private var lastHash: Int   = 0
    private var lastResult: ParsedRide? = null

    // Rate limit
    private var lastOcrMs: Long = 0L

    // ── API pública ───────────────────────────────────────────────────────────

    /**
     * Verifica se o OCR pode ser tentado agora (prefs + rate limit).
     */
    suspend fun shouldAttemptOcr(): Boolean {
        if (!prefs.ocrEnabled.first()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return false

        if (prefs.ocrEconomicMode.first()) {
            val elapsed = System.currentTimeMillis() - lastOcrMs
            if (elapsed < ECONOMIC_MS) {
                Log.d(TAG, "Modo econômico: aguardando ${(ECONOMIC_MS - elapsed) / 1000}s")
                return false
            }
        }
        return true
    }

    /**
     * Executa OCR no bitmap fornecido (já recortado da região do card).
     * O caller é responsável por reciclar o bitmap após a chamada.
     * Retorna ParsedRide ou null. Resultado cacheado por hash.
     */
    suspend fun processImage(bitmap: Bitmap): ParsedRide? {
        val t0 = SystemClock.elapsedRealtime()

        // Hash rápido por amostragem para evitar re-OCR da mesma imagem
        val hash = bitmap.quickHash()
        if (hash == lastHash && lastResult != null) {
            Log.d(TAG, "Hash idêntico ao anterior — retornando cache (sem OCR)")
            return lastResult
        }

        // Redimensionar para economizar CPU/memória
        val scaled = bitmap.scaleDown()
        val imgSize = scaled.byteCount / 1024
        Log.d(TAG, "OCR: imagem ${scaled.width}x${scaled.height} (${imgSize}KB)")

        // ML Kit
        val image   = InputImage.fromBitmap(scaled, 0)
        val t1      = SystemClock.elapsedRealtime()
        val mlResult = runCatching { recognizer.process(image).await() }.getOrNull()
        val ocrMs   = SystemClock.elapsedRealtime() - t1

        if (scaled !== bitmap) scaled.recycle()

        if (mlResult == null) {
            Log.w(TAG, "ML Kit retornou null após ${ocrMs}ms")
            return null
        }

        // Coletar linhas de texto por bloco (preserva layout vertical)
        val lines = mlResult.textBlocks
            .sortedBy { it.boundingBox?.top ?: 0 }
            .flatMap { block -> block.lines.map { it.text } }

        val t2      = SystemClock.elapsedRealtime()
        val result  = OcrResultParser.parse(lines)
        val parseMs = SystemClock.elapsedRealtime() - t2
        val totalMs = SystemClock.elapsedRealtime() - t0

        logMetrics(
            success   = result != null,
            cacheHit  = false,
            imgKb     = imgSize,
            w         = scaled.width,
            h         = scaled.height,
            linesFound = lines.size,
            ocrMs     = ocrMs,
            parseMs   = parseMs,
            totalMs   = totalMs
        )

        // Atualizar cache e rate limit
        lastHash   = hash
        lastResult = result
        lastOcrMs  = System.currentTimeMillis()

        return result
    }

    /**
     * Detecta a região do card na lista de janelas acessíveis.
     * Usa a primeira janela Uber cujo topo está na metade inferior da tela,
     * sinalizando um bottom sheet. Se não encontrar, retorna a metade inferior
     * da janela principal do app como fallback.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    fun detectCardBounds(
        windows: List<AccessibilityWindowInfo>,
        packageName: String,
        screenW: Int,
        screenH: Int
    ): Rect {
        val appWindows = windows.filter { w ->
            runCatching { w.root?.packageName?.toString() == packageName }.getOrDefault(false)
        }

        // Janela do app cujo topo começa abaixo de 30% da tela → provável card/bottom sheet
        for (w in appWindows) {
            val b = Rect()
            w.getBoundsInScreen(b)
            if (b.top > screenH * 0.30 && b.height() > 100) {
                Log.d(TAG, "[$packageName] Card detectado via janela: $b")
                return b
            }
        }

        // Fallback: capturar parte inferior da tela (onde cards aparecem)
        val fallback = Rect(0, (screenH * 0.42).toInt(), screenW, (screenH * 0.97).toInt())
        Log.d(TAG, "[$packageName] Card: fallback $fallback")
        return fallback
    }

    fun invalidateHash() {
        lastHash = 0
        Log.d(TAG, "Hash invalidado — próximo OCR forçará nova captura")
    }

    fun destroy() {
        _recognizer?.let { runCatching { it.close() } }
        _recognizer = null
    }

    // ── Internos ──────────────────────────────────────────────────────────────

    /**
     * Redimensiona mantendo proporção para ≤ MAX_WIDTH_PX ou ≤ MAX_PIXELS.
     * Retorna o próprio bitmap se já estiver dentro dos limites.
     */
    private fun Bitmap.scaleDown(): Bitmap {
        val pixels = width.toLong() * height
        if (width <= MAX_WIDTH_PX && pixels <= MAX_PIXELS) return this

        val scaleW = MAX_WIDTH_PX.toFloat() / width
        val scaleP = Math.sqrt(MAX_PIXELS.toDouble() / pixels).toFloat()
        val scale  = min(scaleW, scaleP)

        val newW = (width * scale).toInt().coerceAtLeast(1)
        val newH = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(this, newW, newH, true)
    }

    /**
     * Hash rápido por amostragem de grade NxN.
     * Evita getPixels() (copia tudo) — usa getPixel() em pontos esparsos.
     */
    private fun Bitmap.quickHash(): Int {
        var hash = 1
        val stepX = (width  / HASH_SAMPLE).coerceAtLeast(1)
        val stepY = (height / HASH_SAMPLE).coerceAtLeast(1)
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                hash = hash * 31 + getPixel(x, y)
                x += stepX
            }
            y += stepY
        }
        return hash
    }

    private fun logMetrics(
        success: Boolean,
        cacheHit: Boolean,
        imgKb: Int,
        w: Int, h: Int,
        linesFound: Int,
        ocrMs: Long,
        parseMs: Long,
        totalMs: Long
    ) {
        val status = if (success) "✓ OK" else "✗ FALHOU"
        Log.d(TAG, "[OCR] $status | cache=$cacheHit | ${w}x${h} (${imgKb}KB) | " +
            "linhas=$linesFound | ocr=${ocrMs}ms parse=${parseMs}ms total=${totalMs}ms")
    }
}

// ── Extension de conveniência ──────────────────────────────────────────────────

/**
 * Recorta o bitmap à região [bounds], respeitando os limites do bitmap.
 * Recicla o bitmap original se [recycleSrc] for true e o recorte diferir.
 */
fun Bitmap.cropSafe(bounds: Rect, recycleSrc: Boolean = true): Bitmap {
    val l = bounds.left.coerceIn(0, width - 1)
    val t = bounds.top.coerceIn(0, height - 1)
    val w = bounds.width().coerceIn(1, width - l)
    val h = bounds.height().coerceIn(1, height - t)

    val cropped = Bitmap.createBitmap(this, l, t, w, h)
    if (recycleSrc && cropped !== this) recycle()
    return cropped
}
