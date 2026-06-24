package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class FreeScanCode(
    val normalizedCode: String,
    val timestamp: Long
)

data class FreeScanSession(
    val id: String,
    val title: String,
    val date: String,
    val scannedCodes: List<FreeScanCode> = emptyList()
)

class ScanSessionViewModel : ViewModel() {
    private val _sessions = MutableStateFlow<List<FreeScanSession>>(emptyList())
    val sessions: StateFlow<List<FreeScanSession>> = _sessions.asStateFlow()
    
    private val _currentSessionId = MutableStateFlow<String?>(null)
    val currentSessionId: StateFlow<String?> = _currentSessionId.asStateFlow()
    
    val scannedCodes: StateFlow<List<FreeScanCode>> = combine(_sessions, _currentSessionId) { sessions, id ->
        if (id == null) emptyList() else sessions.find { it.id == id }?.scannedCodes ?: emptyList()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    
    fun getCurrentSessionCodes(): List<FreeScanCode> {
        val id = _currentSessionId.value ?: return emptyList()
        return _sessions.value.find { it.id == id }?.scannedCodes ?: emptyList()
    }
    
    fun createNewSession(): String {
        val id = UUID.randomUUID().toString()
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = format.format(Date())
        val session = FreeScanSession(id, "Свободное сканирование", dateStr, emptyList())
        _sessions.update { listOf(session) + it }
        _currentSessionId.value = id
        return id
    }

    fun createOrGetSession(): String {
        var id = _currentSessionId.value
        if (id == null) {
            id = createNewSession()
        }
        return id
    }

    fun addCode(code: String) {
        val id = createOrGetSession()
        val newCode = FreeScanCode(code, System.currentTimeMillis())
        _sessions.update { currentList ->
            currentList.map { 
                if (it.id == id) it.copy(scannedCodes = listOf(newCode) + it.scannedCodes) else it
            }
        }
    }
    
    fun clearCodes() {
        val id = _currentSessionId.value ?: return
        _sessions.update { currentList ->
            currentList.map { 
                if (it.id == id) it.copy(scannedCodes = emptyList()) else it
            }
        }
    }
    
    fun selectSession(id: String) {
        _currentSessionId.value = id
    }
}
