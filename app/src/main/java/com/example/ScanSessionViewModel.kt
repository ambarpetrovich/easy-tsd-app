package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

data class FreeScanCode(
    val normalizedCode: String,
    val timestamp: Long
)

class ScanSessionViewModel : ViewModel() {
    private val _scannedCodes = MutableStateFlow<List<FreeScanCode>>(emptyList())
    val scannedCodes: StateFlow<List<FreeScanCode>> = _scannedCodes.asStateFlow()
    
    fun addCode(code: String) {
        val newCode = FreeScanCode(code, System.currentTimeMillis())
        _scannedCodes.update { listOf(newCode) + it }
    }
    
    fun clearCodes() {
        _scannedCodes.update { emptyList() }
    }
}
