package com.privacyguardian.risk

import com.privacyguardian.domain.model.PrivacyRiskResult
import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveEntity
import kotlin.math.roundToInt

class RiskEngine {

    fun calculateRisk(entities: List<SensitiveEntity>): PrivacyRiskResult {
        if (entities.isEmpty()) {
            return PrivacyRiskResult(
                score = 0,
                riskLevel = RiskLevel.SAFE,
                detectedEntities = emptyList(),
                explanation = "No sensitive information detected. Safe to share."
            )
        }

        // Scoring: take max base risk as anchor, then add weighted contributions from others
        // But also consider context multiplier etc. Already baseRisk includes context.
        // Formula: score = max + (sum of others * 0.15) + countBonus; capped at 100
        val maxRisk = entities.maxOf { it.baseRisk }
        val otherSum = entities.sortedByDescending { it.baseRisk }.drop(1).sumOf { it.baseRisk }
        val countBonus = (entities.size - 1) * 2 // small bonus for multiple items

        var rawScore = maxRisk + (otherSum * 0.15).roundToInt() + countBonus

        // If any CRITICAL present, ensure at least 85
        val hasCritical = entities.any { it.riskLevel == RiskLevel.CRITICAL }
        if (hasCritical && rawScore < 85) rawScore = 85

        // If multiple highs, push to critical
        val highCount = entities.count { it.riskLevel == RiskLevel.HIGH }
        if (highCount >= 2 && rawScore < 90) rawScore = (rawScore + 10).coerceAtMost(100)

        val score = rawScore.coerceIn(0, 100)

        val riskLevel = when {
            score >= 85 -> RiskLevel.CRITICAL
            score >= 70 -> RiskLevel.HIGH
            score >= 35 -> RiskLevel.MEDIUM
            score > 0 -> RiskLevel.LOW
            else -> RiskLevel.SAFE
        }

        val explanation = buildExplanation(entities, score, riskLevel)

        return PrivacyRiskResult(score, riskLevel, entities, explanation)
    }

    private fun buildExplanation(entities: List<SensitiveEntity>, score: Int, level: RiskLevel): String {
        val critical = entities.count { it.riskLevel == RiskLevel.CRITICAL }
        val high = entities.count { it.riskLevel == RiskLevel.HIGH }
        val medium = entities.count { it.riskLevel == RiskLevel.MEDIUM }
        val low = entities.count { it.riskLevel == RiskLevel.LOW }

        if (entities.isEmpty()) return "No sensitive information detected."
        val topTypes = entities.take(3).joinToString(", ") { it.type.name.replace("_", " ") }
        val base = "This content contains ${entities.size} sensitive item(s) ($topTypes) that could be exposed if shared."
        val severity = when (level) {
            RiskLevel.CRITICAL -> " Critical risk — immediate protection recommended."
            RiskLevel.HIGH -> " High risk — protection recommended before sharing."
            RiskLevel.MEDIUM -> " Medium risk — review before sharing."
            RiskLevel.LOW -> " Low risk — mostly contact information."
            RiskLevel.SAFE -> ""
        }
        return base + severity
    }
}
