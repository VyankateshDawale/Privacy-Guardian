package com.privacyguardian.domain.model

import android.graphics.Rect

enum class SensitiveType {
    API_KEY,
    AWS_ACCESS_KEY,
    AWS_SECRET_KEY,
    JWT,
    PASSWORD,
    SECRET,
    OTP,
    BANK_CARD,
    GOV_ID,
    PHONE,
    EMAIL,
    ADDRESS,
    DATABASE_URL
}

enum class RiskLevel {
    CRITICAL, HIGH, MEDIUM, LOW, SAFE
}

data class SensitiveEntity(
    val id: String,
    val type: SensitiveType,
    val maskedValue: String,
    // originalValue only in memory, never persisted/logged
    val originalValue: String,
    val boundingBox: Rect?,
    val confidence: Float,
    val baseRisk: Int,
    val riskLevel: RiskLevel,
    val context: String,
    val explanation: String,
    val recommendedAction: String
)

data class PrivacyRiskResult(
    val score: Int,
    val riskLevel: RiskLevel,
    val detectedEntities: List<SensitiveEntity>,
    val explanation: String
)

enum class GuardianMode {
    NORMAL, STRICT, MAXIMUM
}
