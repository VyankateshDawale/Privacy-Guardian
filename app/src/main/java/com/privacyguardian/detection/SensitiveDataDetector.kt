package com.privacyguardian.detection

import android.graphics.Rect
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveEntity
import java.util.UUID

class SensitiveDataDetector {

    data class OcrElementRef(
        val text: String,
        val boundingBox: Rect?
    )

    fun detect(
        fullText: String,
        elements: List<OcrElementRef> = emptyList()
    ): List<SensitiveEntity> {
        if (fullText.isBlank()) return emptyList()
        val entities = mutableListOf<SensitiveEntity>()
        val seenRanges = mutableSetOf<IntRange>()

        // Helper to get surrounding window for context (200 chars around match)
        fun surroundingWindow(matchStart: Int, matchEnd: Int): String {
            val start = (matchStart - 100).coerceAtLeast(0)
            val end = (matchEnd + 100).coerceAtMost(fullText.length)
            return fullText.substring(start, end)
        }

        fun findBoundingBoxForMatch(matchedText: String): Rect? {
            // Find element containing the matched text
            // Try exact substring then fallback to any element that contains part
            for (el in elements) {
                if (el.text.contains(matchedText)) {
                    return el.boundingBox
                }
            }
            // Try cleaned match
            val cleaned = matchedText.trim()
            for (el in elements) {
                if (cleaned.isNotEmpty() && el.text.contains(cleaned.take(8))) {
                    return el.boundingBox
                }
            }
            // Fallback: find element whose text is substring of surrounding
            for (el in elements) {
                if (fullText.contains(el.text) && el.text.length > 3) {
                    // not reliable but return first
                }
            }
            return null
        }

        // Pre-process to de-duplicate OTP/bank false positives - we'll handle ordering
        // First detect critical/high patterns, then medium/low

        for (patternDef in DetectionPatterns.patterns) {
            val regex = patternDef.regex
            val matches = regex.findAll(fullText)
            for (match in matches) {
                val matchedValue = match.value
                if (matchedValue.isBlank()) continue

                // Avoid duplicate ranges (overlap)
                val range = match.range
                if (seenRanges.any { it.intersects(range) }) {
                    // Allow same range to be re-evaluated only if previous was lower priority?
                    // For now skip overlapping
                    // But we need to allow different types overlapping? Skip to avoid duplicates
                    // Check if this is OTP generic overlapping with bank/phone - skip OTP
                    if (patternDef.type == com.privacyguardian.domain.model.SensitiveType.OTP) {
                        // Don't skip, need context engine to decide; but avoid duplicate OTPs overlapping known card/phone
                        // Check overlap with non-OTP entities
                        val overlapsCritical = entities.any { e ->
                            // approximate overlap check via value
                            matchedValue.contains(e.originalValue) || e.originalValue.contains(matchedValue)
                        }
                        if (overlapsCritical) continue
                    } else {
                        // If range overlaps already detected, skip unless it's different value?
                        // We check if any existing entity already covers this exact range for same type
                        var overlaps = false
                        for (r in seenRanges) {
                            if (r.first <= range.last && range.first <= r.last) {
                                overlaps = true; break
                            }
                        }
                        if (overlaps) {
                            // For phone/email we still want distinct; but avoid double counting same text
                            val existingSameValue = entities.any { it.originalValue == matchedValue }
                            if (existingSameValue) continue
                        }
                    }
                }

                // Context evaluation
                val window = surroundingWindow(range.first, range.last + 1)
                val ctx = ContextEngine.evaluate(fullText, patternDef.type, matchedValue, window)

                // If example, reduce confidence; if very low confidence after adjustment, skip
                var confidence = patternDef.confidence * ctx.confidenceMultiplier
                var baseRisk = patternDef.baseRisk + ctx.riskAdjustment
                baseRisk = baseRisk.coerceIn(0, 100)

                // Threshold: if confidence * baseRisk too low, treat as not sensitive
                // For OTP generic pattern, require higher confidence
                if (patternDef.type == com.privacyguardian.domain.model.SensitiveType.OTP) {
                    if (confidence < 0.75f) continue // needs strong OTP context
                }
                if (patternDef.type == com.privacyguardian.domain.model.SensitiveType.BANK_CARD) {
                    if (confidence < 0.55f) continue
                }
                if (patternDef.type == com.privacyguardian.domain.model.SensitiveType.GOV_ID) {
                    if (confidence < 0.55f) continue
                }
                if (patternDef.type == com.privacyguardian.domain.model.SensitiveType.PHONE) {
                    // Exclude numbers that are likely card or OTP etc. Also check length
                    val digits = matchedValue.filter { it.isDigit() }
                    if (digits.length < 10) continue
                    if (digits.length > 15) continue
                    // If it's clearly a OTP 6-digit in OTP context, don't also count as phone
                    // We already prioritize OTP
                }

                // Example handling: don't skip outright — lower risk but keep for demo.
                // Only skip exact placeholder tokens like YOUR_KEY_HERE (not domain example.com)
                if (ctx.isExample) {
                    val isExactPlaceholder = matchedValue.equals("YOUR_KEY_HERE", ignoreCase = true) ||
                            matchedValue.equals("YOUR_SECRET_HERE", ignoreCase = true) ||
                            matchedValue.equals("YOUR_API_KEY_HERE", ignoreCase = true) ||
                            matchedValue.equals("EXAMPLE", ignoreCase = true) ||
                            matchedValue.equals("YOUR_KEY_HERE\"", ignoreCase = true)
                    if (isExactPlaceholder) {
                        continue
                    }
                    // For demo data like AKIA...EXAMPLE or postgres://...example.com — keep but with reduced risk
                    // Don't skip database URLs or AWS keys that contain example as part of demo
                    if (baseRisk < 20) continue // only skip very low after reduction
                }

                // Determine masked value
                val masked = ContextEngine.maskingFor(patternDef.type, matchedValue)

                // Determine bounding box
                val bbox = findBoundingBoxForMatch(match.value)

                // Deduplicate by masked+type
                if (entities.any { it.maskedValue == masked && it.type == patternDef.type }) {
                    continue
                }

                val riskLevel = when {
                    baseRisk >= 85 -> RiskLevel.CRITICAL
                    baseRisk >= 70 -> RiskLevel.HIGH
                    baseRisk >= 35 -> RiskLevel.MEDIUM
                    baseRisk > 0 -> RiskLevel.LOW
                    else -> RiskLevel.SAFE
                }

                entities.add(
                    SensitiveEntity(
                        id = UUID.randomUUID().toString(),
                        type = patternDef.type,
                        maskedValue = masked,
                        originalValue = matchedValue,
                        boundingBox = bbox,
                        confidence = confidence.coerceIn(0f, 1f),
                        baseRisk = baseRisk,
                        riskLevel = riskLevel,
                        context = ctx.contextLabel,
                        explanation = DetectionPatterns.explanationFor(patternDef.type),
                        recommendedAction = DetectionPatterns.recommendedActionFor(patternDef.type)
                    )
                )
                seenRanges.add(range)
            }
        }

        // Post-process: dedup OTP vs PHONE, and EMAIL inside DATABASE_URL
        val otpValues = entities.filter { it.type == com.privacyguardian.domain.model.SensitiveType.OTP }.map { it.originalValue.filter { c -> c.isDigit() } }.toSet()
        if (otpValues.isNotEmpty()) {
            entities.removeAll { e ->
                e.type == com.privacyguardian.domain.model.SensitiveType.PHONE &&
                        e.originalValue.filter { it.isDigit() } in otpValues
            }
        }
        // If DATABASE_URL exists, suppress EMAIL that is actually part of the URL (e.g., SuperSecret123@db.example.com)
        val dbUrls = entities.filter { it.type == com.privacyguardian.domain.model.SensitiveType.DATABASE_URL }.map { it.originalValue }
        if (dbUrls.isNotEmpty()) {
            entities.removeAll { e ->
                e.type == com.privacyguardian.domain.model.SensitiveType.EMAIL &&
                        dbUrls.any { url -> url.contains(e.originalValue) }
            }
        }

        // Sort by risk descending
        return entities.sortedByDescending { it.baseRisk }
    }

    private fun IntRange.intersects(other: IntRange): Boolean {
        return this.first <= other.last && other.first <= this.last
    }
}
