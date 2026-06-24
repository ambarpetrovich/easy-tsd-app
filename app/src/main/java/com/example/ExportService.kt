package com.example

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Xml
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ExportService {

    suspend fun exportToCsv(context: Context, session: Any, fileNamePrefix: String) {
        withContext(Dispatchers.IO) {
            val file = createTempFile(context, fileNamePrefix, "csv")
            val codes = extractCodesFromSession(session)
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, "UTF-8").use { writer ->
                    // Add BOM for Excel UTF-8 compatibility
                    fos.write(byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()))
                    
                    writer.write("Штрихкод;Нормализованный КИЗ;GTIN\n")
                    codes.forEach { code ->
                        writer.write("${code.fullCode};${code.normalized};${code.gtin}\n")
                    }
                }
            }
            shareFile(context, file, "text/csv")
        }
    }

    suspend fun exportToJson(context: Context, session: Any, fileNamePrefix: String) {
        withContext(Dispatchers.IO) {
            val file = createTempFile(context, fileNamePrefix, "json")
            val codes = extractCodesFromSession(session)
            
            val jsonArray = JSONArray()
            codes.forEach { code ->
                val jsonObj = JSONObject()
                jsonObj.put("fullCode", code.fullCode)
                jsonObj.put("normalized", code.normalized)
                jsonObj.put("gtin", code.gtin)
                jsonArray.put(jsonObj)
            }
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, "UTF-8").use { writer ->
                    writer.write(jsonArray.toString(4))
                }
            }
            shareFile(context, file, "application/json")
        }
    }

    suspend fun exportToXmlUpd(context: Context, session: Any, fileNamePrefix: String) {
        withContext(Dispatchers.IO) {
            val file = createTempFile(context, fileNamePrefix, "xml")
            val codes = extractCodesFromSession(session)
            
            FileOutputStream(file).use { fos ->
                val serializer = Xml.newSerializer()
                serializer.setOutput(fos, "UTF-8")
                serializer.startDocument("UTF-8", true)
                serializer.startTag(null, "Файл")
                
                serializer.startTag(null, "Документ")
                serializer.startTag(null, "СвСчФакт")
                
                var rowNumber = 1
                codes.groupBy { it.gtin }.forEach { (gtin, groupedCodes) ->
                    serializer.startTag(null, "СведТов")
                    serializer.attribute(null, "НомТек", rowNumber.toString())
                    serializer.attribute(null, "НаимТов", "Товар GTIN $gtin")
                    serializer.attribute(null, "КолТов", groupedCodes.size.toString())
                    
                    groupedCodes.forEach { code ->
                        serializer.startTag(null, "КИЗ")
                        serializer.text(code.normalized)
                        serializer.endTag(null, "КИЗ")
                    }
                    
                    serializer.endTag(null, "СведТов")
                    rowNumber++
                }
                
                serializer.endTag(null, "СвСчФакт")
                serializer.endTag(null, "Документ")
                serializer.endTag(null, "Файл")
                serializer.endDocument()
            }
            shareFile(context, file, "application/xml")
        }
    }

    // A simple XML Spreadsheet for Excel (XLSX simulation)
    suspend fun exportToXlsxLike(context: Context, session: Any, fileNamePrefix: String) {
        withContext(Dispatchers.IO) {
            val file = createTempFile(context, fileNamePrefix, "xml") // using xml extension but with Excel XML format
            val codes = extractCodesFromSession(session)
            
            FileOutputStream(file).use { fos ->
                OutputStreamWriter(fos, "UTF-8").use { writer ->
                    writer.write("<?xml version=\"1.0\"?>\n")
                    writer.write("<Workbook xmlns=\"urn:schemas-microsoft-com:office:spreadsheet\"\n")
                    writer.write(" xmlns:o=\"urn:schemas-microsoft-com:office:office\"\n")
                    writer.write(" xmlns:x=\"urn:schemas-microsoft-com:office:excel\"\n")
                    writer.write(" xmlns:ss=\"urn:schemas-microsoft-com:office:spreadsheet\">\n")
                    writer.write(" <Worksheet ss:Name=\"Sheet1\">\n")
                    writer.write("  <Table>\n")
                    writer.write("   <Row>\n")
                    writer.write("    <Cell><Data ss:Type=\"String\">Штрихкод</Data></Cell>\n")
                    writer.write("    <Cell><Data ss:Type=\"String\">Нормализованный КИЗ</Data></Cell>\n")
                    writer.write("    <Cell><Data ss:Type=\"String\">GTIN</Data></Cell>\n")
                    writer.write("   </Row>\n")
                    
                    codes.forEach { code ->
                        writer.write("   <Row>\n")
                        writer.write("    <Cell><Data ss:Type=\"String\">${code.fullCode}</Data></Cell>\n")
                        writer.write("    <Cell><Data ss:Type=\"String\">${code.normalized}</Data></Cell>\n")
                        writer.write("    <Cell><Data ss:Type=\"String\">${code.gtin}</Data></Cell>\n")
                        writer.write("   </Row>\n")
                    }
                    
                    writer.write("  </Table>\n")
                    writer.write(" </Worksheet>\n")
                    writer.write("</Workbook>\n")
                }
            }
            shareFile(context, file, "application/vnd.ms-excel")
        }
    }

    private fun extractCodesFromSession(session: Any): List<ExportCode> {
        return when (session) {
            is PdfSession -> {
                // If inventory, take scanned codes, else recognized codes
                if (session.scannedCodes.isNotEmpty()) {
                    session.scannedCodes.map { 
                        ExportCode(it.normalizedCode, it.normalizedCode, if (it.normalizedCode.contains("01")) it.normalizedCode.substringAfter("01").take(14) else "UNKNOWN") 
                    }
                } else {
                    session.recognizedCodes.map { ExportCode(it.fullCode, it.normalizedCode, it.gtin) }
                }
            }
            is ImportSession -> {
                if (session.scannedCodes.isNotEmpty()) {
                    session.scannedCodes.map { 
                        ExportCode(it.normalizedCode, it.normalizedCode, if (it.normalizedCode.contains("01")) it.normalizedCode.substringAfter("01").take(14) else "UNKNOWN") 
                    }
                } else {
                    session.recognizedCodes.map { ExportCode(it.fullCode, it.normalizedCode, it.gtin) }
                }
            }
            else -> emptyList()
        }
    }

    private fun createTempFile(context: Context, prefix: String, extension: String): File {
        val dateFormat = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
        val timestamp = dateFormat.format(Date())
        val fileName = "${prefix}_${timestamp}.$extension"
        val dir = File(context.cacheDir, "exports")
        if (!dir.exists()) dir.mkdirs()
        return File(dir, fileName)
    }

    private fun shareFile(context: Context, file: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, "Экспорт файла")
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(chooser)
    }
}

data class ExportCode(
    val fullCode: String,
    val normalized: String,
    val gtin: String
)
