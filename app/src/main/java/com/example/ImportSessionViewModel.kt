package com.example

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class ImportSessionStatus(@androidx.annotation.StringRes val displayNameRes: Int) {
    PENDING(R.string.status_pending),
    IN_PROGRESS(R.string.status_in_progress),
    COMPLETED(R.string.status_completed),
    FAILED(R.string.status_failed)
}

data class ImportFile(
    val name: String,
    val type: String, // e.g. "xlsx", "xml", "csv"
    val itemsCount: Int // lines or elements count
)

data class ImportRecognizedCode(
    val fullCode: String,
    val normalizedCode: String,
    val gtin: String,
    val fileIndex: Int = 0,
    val itemIndex: Int = 1 // line number or element index
)

enum class ImportSessionType {
    RECOGNITION,
    INVENTORY
}

data class ImportScannedCode(
    val normalizedCode: String,
    val timestamp: Long
)

data class ImportSession(
    val id: String,
    val title: String,
    val date: String,
    val status: ImportSessionStatus,
    val type: ImportSessionType = ImportSessionType.RECOGNITION,
    val progress: Float = 0f,
    val totalCodes: Int = 0,
    val files: List<ImportFile> = emptyList(),
    val recognizedCodes: List<ImportRecognizedCode> = emptyList(),
    val scannedCodes: List<ImportScannedCode> = emptyList()
)

class ImportSessionViewModel : ViewModel() {
    private fun generateMockCodes(count: Int, numFiles: Int = 1): List<ImportRecognizedCode> {
        val gtins = listOf("04601234567890", "04609876543210", "04601111222233")
        return (1..count).map {
            val gtin = gtins.random()
            val serial = java.util.UUID.randomUUID().toString().take(13).replace("-", "A")
            val fullCode = "\u00E801${gtin}21${serial}\u001D91ABCD\u001D921234567890"
            val normalizedCode = "01${gtin}21${serial}"
            val fileIndex = if (numFiles > 0) (0 until numFiles).random() else 0
            val itemIndex = (1..50).random()
            ImportRecognizedCode(fullCode, normalizedCode, gtin, fileIndex, itemIndex)
        }
    }

    private val _sessions = MutableStateFlow<List<ImportSession>>(
        listOf(
            ImportSession("1", "Импорт из 3 файлов", "24.06.2026 12:00", ImportSessionStatus.COMPLETED, ImportSessionType.RECOGNITION, 1f, 80, 
                files = listOf(
                    ImportFile("data_2026.csv", "csv", 30),
                    ImportFile("export.xml", "xml", 20),
                    ImportFile("codes.txt", "txt", 30)
                ),
                recognizedCodes = generateMockCodes(80, 3)
            )
        )
    )
    val sessions: StateFlow<List<ImportSession>> = _sessions.asStateFlow()

    fun getSessionById(id: String): StateFlow<ImportSession?> {
        val flow = MutableStateFlow<ImportSession?>(null)
        viewModelScope.launch {
            sessions.collect { list ->
                flow.value = list.find { it.id == id }
            }
        }
        return flow.asStateFlow()
    }

    init {
        viewModelScope.launch {
            while(true) {
                delay(1000)
                _sessions.update { currentList ->
                    currentList.map { session ->
                        if (session.status == ImportSessionStatus.PENDING) {
                            session.copy(status = ImportSessionStatus.IN_PROGRESS)
                        } else if (session.status == ImportSessionStatus.IN_PROGRESS) {
                            val newProgress = session.progress + 0.1f
                            if (newProgress >= 1f) {
                                session.copy(status = ImportSessionStatus.COMPLETED, progress = 1f, totalCodes = (50..300).random())
                            } else {
                                session.copy(progress = newProgress)
                            }
                        } else {
                            session
                        }
                    }
                }
            }
        }
    }

    fun createSession(filenames: List<String>, context: android.content.Context) {
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = format.format(Date())
        val title = if (filenames.size == 1) filenames.first() else context.getString(R.string.import_n_files, filenames.size)
        
        val files = filenames.map { name ->
            val ext = name.substringAfterLast('.', "")
            ImportFile(name, ext, 0)
        }
        
        val newSession = ImportSession(
            id = UUID.randomUUID().toString(),
            title = title,
            date = dateStr,
            status = ImportSessionStatus.PENDING,
            files = files,
            recognizedCodes = generateMockCodes(50, files.size)
        )
        _sessions.update { listOf(newSession) + it }
    }

    fun startInventory(id: String) {
        _sessions.update { currentList ->
            currentList.map { session ->
                if (session.id == id) {
                    val mockScanned = session.recognizedCodes.take(session.recognizedCodes.size / 2).map { 
                        ImportScannedCode(it.normalizedCode, System.currentTimeMillis() - (0..100000).random()) 
                    } + listOf(
                        ImportScannedCode("010460123456789021UNKNOWN123", System.currentTimeMillis())
                    )
                    session.copy(type = ImportSessionType.INVENTORY, scannedCodes = mockScanned)
                } else {
                    session
                }
            }
        }
    }

    fun scanCode(id: String, code: String) {
        _sessions.update { currentList ->
            currentList.map { session ->
                if (session.id == id) {
                    val newScan = ImportScannedCode(code, System.currentTimeMillis())
                    session.copy(scannedCodes = session.scannedCodes + newScan)
                } else {
                    session
                }
            }
        }
    }

    fun simulateInventoryScan(id: String) {
        _sessions.update { currentList ->
            currentList.map { session ->
                if (session.id == id) {
                    val codeToScan = if (session.recognizedCodes.isNotEmpty()) session.recognizedCodes.random().normalizedCode else "010460123456789021NEW${System.currentTimeMillis()}"
                    val newScan = ImportScannedCode(codeToScan, System.currentTimeMillis())
                    session.copy(scannedCodes = session.scannedCodes + newScan)
                } else {
                    session
                }
            }
        }
    }
}
