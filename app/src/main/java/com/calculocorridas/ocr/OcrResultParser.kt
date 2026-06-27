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

        var value: Double? = null
        var totalDist = 0.0
        var distCount = 0
        var totalDur  = 0.0
        var durCount  = 0
        var destination: String? = null

        for (raw in lines) {
            val line = raw.trim()
            if (line.isBlank()) continue

            // Preço: apenas primeira ocorrência
            if (value == null) {
                PRICE_RE.find(line)?.let { value = parseDecimal(it.groupValues[1], isPrice = true) }
            }
            // Distância: SOMA todas (pickup + destino)
            DISTANCE_RE.findAll(line).forEach { m ->
                parseDecimal(m.groupValues[1])?.let { totalDist += it; distCount++ }
            }
            // Tempo: SOMA todos (pickup + destino)
            TIME_RE.findAll(line).forEach { m ->
                m.groupValues[1].toDoubleOrNull()?.let { totalDur += it; durCount++ }
            }
        }

        val distKm = if (distCount > 0) totalDist else null
        val durMin = if (durCount  > 0) totalDur  else null

        if (value == null || value!! <= 0.0)   { Log.d(TAG, "Valor ausente"); return null }
        if (distKm == null || distKm <= 0.0)   { Log.d(TAG, "Distância ausente"); return null }
        if (durMin == null || durMin <= 0.0)   { Log.d(TAG, "Tempo ausente"); return null }

        destination = findDestination(lines, value!!, distKm, durMin)

        Log.i(TAG, "OCR parse OK → R\$$value · ${distKm}km ($distCount segmentos) · ${durMin}min ($durCount segmentos) · dest=$destination")

        return ParsedRide(
            value       = value!!,
            distanceKm  = distKm,
            durationMin = durMin,
            origin      = null,
            destination = destination,
            category    = null
        )
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun parseDecimal(s: String, isPrice: Boolean = false): Double? {
        val clean = s.trim()
        // "18,50" → "18.50"  |  "1.234,56" → "1234.56"
        val value = when {
            clean.contains(',') && clean.contains('.') -> {
                // Ponto é separador de milhar, vírgula é decimal
                clean.replace(".", "").replace(",", ".").toDoubleOrNull()
            }
            clean.contains(',') -> clean.replace(",", ".").toDoubleOrNull()
            else -> clean.toDoubleOrNull()
        }
        // Para preços: inteiro ≥ 1000 sem separador decimal = OCR perdeu a vírgula
        // Ex: "17,10" lido como "1710" → 1710 ÷ 100 = 17.10
        if (isPrice && value != null && value >= 1000.0 &&
            !clean.contains(',') && !clean.contains('.')) {
            Log.d(TAG, "Preço ajustado (vírgula perdida pelo OCR): $clean → ${value / 100.0}")
            return value / 100.0
        }
        return value
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
