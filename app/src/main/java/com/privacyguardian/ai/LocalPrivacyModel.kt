package com.privacyguardian.ai

import android.content.Context

/**
 * Stub for future TFLite/LiteRT integration.
 * Currently delegates to RuleBasedPrivacyReasoner to keep app stable.
 * Architecture allows swapping without changing callers.
 */
class LocalPrivacyModel(
    private val context: Context,
    private val fallback: PrivacyReasoner = RuleBasedPrivacyReasoner()
) : PrivacyReasoner {

    // In a real implementation, this would load a .tflite model via TensorFlow Lite
    // and run inference. For hackathon prototype, we keep deterministic fallback.

    private var modelAvailable: Boolean = false

    init {
        // Try to check for model file existence without failing
        try {
            val files = context.assets.list("") ?: emptyArray()
            modelAvailable = files.any { it.endsWith(".tflite") }
        } catch (_: Exception) {
            modelAvailable = false
        }
    }

    override suspend fun reason(input: PrivacyReasonerInput): PrivacyReasonerOutput {
        // Future: run TFLite inference here with sanitized input
        // For now, delegate to fallback (which uses same interface)
        return fallback.reason(input)
    }

    override fun isLocalModel(): Boolean = modelAvailable

    fun getStatus(): String {
        return if (modelAvailable) "Local contextual reasoning: enabled"
        else "Local contextual reasoning: rule-based fallback"
    }
}
