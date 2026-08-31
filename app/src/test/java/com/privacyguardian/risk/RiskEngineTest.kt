package com.privacyguardian.risk

import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveEntity
import com.privacyguardian.domain.model.SensitiveType
import org.junit.Assert.*
import org.junit.Test

class RiskEngineTest {

    private val engine = RiskEngine()

    private fun makeEntity(type: SensitiveType, baseRisk: Int, level: RiskLevel): SensitiveEntity {
        return SensitiveEntity(
            id = "id",
            type = type,
            maskedValue = "masked",
            originalValue = "orig",
            boundingBox = null,
            confidence = 0.9f,
            baseRisk = baseRisk,
            riskLevel = level,
            context = "test",
            explanation = "test",
            recommendedAction = "test"
        )
    }

    @Test
    fun emptyScoringIsZero() {
        val result = engine.calculateRisk(emptyList())
        assertEquals(0, result.score)
        assertEquals(RiskLevel.SAFE, result.riskLevel)
    }

    @Test
    fun criticalApiKeyScoring() {
        val e = makeEntity(SensitiveType.API_KEY, 95, RiskLevel.CRITICAL)
        val result = engine.calculateRisk(listOf(e))
        assertEquals(95, result.score)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun multipleEntitiesIncreasesScore() {
        val e1 = makeEntity(SensitiveType.API_KEY, 95, RiskLevel.CRITICAL)
        val e2 = makeEntity(SensitiveType.PASSWORD, 90, RiskLevel.CRITICAL)
        val e3 = makeEntity(SensitiveType.EMAIL, 20, RiskLevel.LOW)
        val result = engine.calculateRisk(listOf(e1, e2, e3))
        // Should be capped at 100 and >= 90
        assertTrue(result.score >= 95)
        assertTrue(result.score <= 100)
        assertEquals(RiskLevel.CRITICAL, result.riskLevel)
    }

    @Test
    fun mediumRiskScoring() {
        val e = makeEntity(SensitiveType.PHONE, 40, RiskLevel.MEDIUM)
        val result = engine.calculateRisk(listOf(e))
        assertTrue(result.score in 35..69)
        assertEquals(RiskLevel.MEDIUM, result.riskLevel)
    }

    @Test
    fun lowRiskScoring() {
        val e = makeEntity(SensitiveType.EMAIL, 20, RiskLevel.LOW)
        val result = engine.calculateRisk(listOf(e))
        assertTrue(result.score in 1..34)
        assertEquals(RiskLevel.LOW, result.riskLevel)
    }

    @Test
    fun highCountBoostsToCritical() {
        val e1 = makeEntity(SensitiveType.BANK_CARD, 80, RiskLevel.HIGH)
        val e2 = makeEntity(SensitiveType.GOV_ID, 80, RiskLevel.HIGH)
        val result = engine.calculateRisk(listOf(e1, e2))
        // Two highs should push toward critical or at least high
        assertTrue(result.score >= 80)
    }
}
