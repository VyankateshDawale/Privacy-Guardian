package com.privacyguardian.ai

import com.privacyguardian.detection.DetectionPatterns

class RuleBasedPrivacyReasoner : PrivacyReasoner {

    override suspend fun reason(input: PrivacyReasonerInput): PrivacyReasonerOutput {
        val explanation = DetectionPatterns.explanationFor(input.entityType)
        val action = DetectionPatterns.recommendedActionFor(input.entityType)
        val whatCanLeak = DetectionPatterns.whatCanLeakFor(input.entityType)
        val risk = when (input.entityType) {
            com.privacyguardian.domain.model.SensitiveType.API_KEY -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.AWS_ACCESS_KEY -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.AWS_SECRET_KEY -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.JWT -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.PASSWORD -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.SECRET -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.DATABASE_URL -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.BANK_CARD -> "HIGH"
            com.privacyguardian.domain.model.SensitiveType.GOV_ID -> "HIGH"
            com.privacyguardian.domain.model.SensitiveType.OTP -> "CRITICAL"
            com.privacyguardian.domain.model.SensitiveType.PHONE -> "MEDIUM"
            com.privacyguardian.domain.model.SensitiveType.ADDRESS -> "MEDIUM"
            com.privacyguardian.domain.model.SensitiveType.EMAIL -> "LOW"
        }
        return PrivacyReasonerOutput(
            category = input.entityType.name,
            risk = risk,
            confidence = 0.92f,
            reason = explanation,
            recommendedAction = action
        )
    }

    override fun isLocalModel(): Boolean = false
}
