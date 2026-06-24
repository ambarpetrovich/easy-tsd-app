package com.example

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ActionCard
import com.example.ui.components.EmptyState
import com.example.ui.components.SectionHeader
import com.example.ui.components.SessionCard

import androidx.lifecycle.viewmodel.compose.viewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    accountingViewModel: AccountingViewModel = viewModel(),
    pdfSessionViewModel: PdfSessionViewModel = viewModel(),
    importSessionViewModel: ImportSessionViewModel = viewModel(),
    onNavigateToScan: () -> Unit = {},
    onNavigateToPdfSession: (String) -> Unit = {},
    onNavigateToImportSession: (String) -> Unit = {}
) {
    val accountingStatus by accountingViewModel.sessionAccountingStatus.collectAsState()
    val pdfSessions by pdfSessionViewModel.sessions.collectAsState()
    val importSessions by importSessionViewModel.sessions.collectAsState()
    
    var showFeatureInfo by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("EasyTSD", fontWeight = FontWeight.Black) 
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                modifier = Modifier.fillMaxWidth().clickable { showFeatureInfo = true },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = MaterialTheme.shapes.medium
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    Column {
                        Text("Умная приемка и отгрузка", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Проверка и учет КИЗов. Сверка и инвентаризация по любым файлам (PDF, XML, Excel).", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }

            ActionCard(
                title = "Свободное сканирование",
                description = "Сканируйте штрих-коды без привязки к документу.",
                icon = Icons.Default.QrCode,
                onClick = onNavigateToScan
            )
            
            SectionHeader(title = "Предыдущие сессии")
            
            val dateFormat = SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
            
            if (pdfSessions.isEmpty() && importSessions.isEmpty()) {
                EmptyState(
                    title = "Нет сессий",
                    subtitle = "Создайте новую сессию из файлов или начните свободное сканирование"
                )
            } else {
                pdfSessions.sortedByDescending { 
                    try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
                }.forEach { session ->
                    Card(modifier = Modifier.clickable { onNavigateToPdfSession(session.id) }) {
                        SessionCard(
                            title = session.title, 
                            date = session.date, 
                            type = if (session.type == PdfSessionType.RECOGNITION) "Распознавание PDF" else "Инвентаризация PDF", 
                            codesCount = session.totalCodes,
                            accountingType = accountingStatus[session.id]
                        )
                    }
                }
                importSessions.sortedByDescending { 
                    try { dateFormat.parse(it.date)?.time ?: 0L } catch (e: Exception) { 0L }
                }.forEach { session ->
                    Card(modifier = Modifier.clickable { onNavigateToImportSession(session.id) }) {
                        SessionCard(
                            title = session.title, 
                            date = session.date, 
                            type = if (session.type == ImportSessionType.RECOGNITION) "Импорт данных" else "Инвентаризация (Импорт)", 
                            codesCount = session.totalCodes,
                            accountingType = accountingStatus[session.id]
                        )
                    }
                }
            }
        }
        
        if (showFeatureInfo) {
            AlertDialog(
                onDismissRequest = { showFeatureInfo = false },
                title = { Text("Как это работает?", fontWeight = FontWeight.Bold) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text("1. Загрузите файлы с кодами маркировки (PDF, Excel, XML, CSV).")
                        Text("2. Сверьте данные сканированием с помощью камеры или сканера штрихкодов.")
                        Text("3. Примите отсканированное к учету (Приёмка/Отгрузка) для будущих проверок и контроля остатков.")
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showFeatureInfo = false }) {
                        Text("Понятно")
                    }
                }
            )
        }
    }
}


