package com.example

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import androidx.lifecycle.viewModelScope
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

enum class PdfSessionStatus(val displayName: String) {
    PENDING("Ожидает"),
    IN_PROGRESS("В процессе"),
    COMPLETED("Завершено"),
    FAILED("Ошибка")
}

data class PdfFile(
    val name: String,
    val pages: Int,
    val codesCount: Int
)

data class RecognizedCode(
    val fullCode: String,
    val normalizedCode: String,
    val gtin: String,
    val fileIndex: Int = 0,
    val pageNumber: Int = 1
)

enum class PdfSessionType {
    RECOGNITION,
    INVENTORY
}

data class ScannedCode(
    val normalizedCode: String,
    val timestamp: Long
)

data class PdfSession(
    val id: String,
    val title: String,
    val date: String,
    val status: PdfSessionStatus,
    val type: PdfSessionType = PdfSessionType.RECOGNITION,
    val progress: Float = 0f, // 0.0 to 1.0
    val totalCodes: Int = 0,
    val files: List<PdfFile> = emptyList(),
    val recognizedCodes: List<RecognizedCode> = emptyList(),
    val scannedCodes: List<ScannedCode> = emptyList()
)

class PdfSessionViewModel : ViewModel() {
    private fun generateMockCodes(count: Int, numFiles: Int = 1): List<RecognizedCode> {
        val gtins = listOf("04601234567890", "04609876543210", "04601111222233")
        return (1..count).map {
            val gtin = gtins.random()
            val serial = java.util.UUID.randomUUID().toString().take(13).replace("-", "A")
            val fullCode = "\u00E801${gtin}21${serial}\u001D91ABCD\u001D921234567890"
            val normalizedCode = "01${gtin}21${serial}"
            val fileIndex = if (numFiles > 0) (0 until numFiles).random() else 0
            val pageNumber = (1..5).random()
            RecognizedCode(fullCode, normalizedCode, gtin, fileIndex, pageNumber)
        }
    }

    private val _sessions = MutableStateFlow<List<PdfSession>>(
        listOf(
            PdfSession("1", "Сессия из 7 файлов", "24.06.2026 10:15", PdfSessionStatus.COMPLETED, PdfSessionType.RECOGNITION, 1f, 150, 
                files = listOf(
                    PdfFile("Накладная_1.pdf", 5, 20),
                    PdfFile("Счет_12.pdf", 2, 10),
                    PdfFile("Документ_A.pdf", 10, 40),
                    PdfFile("Документ_B.pdf", 3, 15),
                    PdfFile("Акт_1.pdf", 1, 5),
                    PdfFile("Акт_2.pdf", 1, 10),
                    PdfFile("ТТН_99.pdf", 12, 50)
                ),
                recognizedCodes = generateMockCodes(150, 7)
            ),
            PdfSession("2", "Счет-фактура_12.pdf", "24.06.2026 11:30", PdfSessionStatus.IN_PROGRESS, PdfSessionType.RECOGNITION, 0.45f, 0,
                files = listOf(PdfFile("Счет-фактура_12.pdf", 2, 0))
            )
        )
    )
    val sessions: StateFlow<List<PdfSession>> = _sessions.asStateFlow()

    fun getSessionById(id: String): StateFlow<PdfSession?> {
        val flow = MutableStateFlow<PdfSession?>(null)
        viewModelScope.launch {
            sessions.collect { list ->
                flow.value = list.find { it.id == id }
            }
        }
        return flow.asStateFlow()
    }

    init {
        // Mock progress update removed, now using actual PDF extraction
    }

    fun createSession(uris: List<android.net.Uri>, filenames: List<String>, context: android.content.Context) {
        val format = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
        val dateStr = format.format(Date())
        val title = if (filenames.size == 1) filenames.first() else "Сессия из ${filenames.size} файлов"
        val sessionId = UUID.randomUUID().toString()
        val pdfFiles = filenames.map { PdfFile(it, 1, 0) } // We don't have page counts yet without parsing
        
        val newSession = PdfSession(
            id = sessionId,
            title = title,
            date = dateStr,
            status = PdfSessionStatus.PENDING,
            files = pdfFiles
        )
        _sessions.update { listOf(newSession) + it }
        
        viewModelScope.launch {
            _sessions.update { currentList ->
                currentList.map { if (it.id == sessionId) it.copy(status = PdfSessionStatus.IN_PROGRESS, progress = 0.1f) else it }
            }
            
            val allExtractedCodes = mutableListOf<String>()
            
            for (i in uris.indices) {
                val uri = uris[i]
                val extracted = PdfBarcodeExtractor.extractBarcodes(context, uri)
                allExtractedCodes.addAll(extracted)
                
                val currentProgress = 0.1f + (0.8f * (i + 1) / uris.size)
                _sessions.update { currentList ->
                    currentList.map { if (it.id == sessionId) it.copy(progress = currentProgress) else it }
                }
            }
            
            val recognized = allExtractedCodes.distinct().map { fullCode ->
                val gtin = if (fullCode.contains("01") && fullCode.length > 16) fullCode.substringAfter("01").take(14) else "UNKNOWN"
                val normalized = if (fullCode.contains("21")) {
                    val s1 = fullCode.substringAfter("01").take(14)
                    val s2 = fullCode.substringAfter("21").takeWhile { it != '\u001D' }
                    "01${s1}21${s2}"
                } else fullCode
                
                RecognizedCode(fullCode, normalized, gtin, 0, 1)
            }
            
            _sessions.update { currentList ->
                currentList.map { if (it.id == sessionId) it.copy(
                    status = PdfSessionStatus.COMPLETED, 
                    progress = 1f,
                    totalCodes = recognized.size,
                    recognizedCodes = recognized
                ) else it }
            }
        }
    }

    fun startInventory(id: String) {
        _sessions.update { currentList ->
            currentList.map { session ->
                if (session.id == id) {
                    // Generate some mock scanned codes to demonstrate the inventory feature
                    val mockScanned = session.recognizedCodes.take(session.recognizedCodes.size / 2).map { 
                        ScannedCode(it.normalizedCode, System.currentTimeMillis() - (0..100000).random()) 
                    } + listOf(
                        ScannedCode("010460123456789021UNKNOWN123", System.currentTimeMillis()) // unknown code
                    )
                    session.copy(type = PdfSessionType.INVENTORY, scannedCodes = mockScanned)
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
                    val newScan = ScannedCode(code, System.currentTimeMillis())
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
                    val newScan = ScannedCode(codeToScan, System.currentTimeMillis())
                    session.copy(scannedCodes = session.scannedCodes + newScan)
                } else {
                    session
                }
            }
        }
    }
}
