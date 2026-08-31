package com.privacyguardian.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.privacyguardian.PrivacyGuardianApp
import com.privacyguardian.data.local.ScanHistoryEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HistoryViewModel : ViewModel() {
    private val _items = MutableStateFlow<List<ScanHistoryEntity>>(emptyList())
    val items: StateFlow<List<ScanHistoryEntity>> = _items

    private var app: PrivacyGuardianApp? = null

    fun init(app: PrivacyGuardianApp) {
        this.app = app
        viewModelScope.launch {
            app.scanHistoryRepository.getAll().collect { list ->
                _items.value = list
            }
        }
    }

    fun clearAll(onDone: () -> Unit = {}) {
        viewModelScope.launch {
            app?.scanHistoryRepository?.clearAll()
            onDone()
        }
    }
}
