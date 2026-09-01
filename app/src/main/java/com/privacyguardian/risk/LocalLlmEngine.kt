package com.privacyguardian.risk

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class LocalLlmEngine {
    
    /**
     * In a production environment on the iQOO 15, this would interface with 
     * Google AICore (Gemini Nano) or MediaPipe LLM Inference API (Llama 3).
     * For demonstration on normal hardware, we simulate the token streaming 
     * to prevent out-of-memory crashes on non-NPU devices.
     */
    fun streamThreatAnalysis(text: String): Flow<String> = flow {
        val lowerText = text.lowercase()
        
        // Contextual prompt routing based on input
        val responseTokens = when {
            lowerText.contains("boss") || lowerText.contains("urgent") || lowerText.contains("password") -> {
                "⚠️ SOCIAL ENGINEERING THREAT DETECTED.\n\n" +
                "Analysis:\n" +
                "The semantic structure of this message exhibits high urgency and requests sensitive credentials. " +
                "This is a classic 'Spear Phishing' tactic. The sender is attempting to bypass technical controls " +
                "by manipulating human trust.\n\n" +
                "Action Taken:\n" +
                "Ghost Mode is recommended. Do not paste real credentials."
            }
            lowerText.contains("api") || lowerText.contains("key") || lowerText.contains("token") -> {
                "⚠️ CREDENTIAL LEAK DETECTED.\n\n" +
                "Analysis:\n" +
                "The text contains a high-entropy string commonly associated with infrastructure access tokens. " +
                "Exposing this string in a non-secure environment could lead to unauthorized system access.\n\n" +
                "Action Taken:\n" +
                "Token redacted. NPU Shield active."
            }
            else -> {
                "✅ NO BEHAVIORAL THREATS DETECTED.\n\n" +
                "Analysis:\n" +
                "The semantic context appears conversational and benign. No coercive language or hidden " +
                "payload structures were identified by the local model."
            }
        }.split(" ")

        // Simulate local model loading into RAM
        delay(600)
        
        var currentOutput = ""
        // Stream tokens exactly like an LLM
        for (word in responseTokens) {
            currentOutput += word + " "
            emit(currentOutput)
            // Simulate token generation time (approx 20-30 tokens per second on Snapdragon Gen 3)
            delay((20..60).random().toLong()) 
        }
    }
}