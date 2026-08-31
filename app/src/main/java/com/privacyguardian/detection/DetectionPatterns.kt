package com.privacyguardian.detection

import com.privacyguardian.domain.model.RiskLevel
import com.privacyguardian.domain.model.SensitiveType

object DetectionPatterns {

    data class PatternDef(
        val type: SensitiveType,
        val regex: Regex,
        val baseRisk: Int,
        val riskLevel: RiskLevel,
        val confidence: Float = 0.95f
    )

    // CRITICAL: API keys, AWS, JWT, passwords
    val patterns: List<PatternDef> = listOf(
        // AWS Access Key: AKIA + 16 uppercase alphanumeric
        PatternDef(
            SensitiveType.AWS_ACCESS_KEY,
            Regex("""AKIA[0-9A-Z]{16}"""),
            95, RiskLevel.CRITICAL, 0.99f
        ),
        // AWS Secret Key: 40 char base64 like
        PatternDef(
            SensitiveType.AWS_SECRET_KEY,
            Regex("""(?i)aws_secret_access_key\s*[:=]\s*['\"]?([A-Za-z0-9/+=]{40})['\"]?"""),
            95, RiskLevel.CRITICAL, 0.98f
        ),
        // Generic API key: sk_live_, sk_test_, api_key, etc.
        PatternDef(
            SensitiveType.API_KEY,
            Regex("""(?i)(?:sk_live|sk_test|api[_-]?key|apikey)\s*[:=]\s*['\"]?([A-Za-z0-9_\-]{16,})['\"]?"""),
            95, RiskLevel.CRITICAL, 0.95f
        ),
        // Also detect sk_live_xxx standalone
        PatternDef(
            SensitiveType.API_KEY,
            Regex("""sk_live_[A-Za-z0-9]{10,}"""),
            95, RiskLevel.CRITICAL, 0.96f
        ),
        PatternDef(
            SensitiveType.API_KEY,
            Regex("""sk_test_[A-Za-z0-9]{10,}"""),
            95, RiskLevel.CRITICAL, 0.96f
        ),
        // Generic long token that looks like api key: 32+ hex/alnum with prefix
        PatternDef(
            SensitiveType.API_KEY,
            Regex("""(?i)(?:ghp|gho|github_pat)_[A-Za-z0-9_]{20,}"""),
            95, RiskLevel.CRITICAL, 0.97f
        ),
        // JWT: eyJ... tokens
        PatternDef(
            SensitiveType.JWT,
            Regex("""eyJ[A-Za-z0-9_-]{10,}\.eyJ[A-Za-z0-9_-]{10,}\.[A-Za-z0-9_-]{10,}"""),
            90, RiskLevel.CRITICAL, 0.97f
        ),
        // JWT simplified fallback: eyJ + . + .
        PatternDef(
            SensitiveType.JWT,
            Regex("""eyJ[A-Za-z0-9_\-]+=*\.[A-Za-z0-9_\-]+=*\.[A-Za-z0-9_\-+=/]*"""),
            90, RiskLevel.CRITICAL, 0.92f
        ),
        // Password assignment
        PatternDef(
            SensitiveType.PASSWORD,
            Regex("""(?i)(?:password|passwd|pwd)\s*[:=]\s*['\"]?([^\s'\";]{4,})['\"]?"""),
            90, RiskLevel.CRITICAL, 0.93f
        ),
        // Secret assignment
        PatternDef(
            SensitiveType.SECRET,
            Regex("""(?i)(?:secret|client_secret)\s*[:=]\s*['\"]?([A-Za-z0-9_\-+/=]{8,})['\"]?"""),
            90, RiskLevel.CRITICAL, 0.93f
        ),
        // Database URL
        PatternDef(
            SensitiveType.DATABASE_URL,
            Regex("""(?i)(?:postgres|postgresql|mysql|mongodb)://[^\s'\";]+"""),
            90, RiskLevel.CRITICAL, 0.95f
        ),
        // Bank card: 13-19 digits with optional spaces/dashes, but validate loosely
        PatternDef(
            SensitiveType.BANK_CARD,
            Regex("""\b(?:\d[ -]*?){13,19}\b"""),
            80, RiskLevel.HIGH, 0.75f
        ),
        // OTP: will be handled with context, but generic 4-8 digit code near OTP keyword
        // We keep a generic OTP pattern for context engine
        PatternDef(
            SensitiveType.OTP,
            Regex("""\b\d{4,8}\b"""),
            85, RiskLevel.CRITICAL, 0.6f
        ),
        // Phone: international and national
        PatternDef(
            SensitiveType.PHONE,
            Regex("""(?:\+?\d{1,3}[-.\s]?)?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}\b"""),
            40, RiskLevel.MEDIUM, 0.70f
        ),
        // More specific phone with country code
        PatternDef(
            SensitiveType.PHONE,
            Regex("""\+?\d{1,3}[-.\s]?\(?\d{3}\)?[-.\s]?\d{3}[-.\s]?\d{4}"""),
            40, RiskLevel.MEDIUM, 0.75f
        ),
        // Indian phone: 10 digits starting 6-9
        PatternDef(
            SensitiveType.PHONE,
            Regex("""\b[6-9]\d{9}\b"""),
            40, RiskLevel.MEDIUM, 0.80f
        ),
        // Email
        PatternDef(
            SensitiveType.EMAIL,
            Regex("""[a-zA-Z0-9._%+\-]+@[a-zA-Z0-9.\-]+\.[a-zA-Z]{2,}"""),
            20, RiskLevel.LOW, 0.95f
        ),
        // Gov ID: Aadhaar-like 12 digits, SSN-like, etc. Simplified
        PatternDef(
            SensitiveType.GOV_ID,
            Regex("""\b\d{4}\s?\d{4}\s?\d{4}\b"""),
            80, RiskLevel.HIGH, 0.65f
        ),
        // Address: heuristic - numbers + street keywords
        PatternDef(
            SensitiveType.ADDRESS,
            Regex("""(?i)\d+\s+[A-Za-z]+\s+(?:Street|St|Avenue|Ave|Road|Rd|Boulevard|Blvd|Lane|Ln|Drive|Dr)"""),
            40, RiskLevel.MEDIUM, 0.70f
        )
    )

    fun explanationFor(type: SensitiveType): String = when (type) {
        SensitiveType.API_KEY -> "This appears to be an authentication credential. Sharing it may expose access to an associated service."
        SensitiveType.AWS_ACCESS_KEY -> "This looks like an AWS access key. It could allow access to cloud resources if combined with its secret."
        SensitiveType.AWS_SECRET_KEY -> "This appears to be an AWS secret key. Sharing it could expose full access to associated AWS services."
        SensitiveType.JWT -> "This appears to be a signed authentication token. Sharing it may allow impersonation until it expires."
        SensitiveType.PASSWORD -> "This is a password credential. Sharing it could allow unauthorized account access."
        SensitiveType.SECRET -> "This appears to be a secret credential. Sharing it may expose service access."
        SensitiveType.DATABASE_URL -> "This appears to be a database connection string containing credentials. Sharing it could expose database access."
        SensitiveType.OTP -> "This appears to be a temporary authentication code. Sharing it could allow another person to use it during its validity window."
        SensitiveType.BANK_CARD -> "This appears to be a payment card number. Sharing it could expose financial information."
        SensitiveType.GOV_ID -> "This appears to be a government-issued identifier. Sharing it could expose identity information."
        SensitiveType.PHONE -> "This is personally identifying contact information."
        SensitiveType.EMAIL -> "This can identify or contact you and may increase phishing or spam exposure."
        SensitiveType.ADDRESS -> "This is location information that could expose physical privacy."
    }

    fun whatCanLeakFor(type: SensitiveType): String = when (type) {
        SensitiveType.API_KEY -> "Potential credential/service exposure"
        SensitiveType.AWS_ACCESS_KEY -> "Potential cloud account exposure"
        SensitiveType.AWS_SECRET_KEY -> "Potential cloud account exposure"
        SensitiveType.JWT -> "Potential session/account exposure"
        SensitiveType.PASSWORD -> "Potential account takeover"
        SensitiveType.SECRET -> "Potential service exposure"
        SensitiveType.DATABASE_URL -> "Potential database breach"
        SensitiveType.OTP -> "Potential authentication bypass"
        SensitiveType.BANK_CARD -> "Financial fraud exposure"
        SensitiveType.GOV_ID -> "Identity theft exposure"
        SensitiveType.PHONE -> "Identity/contact exposure"
        SensitiveType.EMAIL -> "Contact/phishing exposure"
        SensitiveType.ADDRESS -> "Physical privacy exposure"
    }

    fun recommendedActionFor(type: SensitiveType): String = when (type) {
        SensitiveType.API_KEY -> "Redact before sharing and rotate the key if exposed."
        SensitiveType.AWS_ACCESS_KEY -> "Do not share. Rotate keys via IAM if exposed."
        SensitiveType.AWS_SECRET_KEY -> "Do not share. Rotate immediately if exposed."
        SensitiveType.JWT -> "Do not share. Invalidate token if exposed."
        SensitiveType.PASSWORD -> "Do not share. Change password if exposed."
        SensitiveType.SECRET -> "Redact and rotate secret."
        SensitiveType.DATABASE_URL -> "Do not share. Change credentials and rotate."
        SensitiveType.OTP -> "Do not share. Let it expire, request new if needed."
        SensitiveType.BANK_CARD -> "Do not share. Contact bank if exposed."
        SensitiveType.GOV_ID -> "Do not share. Monitor identity if exposed."
        SensitiveType.PHONE -> "Mask before sharing if not essential."
        SensitiveType.EMAIL -> "Mask if privacy is required."
        SensitiveType.ADDRESS -> "Mask precise location before sharing."
    }
}
