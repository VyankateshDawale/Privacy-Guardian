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
        "example", "sample", "placeholder", "your_key_here", "your_api_key",
        "xxx", "xxxx", "test_key", "dummy", "fake"
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

        // Example / documentation detection
        val isExample = if (isEmailOrPhone) {
            // For email/phone, only treat as example if explicitly placeholder-like YOUR, XXX
            valueLower.contains("your_") || valueLower.contains("your-") || valueLower.contains("xxx") || lower.contains("your_key_here") || lower.contains("placeholder")
        } else {
            exampleIndicators.any { lower.contains(it) || valueLower.contains(it) } ||
                    valueLower.contains("your_") ||
                    valueLower.contains("example")
        }

        if (isExample) {
            return ContextResult(
                isExample = true,
                confidenceMultiplier = 0.3f,
                riskAdjustment = -40,
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
            SensitiveType.API_KEY -> {
                if (value.length <= 8) "********"
                else value.take(8) + "********"
            }
            SensitiveType.AWS_ACCESS_KEY -> "AKIA****************"
            SensitiveType.AWS_SECRET_KEY -> "********************"
            SensitiveType.JWT -> "eyJ********.********.********"
            SensitiveType.PASSWORD -> "*".repeat(value.length.coerceIn(8, 16))
            SensitiveType.SECRET -> "*".repeat(value.length.coerceIn(8, 16))
            SensitiveType.DATABASE_URL -> "postgres://***:***@***"
            SensitiveType.OTP -> "*".repeat(value.length)
            SensitiveType.BANK_CARD -> "**** **** **** " + value.filter { it.isDigit() }.takeLast(4)
            SensitiveType.GOV_ID -> "**** **** " + value.filter { it.isDigit() }.takeLast(4)
            SensitiveType.PHONE -> {
                val digits = value.filter { it.isDigit() }
                if (digits.length >= 4) "******" + digits.takeLast(4) else "******"
            }
            SensitiveType.EMAIL -> {
                val parts = value.split("@")
                if (parts.size == 2) {
                    val user = parts[0]
                    val maskedUser = if (user.length <= 1) "*" else user.first() + "***"
                    "$maskedUser@${parts[1]}"
                } else "u***@example.com"
            }
            SensitiveType.ADDRESS -> "**** Address ****"
        }
    }
}
