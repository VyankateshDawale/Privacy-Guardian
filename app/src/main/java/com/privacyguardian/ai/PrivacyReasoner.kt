package com.privacyguardian.ai

import com.privacyguardian.domain.model.SensitiveType

data class PrivacyReasonerInput(
    val entityType: SensitiveType,
    val maskedValue: String,
    val context: String,
    val documentType: String
)

data class PrivacyReasonerOutput(
    val category: String,
    val risk: String,
    val confidence: Float,
    val reason: String,
    val recommendedAction: String
)

interface PrivacyReasoner {
    suspend fun reason(input: PrivacyReasonerInput): PrivacyReasonerOutput
    fun isLocalModel(): Boolean
}
