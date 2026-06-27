package com.calculocorridas.ocr

import android.util.Log
import com.calculocorridas.services.accessibility.parsers.ParsedRide

private const val TAG = "OcrParser"

/**
 * Extrai dados de corrida dos blocos de texto produzidos pelo ML Kit.
 * Trabalha com o layout brasileiro do Uber Driver:
 *   "R$ 18,50"  —  "3,2 km"  —  "12 min"  —  linha de destino
 */
object OcrResultParser {

    // ── Padrões ───────────────────────────────────────────────────────────────

    // R$ 18,50 | R$18.50 | R$18 | RS 18,50 (OCR confunde R$ com RS às vezes)
    private val PRICE_RE = Regex(
        """(?:R\$|RS|R\s*\$)\s*(\d{1,3}(?:[.,]\d{3})*(?:[.,]\d{1,2})|\d+)""",
        RegexOption.IGNORE_CASE
    )

    // 3,2 km | 3.2km | 12km
    private val DISTANCE_RE = Regex(
        """(\d{1,3}(?:[.,]\d{1,2})?)\s*km""",
        RegexOption.IGNORE_CASE
    )

    // 12 min | 5min | ~8 min
    private val TIME_RE = Regex(
        """~?\s*(\d{1,3})\s*min""",
        RegexOption.IGNORE_CASE
    )

    // Ruído — linhas que certamente não são destino
    private val NOISE_RE = Regex(
        """^(?:aceitar|recusar|detalhes|uber|corrida|viagem|categorias?|uberx|comfort|black|flash|\d+%|nenhum)""",
        RegexOption.IGNORE_CASE
    )

    // ── API pública ───────────────────────────────────────────────────────────

    fun parse(lines: List<String>): ParsedRide? {
        if (lines.isEmpty()) return null

        Log.d(TAG, "Linhas para parse (${lines.size}): $lines")

        var value: Double?   = null
        var distKm: Double?  = null
        var durMin: Double?  = null
        var destination: String? = null

        for (raw in lines) {
            val line = raw.trim()
            if (line.isBlank()) continue

            if (value == null) {
                PRICE_RE.find(line)?.let { value = parseDecimal(it.groupValues[1]) }
            }
            if (distKm == null) {
                DISTANCE_RE.find(line)?.let { distKm = parseDecimal(it.groupValues[1]) }
            }
            if (durMin == null) {
                TIME_RE.find(line)?.let { durMin = it.groupValues[1].toDoubleOrNull() }
            }
        }

        if (value == null || value!! <= 0.0)   { Log.d(TAG, "Valor ausente"); return null }
        if (distKm == null || distKm!! <= 0.0) { Log.d(TAG, "Distância ausente"); return null }
        if (durMin == null || durMin!! <= 0.0) { Log.d(TAG, "Tempo ausente"); return null }

        destination = findDestination(lines, value!!, distKm!!, durMin!!)

        Log.i(TAG, "OCR parse OK → R\$$value · ${distKm}km · ${durMin}min · dest=$destination")

        return ParsedRide(
            value       = value!!,
            distanceKm  = distKm!!,
            durationMin = durMin!!,
            origin      = null,
            destination = destination,
            category    = null
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseDecimal(s: String): Double? {
        val clean = s.trim()
        // "18,50" → "18.50"  |  "1.234,56" → "1234.56"
        return when {
            clean.contains(',') && clean.contains('.') -> {
                // Ponto é separador de milhar, vírgula é decimal
                clean.replace(".", "").replace(",", ".").toDoubleOrNull()
            }
            clean.contains(',') -> clean.replace(",", ".").toDoubleOrNull()
            else -> clean.toDoubleOrNull()
        }
    }

    private fun findDestination(
        lines: List<String>,
        value: Double,
        distKm: Double,
        durMin: Double
    ): String? {
        // Linhas candidatas: tem letra, não tem preço/km/min, não é ruído
        val candidates = lines.filter { raw ->
            val line = raw.trim()
            line.length >= 5 &&
            line.any { it.isLetter() } &&
            !PRICE_RE.containsMatchIn(line) &&
            !DISTANCE_RE.containsMatchIn(line) &&
            !TIME_RE.containsMatchIn(line) &&
            !NOISE_RE.containsMatchIn(line)
        }

        // Preferir a linha mais longa (endereços são descritivos)
        return candidates.maxByOrNull { it.length }
            ?.trim()
            ?.takeIf { it.length >= 5 }
    }
}
