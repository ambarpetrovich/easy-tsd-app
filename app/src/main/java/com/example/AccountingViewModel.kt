package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class AccountingType(val displayName: String) {
    RECEIPT("Приёмка"),
    SHIPMENT("Отгрузка")
}

data class AccountingOperation(
    val code: String,
    val type: AccountingType,
    val timestamp: Long,
    val sessionId: String,
    val sessionName: String
)

class AccountingViewModel : ViewModel() {
    private val _operations = MutableStateFlow<List<AccountingOperation>>(emptyList())
    val operations: StateFlow<List<AccountingOperation>> = _operations.asStateFlow()
    
    // Map of sessionId -> AccountingType (for the session as a whole)
    private val _sessionAccountingStatus = MutableStateFlow<Map<String, AccountingType>>(emptyMap())
    val sessionAccountingStatus: StateFlow<Map<String, AccountingType>> = _sessionAccountingStatus.asStateFlow()

    fun verifyOperations(codes: List<String>, type: AccountingType): List<Pair<String, List<AccountingOperation>>> {
        val warnings = mutableListOf<Pair<String, List<AccountingOperation>>>()
        val currentOps = _operations.value
        
        for (code in codes) {
            val codeOps = currentOps.filter { it.code == code }.sortedByDescending { it.timestamp }
            if (codeOps.isNotEmpty()) {
                val lastOp = codeOps.first()
                if (lastOp.type == type) {
                    warnings.add(code to codeOps)
                }
            }
        }
        return warnings
    }

    fun applyAccounting(codes: List<String>, type: AccountingType, sessionId: String, sessionName: String) {
        val newOps = codes.map { code ->
            AccountingOperation(
                code = code,
                type = type,
                timestamp = System.currentTimeMillis(),
                sessionId = sessionId,
                sessionName = sessionName
            )
        }
        _operations.update { it + newOps }
        _sessionAccountingStatus.update { it + (sessionId to type) }
    }

    fun cancelAccounting(sessionId: String) {
        _operations.update { ops -> ops.filter { it.sessionId != sessionId } }
        _sessionAccountingStatus.update { status -> status - sessionId }
    }
}
