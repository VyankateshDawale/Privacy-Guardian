package com.privacyguardian.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.privacyguardian.domain.model.GuardianMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "privacy_guardian_prefs")

class PreferencesManager(private val context: Context) {

    private val guardianModeKey = stringPreferencesKey("guardian_mode")
    private val privacyRiskKey = intPreferencesKey("privacy_risk")

    val guardianMode: Flow<GuardianMode> = context.dataStore.data.map { prefs ->
        val v = prefs[guardianModeKey] ?: GuardianMode.NORMAL.name
        try { GuardianMode.valueOf(v) } catch (_: Exception) { GuardianMode.NORMAL }
    }

    suspend fun setGuardianMode(mode: GuardianMode) {
        context.dataStore.edit { it[guardianModeKey] = mode.name }
    }

    val lastRiskScore: Flow<Int> = context.dataStore.data.map { it[privacyRiskKey] ?: 18 }

    suspend fun setLastRiskScore(score: Int) {
        context.dataStore.edit { it[privacyRiskKey] = score }
    }
}
