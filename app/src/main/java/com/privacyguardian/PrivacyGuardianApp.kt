package com.privacyguardian

import android.app.Application
import androidx.room.Room
import com.privacyguardian.ai.LocalPrivacyModel
import com.privacyguardian.ai.RuleBasedPrivacyReasoner
import com.privacyguardian.data.local.PreferencesManager
import com.privacyguardian.data.local.PrivacyDatabase
import com.privacyguardian.data.repository.ScanHistoryRepository
import com.privacyguardian.detection.SensitiveDataDetector
import com.privacyguardian.ocr.MlKitOcrEngine
import com.privacyguardian.protection.ProtectionEngine
import com.privacyguardian.risk.RiskEngine

class PrivacyGuardianApp : Application() {

    lateinit var database: PrivacyDatabase
        private set
    lateinit var scanHistoryRepository: ScanHistoryRepository
        private set
    lateinit var preferencesManager: PreferencesManager
        private set

    // Engines
    val detector by lazy { SensitiveDataDetector() }
    val riskEngine by lazy { RiskEngine() }
    val ocrEngine by lazy { MlKitOcrEngine() }
    val privacyReasoner by lazy { LocalPrivacyModel(this, RuleBasedPrivacyReasoner()) }
    // Protection needs context, will be provided via factory or new instance
    fun protectionEngine(): ProtectionEngine = ProtectionEngine(this)

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(this, PrivacyDatabase::class.java, "privacy_guardian.db")
            .fallbackToDestructiveMigration()
            .build()
        scanHistoryRepository = ScanHistoryRepository(database.scanHistoryDao())
        preferencesManager = PreferencesManager(this)
    }
}
