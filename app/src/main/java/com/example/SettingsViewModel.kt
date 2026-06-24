package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsViewModel : ViewModel() {
    private val _defaultScannerMode = MutableStateFlow("Камера")
    val defaultScannerMode: StateFlow<String> = _defaultScannerMode.asStateFlow()

    fun setDefaultScannerMode(mode: String) {
        _defaultScannerMode.value = mode
    }
}
