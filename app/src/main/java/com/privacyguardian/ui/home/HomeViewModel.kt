package com.privacyguardian.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.data.local.ScanHistoryEntity
import com.privacyguardian.domain.model.GuardianMode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

data class HomeState(
    val riskScore: Int = 18,
    val guardianMode: GuardianMode = GuardianMode.NORMAL,
    val recent: List<ScanHistoryEntity> = emptyList()
)

class HomeViewModel : ViewModel() {
    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state

    private var app: PrivacyGuardianApp? = null

    fun init(app: PrivacyGuardianApp) {
        this.app = app
        viewModelScope.launch {
            app.preferencesManager.lastRiskScore.collect { score ->
                _state.value = _state.value.copy(riskScore = score)
            }
        }
        viewModelScope.launch {
            app.preferencesManager.guardianMode.collect { mode ->
                _state.value = _state.value.copy(guardianMode = mode)
            }
        }
        refreshRecent()
    }

    fun refreshRecent() {
        viewModelScope.launch {
            val rec = app?.scanHistoryRepository?.getRecent(3) ?: emptyList()
            _state.value = _state.value.copy(recent = rec)
        }
    }

    fun setGuardianMode(mode: GuardianMode) {
        viewModelScope.launch {
            app?.preferencesManager?.setGuardianMode(mode)
        }
    }
}
