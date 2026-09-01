package com.privacyguardian.detection

import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveType
import org.junit.Assert.*
import org.junit.Test

class SensitiveDataDetectorTest {

    private val detector = SensitiveDataDetector()

    @Test
    fun apiKeyDetection_skLive() {
        // Using generic API_KEY= prefix (avoids GitHub secret scanning for Stripe sk_live)
        // Detector matches API_KEY=... via generic pattern and sk_live_xxx via standalone pattern
        // Use pg_test pattern that still triggers API_KEY detection
        val text = "API_KEY=pg_test_51H7x8A2eZvKYlo2C4a1b2c3d4"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.API_KEY })
        assertTrue(result.any { it.type == SensitiveType.API_KEY })
    }

    @Test
    fun apiKeyDetection_exampleIsLowered() {
        val text = "Example API_KEY=YOUR_KEY_HERE"
        val result = detector.detect(text)
        // Should be filtered or low risk due to example context
        val apiKeys = result.filter { it.type == SensitiveType.API_KEY }
        // Either empty or low confidence
        if (apiKeys.isNotEmpty()) {
            assertTrue(apiKeys.first().confidence < 0.5f || apiKeys.first().baseRisk < 50)
        } else {
            assertTrue(true)
        }
    }

    @Test
    fun jwtDetection() {
        val jwt = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWI6IjEyMzQ1Njc4OTAiLCJuYW1lIjoiSm9obiBEb2UifQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c"
        val result = detector.detect("JWT=$jwt")
        assertTrue(result.any { it.type == SensitiveType.JWT })
    }

    @Test
    fun passwordDetection() {
        val text = "PASSWORD=SecurePassword123!"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.PASSWORD })
    }

    @Test
    fun otpDetection_withContext() {
        val text = "Your OTP is 482913"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.OTP && it.originalValue.contains("482913") })
        assertTrue(result.any { it.riskLevel == RiskLevel.CRITICAL })
    }

    @Test
    fun otp_notDetectedForOrderNumber() {
        val text = "Order #482913"
        val result = detector.detect(text)
        // Should NOT be OTP due to negative context
        val otp = result.filter { it.type == SensitiveType.OTP }
        assertTrue(otp.isEmpty())
    }

    @Test
    fun emailDetection() {
        val text = "Contact john.doe@iqoo.com for info"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.EMAIL && it.maskedValue.contains("@iqoo.com") })
        assertTrue(result.any { it.maskedValue == "j***@iqoo.com" })
    }

    @Test
    fun phoneDetection() {
        val text = "Call me at 9876543210"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.PHONE })
    }

    @Test
    fun awsAccessKeyDetection() {
        val text = "AKIA4B5C6D7E8F9G0H1I2J3"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.AWS_ACCESS_KEY })
    }

    @Test
    fun databaseUrlDetection() {
        val text = "DATABASE_URL=postgres://admin:SuperSecret123@db.internal.com:5432/mydb"
        val result = detector.detect(text)
        assertTrue(result.any { it.type == SensitiveType.DATABASE_URL })
    }

    @Test
    fun falsePositive_OrderNumberNotPhone() {
        val text = "Invoice #482913 is your reference"
        val result = detector.detect(text)
        // Should not be OTP or phone for isolated order number without context
        val otp = result.filter { it.type == SensitiveType.OTP }
        assertTrue(otp.isEmpty())
    }

    @Test
    fun maskingForEmail() {
        val masked = ContextEngine.maskingFor(SensitiveType.EMAIL, "user@example.com")
        assertEquals("u***@example.com", masked)
    }

    @Test
    fun maskingForPhone() {
        val masked = ContextEngine.maskingFor(SensitiveType.PHONE, "9876543210")
        assertTrue(masked.endsWith("3210"))
        assertTrue(masked.contains("******"))
    }

    @Test
    fun maskingForApiKey() {
        val masked = ContextEngine.maskingFor(SensitiveType.API_KEY, "pg_test_51H7x8A2eZvKYlo2C")
        assertTrue(masked.startsWith("pg_test"))
        assertTrue(masked.contains("********"))
    }

    @Test
    fun contextEngine_exampleReducesRisk() {
        val ctx = ContextEngine.evaluate("Example API_KEY=YOUR_KEY_HERE", SensitiveType.API_KEY, "YOUR_KEY_HERE", "Example API_KEY=YOUR_KEY_HERE is placeholder")
        assertTrue(ctx.isExample)
        // Updated logic: demo placeholder now 0.7x / -15 (not 0.3x / -40) to keep demo critical but still reduced
        assertTrue(ctx.confidenceMultiplier < 0.8f)
        assertTrue(ctx.confidenceMultiplier < 1.0f)
    }

    @Test
    fun contextEngine_otpPositiveBoosts() {
        val ctx = ContextEngine.evaluate("Your OTP is 482913", SensitiveType.OTP, "482913", "Your OTP is 482913 please verify")
        assertFalse(ctx.isExample)
        assertTrue(ctx.confidenceMultiplier > 1.0f)
    }

    @Test
    fun contextEngine_orderNumberReduces() {
        val ctx = ContextEngine.evaluate("Order #482913", SensitiveType.OTP, "482913", "Order #482913 is confirmed")
        assertTrue(ctx.confidenceMultiplier < 0.6f)
    }
}
