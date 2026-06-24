package com.example

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle

val fileColors = listOf(
    Color(0xFFE57373), Color(0xFF81C784), Color(0xFF64B5F6), 
    Color(0xFFFFD54F), Color(0xFFBA68C8), Color(0xFF4DB6AC),
    Color(0xFFFF8A65), Color(0xFFAED581), Color(0xFF7986CB), Color(0xFF4DD0E1)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSessionDetailsScreen(
    sessionId: String,
    onBack: () -> Unit,
    onNavigateToProduct: (String, String) -> Unit,
    viewModel: PdfSessionViewModel,
    productsViewModel: ProductsViewModel,
    accountingViewModel: AccountingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val session by viewModel.getSessionById(sessionId).collectAsState()
    val accountingStatus by accountingViewModel.sessionAccountingStatus.collectAsState()

    var showFiles by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 - flat, 1 - grouped

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Детали сессии", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (session?.status == PdfSessionStatus.COMPLETED) {
                        var showExportMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Экспорт")
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("В CSV") }, onClick = { showExportMenu = false })
                                DropdownMenuItem(text = { Text("В XLSX") }, onClick = { showExportMenu = false })
                                DropdownMenuItem(text = { Text("В XML-УПД") }, onClick = { showExportMenu = false })
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary,
                    actionIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    ) { innerPadding ->
        if (session == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Files Spoiler and Status block
                @Composable
                fun SourceDataBlock() {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Статус: ${session!!.status.displayName}")
                            if (session!!.status == PdfSessionStatus.COMPLETED) {
                                Text("Распознано: ${session!!.totalCodes} кодов", fontWeight = FontWeight.Bold)
                            }
                        }

                        if (session!!.files.isNotEmpty()) {
                            val useSpoiler = session!!.files.size > 5
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 8.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Column {
                                    if (useSpoiler) {
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clickable { showFiles = !showFiles }
                                                .padding(16.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text("${session!!.files.size} файлов", fontWeight = FontWeight.Bold)
                                            Icon(if (showFiles) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                                        }
                                    } else {
                                        Box(modifier = Modifier.padding(16.dp)) {
                                            Text("Файлы сессии:", fontWeight = FontWeight.Bold)
                                        }
                                    }
                                    
                                    AnimatedVisibility(visible = !useSpoiler || showFiles) {
                                        Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 16.dp)) {
                                            session!!.files.forEachIndexed { index, file ->
                                                Row(
                                                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                                    horizontalArrangement = Arrangement.SpaceBetween
                                                ) {
                                                    Text(
                                                        text = file.name, 
                                                        modifier = Modifier.weight(1f),
                                                        color = fileColors[index % fileColors.size],
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text("${file.pages} стр., ${file.codesCount} кодов", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (session!!.type == PdfSessionType.INVENTORY) {
                    var showSourceData by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { showSourceData = !showSourceData }
                                    .padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Исходные данные", fontWeight = FontWeight.Bold)
                                Icon(if (showSourceData) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                            }
                            AnimatedVisibility(visible = showSourceData) {
                                SourceDataBlock()
                            }
                        }
                    }
                } else {
                    SourceDataBlock()
                }

                var showAccountingDialog by remember { mutableStateOf(false) }
                val currentAccounting = accountingStatus[sessionId]

                if (session!!.status == PdfSessionStatus.COMPLETED && session!!.type == PdfSessionType.RECOGNITION) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { viewModel.startInventory(sessionId) },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("Сверка")
                        }
                        if (currentAccounting != null) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(currentAccounting.displayName, fontWeight = FontWeight.Bold) },
                                modifier = Modifier.weight(1f),
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        } else {
                            Button(
                                onClick = { showAccountingDialog = true },
                                modifier = Modifier.weight(1f),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Учет")
                            }
                        }
                    }
                } else if (session!!.type == PdfSessionType.INVENTORY) {
                    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Режим: Сверка",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        if (currentAccounting != null) {
                            SuggestionChip(
                                onClick = { },
                                label = { Text(currentAccounting.displayName, fontWeight = FontWeight.Bold) },
                                colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                            )
                        } else {
                            Button(
                                onClick = { showAccountingDialog = true },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                            ) {
                                Text("Учет")
                            }
                        }
                    }
                }

                if (session!!.status == PdfSessionStatus.IN_PROGRESS || session!!.status == PdfSessionStatus.PENDING) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(progress = { session!!.progress })
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Распознавание... ${(session!!.progress * 100).toInt()}%")
                    }
                }

                if (showAccountingDialog && session!!.recognizedCodes.isNotEmpty()) {
                    // For accounting, we take all scanned codes if we are in inventory mode, otherwise recognized codes
                    val codesToAccount = if (session!!.type == PdfSessionType.INVENTORY) session!!.scannedCodes.map { it.normalizedCode } else session!!.recognizedCodes.map { it.normalizedCode }
                    if (codesToAccount.isNotEmpty()) {
                        com.example.ui.components.AccountingVerificationDialog(
                            codes = codesToAccount,
                            sessionId = sessionId,
                            sessionName = session!!.title,
                            accountingViewModel = accountingViewModel,
                            onDismiss = { showAccountingDialog = false }
                        )
                    } else {
                        showAccountingDialog = false
                    }
                }

                if (session!!.status == PdfSessionStatus.COMPLETED && session!!.recognizedCodes.isNotEmpty()) {
                    if (session!!.type == PdfSessionType.INVENTORY) {
                        com.example.ui.components.ScannerSelectionBlock(
                            onSimulateScan = {
                                viewModel.simulateInventoryScan(sessionId)
                            }
                        )
                    }

                    TabRow(selectedTabIndex = selectedTab) {
                        Tab(
                            selected = selectedTab == 0,
                            onClick = { selectedTab = 0 },
                            text = { Text("Плоский список") }
                        )
                        Tab(
                            selected = selectedTab == 1,
                            onClick = { selectedTab = 1 },
                            text = { Text("По GTIN") }
                        )
                        if (session!!.type == PdfSessionType.INVENTORY) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Отсканированные") }
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        FlatCodesList(session!!.recognizedCodes, session!!.scannedCodes, onNavigateToProduct, productsViewModel, session!!.files)
                    } else if (selectedTab == 1) {
                        GroupedCodesList(session!!.recognizedCodes, session!!.scannedCodes, onNavigateToProduct, productsViewModel, session!!.files)
                    } else if (selectedTab == 2) {
                        // Just map ScannedCode to FreeScanCode and reuse the UI, or build a simple list.
                        // For simplicity, let's just build it inline or call a small composable.
                        ScannedCodesList(session!!.scannedCodes, onNavigateToProduct, productsViewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun ScannedCodesList(codes: List<ScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(codes.reversed()) { code -> // new ones on top
            var expanded by remember { mutableStateOf(false) }

            Card(
                modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    val prefix = "01"
                    val gtin = try { code.normalizedCode.substring(2, 16) } catch (e: Exception) { "" }
                    val rest = if (code.normalizedCode.length > 16) code.normalizedCode.substringAfter(prefix + gtin) else code.normalizedCode
                    
                    val annotatedString = buildAnnotatedString {
                        if (code.normalizedCode.startsWith(prefix) && code.normalizedCode.length >= 16) {
                            append(prefix)
                            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                                append(gtin)
                            }
                            append(rest)
                        } else {
                            append(code.normalizedCode)
                        }
                    }
                    
                    val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy", java.util.Locale.getDefault()).format(java.util.Date(code.timestamp))
                    val timeStr = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(code.timestamp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = annotatedString, 
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, 
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.weight(1f)
                        )
                        
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(start = 8.dp)) {
                            Text(dateStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(timeStr, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    AnimatedVisibility(visible = expanded) {
                        Column {
                            if (gtin.isNotEmpty()) {
                                val product by productsViewModel.getProductByGtin(gtin).collectAsState(initial = null)
                                if (product != null) {
                                    Text("Товар: ${product!!.product.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
                                    Button(onClick = { onNavigateToProduct(gtin, "view") }, modifier = Modifier.padding(top = 8.dp)) {
                                        Text("Открыть товар")
                                    }
                                } else {
                                    Text("Товар не найден", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.error, modifier = Modifier.padding(top = 8.dp))
                                    Button(onClick = { onNavigateToProduct(gtin, "create") }, modifier = Modifier.padding(top = 8.dp)) {
                                        Text("Добавить товар")
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun FlatCodesList(codes: List<RecognizedCode>, scannedCodes: List<ScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<PdfFile>) {
    // Combine recognized and strictly unknown scanned codes
    val recognizedNormalized = codes.map { it.normalizedCode }.toSet()
    val unknownScanned = scannedCodes.filter { it.normalizedCode !in recognizedNormalized }.distinctBy { it.normalizedCode }
    
    val allCodes = codes + unknownScanned.map { 
        RecognizedCode(
            fullCode = it.normalizedCode, // mock
            normalizedCode = it.normalizedCode,
            gtin = it.normalizedCode.substring(2, 16), // mock extraction
            fileIndex = -1,
            pageNumber = -1
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allCodes) { code ->
            val scans = scannedCodes.filter { it.normalizedCode == code.normalizedCode }
            CodeItemCard(code, scans, onNavigateToProduct, productsViewModel, files)
        }
    }
}

@Composable
fun GroupedCodesList(codes: List<RecognizedCode>, scannedCodes: List<ScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<PdfFile>) {
    val recognizedNormalized = codes.map { it.normalizedCode }.toSet()
    val unknownScanned = scannedCodes.filter { it.normalizedCode !in recognizedNormalized }.distinctBy { it.normalizedCode }
    
    val allCodes = codes + unknownScanned.map { 
        val gtin = try { it.normalizedCode.substring(2, 16) } catch (e: Exception) { "UNKNOWN" }
        RecognizedCode(
            fullCode = it.normalizedCode, 
            normalizedCode = it.normalizedCode,
            gtin = gtin,
            fileIndex = -1,
            pageNumber = -1
        )
    }

    val grouped = remember(allCodes) { allCodes.groupBy { it.gtin } }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (gtin, gtinCodes) ->
            item(key = "header_$gtin") {
                val scannedGtinCount = scannedCodes.count { it.normalizedCode in gtinCodes.map { c -> c.normalizedCode } }
                
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { expanded = !expanded }
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GTIN: $gtin", fontWeight = FontWeight.Bold)
                                val product by productsViewModel.getProductByGtin(gtin).collectAsState(initial = null)
                                if (product != null) {
                                    Text(product!!.product.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("Товар не найден", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                Text("Кодов: ${gtinCodes.size}" + if (scannedCodes.isNotEmpty()) " (Отсканировано: $scannedGtinCount)" else "", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }
                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                val product by productsViewModel.getProductByGtin(gtin).collectAsState(initial = null)
                                Button(
                                    onClick = { onNavigateToProduct(gtin, if (product != null) "edit" else "create") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (product != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                                        contentColor = if (product != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text(if (product != null) "Открыть товар" else "Добавить товар")
                                }
                                gtinCodes.forEach { code ->
                                    val scans = scannedCodes.filter { it.normalizedCode == code.normalizedCode }
                                    HighlightedNormalizedCode(code, scans, files)
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeItemCard(code: RecognizedCode, scans: List<ScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<PdfFile>) {
    var expanded by remember { mutableStateOf(false) }

    val isUnknown = code.fileIndex == -1

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = if (isUnknown) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            HighlightedNormalizedCode(code, scans, files)
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    val product by productsViewModel.getProductByGtin(code.gtin).collectAsState(initial = null)
                    
                    if (product != null) {
                        Column(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                            Text(product!!.product.name, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                            if (product!!.attributes.isNotEmpty()) {
                                Text(
                                    text = product!!.attributes.joinToString(" | ") { "${it.name}: ${it.value}" },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                    
                    Button(
                        onClick = { onNavigateToProduct(code.gtin, if (product != null) "edit" else "create") },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (product != null) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.primary,
                            contentColor = if (product != null) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(if (product != null) "Открыть товар" else "Добавить товар")
                    }
                }
            }
        }
    }
}

fun truncateMiddle(text: String, maxLength: Int = 20): String {
    if (text.length <= maxLength) return text
    val half = (maxLength - 3) / 2
    return text.take(half) + "..." + text.takeLast(maxLength - 3 - half)
}

@Composable
fun HighlightedNormalizedCode(code: RecognizedCode, scans: List<ScannedCode>, files: List<PdfFile>) {
    var showFileInfo by remember { mutableStateOf(false) }
    
    val prefix = "01"
    val gtin = code.gtin
    
    val rest = if (code.normalizedCode.length > 16) {
        code.normalizedCode.substringAfter(prefix + gtin)
    } else {
        code.normalizedCode
    }
    
    val annotatedString = buildAnnotatedString {
        if (code.normalizedCode.startsWith(prefix) && code.normalizedCode.length >= 16) {
            append(prefix)
            withStyle(style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) {
                append(gtin)
            }
            append(rest)
        } else {
            append(code.normalizedCode)
        }
    }
    
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = annotatedString, 
                fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, 
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f)
            )
            
            if (code.fileIndex != -1) {
                Text(
                    text = "${code.pageNumber}",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = fileColors[code.fileIndex % fileColors.size],
                    modifier = Modifier
                        .padding(start = 8.dp)
                        .clickable { showFileInfo = true }
                        .padding(4.dp)
                )
            } else {
                Text(
                    text = "НЕТ В УПД",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }

        if (scans.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text("Отсканирован: ${scans.size} раз", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary)
            scans.forEach { scan ->
                val dateStr = java.text.SimpleDateFormat("dd.MM.yyyy HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date(scan.timestamp))
                Text(text = " - $dateStr", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
    
    if (showFileInfo) {
        val fileName = files.getOrNull(code.fileIndex)?.name ?: "Unknown"
        AlertDialog(
            onDismissRequest = { showFileInfo = false },
            confirmButton = {
                TextButton(onClick = { showFileInfo = false }) { Text("OK") }
            },
            title = { Text("Информация о файле") },
            text = {
                Column {
                    Text("Файл: ${truncateMiddle(fileName, 30)}")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Страница: ${code.pageNumber}")
                }
            }
        )
    }
}
