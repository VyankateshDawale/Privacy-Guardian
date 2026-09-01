package com.privacyguardian.detection

import com.privacyguardian.domain.model.SensitiveType

object ContextEngine {

    data class ContextResult(
        val isExample: Boolean,
        val confidenceMultiplier: Float,
        val riskAdjustment: Int,
        val contextLabel: String
    )

    private val exampleIndicators = listOf(
        "your_key_here", "your_api_key", "your_secret", "placeholder", "dummy", "sample", "test_key", "fake"
    )

    private val productionIndicators = listOf(
        "production", "prod", "live", "secret", "private"
    )

    private val otpPositiveContext = listOf(
        "otp", "one-time", "one time", "verification code", "auth code", "login code", "your code is"
    )

    private val otpNegativeContext = listOf(
        "order", "invoice", "ticket", "reference", "id #", "order #", "bill", "tracking"
    )

    private val secretPositiveContext = listOf(
        "api_key", "api-key", "apikey", "password", "secret", "aws", "jwt", "token"
    )

    fun evaluate(text: String, detectedType: SensitiveType, matchedValue: String, surroundingWindow: String): ContextResult {
        val lower = surroundingWindow.lowercase()
        val valueLower = matchedValue.lowercase()

        // For EMAIL/PHONE/ADDRESS, don't treat example.com domain as automatic example - keep neutral
        // Only API_KEY etc should be heavily penalized for example
        val isSensitiveExampleType = detectedType in setOf(SensitiveType.API_KEY, SensitiveType.AWS_ACCESS_KEY, SensitiveType.AWS_SECRET_KEY, SensitiveType.PASSWORD, SensitiveType.SECRET, SensitiveType.JWT, SensitiveType.DATABASE_URL)
        val isEmailOrPhone = detectedType in setOf(SensitiveType.EMAIL, SensitiveType.PHONE)

        // Example / documentation detection — only true placeholder, not domain example.com
        // For demo file, we want real secrets like AKIA...EXAMPLE to still count as high risk (just slightly reduced)
        val isExample = if (isEmailOrPhone) {
            valueLower.contains("your_") || valueLower.contains("your-") || valueLower.contains("xxx") || lower.contains("your_key_here") || lower.contains("placeholder")
        } else {
            // Only explicit placeholders in value or surrounding, not bare 'example' substring from example.com
            val placeholderInValue = valueLower == "your_key_here" || valueLower == "your_api_key" || valueLower.contains("your_key_here") && valueLower.length < 30
            val explicitPlaceholder = placeholderInValue || valueLower == "xxx" || valueLower == "dummy" || valueLower.contains("placeholder")
            val surroundingPlaceholder = exampleIndicators.any { lower.contains(it) }
            // Special case: AWS example key AKIA...EXAMPLE — treat as demo secret, keep high risk (slight penalty)
            val awsExample = detectedType == SensitiveType.AWS_ACCESS_KEY && valueLower.contains("example")
            if (awsExample) {
                // Demo: keep detection but slightly lower risk so vẫn critical
                return ContextResult(true, 0.85f, -10, "Demo / example key")
            }
            // DATABASE_URL with example.com should NOT be considered example — keep critical
            if (detectedType == SensitiveType.DATABASE_URL && valueLower.contains("example.com")) {
                false
            } else {
                explicitPlaceholder || surroundingPlaceholder || (valueLower.contains("your_") && valueLower.length < 30)
            }
        }

        if (isExample) {
            return ContextResult(
                isExample = true,
                confidenceMultiplier = 0.7f,
                riskAdjustment = -15,
                contextLabel = "Example / documentation"
            )
        }

        // Production context increases risk
        val isProduction = productionIndicators.any { lower.contains(it) }
        if (isProduction && detectedType in setOf(SensitiveType.API_KEY, SensitiveType.PASSWORD, SensitiveType.SECRET, SensitiveType.AWS_ACCESS_KEY)) {
            return ContextResult(false, 1.1f, +10, "Production credential")
        }

        // OTP context
        if (detectedType == SensitiveType.OTP) {
            val pos = otpPositiveContext.any { lower.contains(it) }
            val neg = otpNegativeContext.any { lower.contains(it) }
            if (pos) {
                return ContextResult(false, 1.4f, +10, "OTP context")
            }
            if (neg) {
                return ContextResult(false, 0.4f, -50, "Likely order/reference number")
            }
            // Isolated number without OTP context => lower confidence
            return ContextResult(false, 0.5f, -30, "Unverified numeric code")
        }

        // BANK_CARD context: if no card keywords, reduce confidence
        if (detectedType == SensitiveType.BANK_CARD) {
            val cardKeywords = listOf("card", "visa", "mastercard", "credit", "debit", "card number")
            val hasKeyword = cardKeywords.any { lower.contains(it) }
            // Also apply Luhn check influence
            val digits = matchedValue.filter { it.isDigit() }
            val luhnValid = isLuhnValid(digits)
            if (!luhnValid && !hasKeyword) {
                return ContextResult(false, 0.4f, -20, "Unverified numeric sequence")
            }
            if (luhnValid) {
                return ContextResult(false, 1.2f, +10, "Card-like number (Luhn valid)")
            }
        }

        // GOV_ID: similar
        if (detectedType == SensitiveType.GOV_ID) {
            val govKeywords = listOf("aadhaar", "ssn", "id number", "gov", "identity")
            if (govKeywords.any { lower.contains(it) }) {
                return ContextResult(false, 1.2f, +10, "ID context")
            }
        }

        // Generic secret context boost
        if (detectedType in setOf(SensitiveType.API_KEY, SensitiveType.PASSWORD, SensitiveType.SECRET)) {
            if (secretPositiveContext.any { lower.contains(it) }) {
                return ContextResult(false, 1.2f, +5, "Credential context")
            }
        }

        // Email/phone with no extra context: keep neutral
        return ContextResult(false, 1.0f, 0, "Detected")
    }

    private fun isLuhnValid(number: String): Boolean {
        if (number.length < 13 || number.length > 19) return false
        var sum = 0
        var alt = false
        for (i in number.length - 1 downTo 0) {
            var n = number[i].digitToInt()
            if (alt) {
                n *= 2
                if (n > 9) n -= 9
            }
            sum += n
            alt = !alt
        }
        return sum % 10 == 0
    }

    fun maskingFor(type: SensitiveType, value: String): String {
        return when (type) {
            SensitiveType.API_KEY -> "sk_live_" + java.util.UUID.randomUUID().toString().replace("-", "").take(24)
            SensitiveType.AWS_ACCESS_KEY -> "AKIA" + (1..16).map { ('A'..'Z').random() }.joinToString("")
            SensitiveType.AWS_SECRET_KEY -> (1..40).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            SensitiveType.JWT -> "eyJhbGciOiJIUzI1NiJ9." + (1..32).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("") + ".ghost_signature"
            SensitiveType.PASSWORD -> (1..12).map { (('a'..'z') + ('A'..'Z') + ('0'..'9') + listOf('!','@','#','$')).random() }.joinToString("")
            SensitiveType.SECRET -> (1..16).map { (('a'..'z') + ('A'..'Z') + ('0'..'9')).random() }.joinToString("")
            SensitiveType.DATABASE_URL -> "postgres://ghost_user:" + (1..10).map{('a'..'z').random()}.joinToString("") + "@127.0.0.1:5432/ghost_db"
            SensitiveType.OTP -> (1..6).map { ('0'..'9').random() }.joinToString("")
            SensitiveType.BANK_CARD -> "4111 1111 1111 " + (1000..9999).random().toString()
            SensitiveType.GOV_ID -> "999-00-" + (1000..9999).random().toString()
            SensitiveType.PHONE -> "+1 (555) 019-" + (1000..9999).random().toString()
            SensitiveType.EMAIL -> "ghost_" + (1000..9999).random().toString() + "@anon.local"
            else -> "[REDACTED]"
        }
    }
}
