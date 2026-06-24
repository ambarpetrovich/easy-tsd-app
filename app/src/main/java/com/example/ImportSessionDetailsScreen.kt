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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSessionDetailsScreen(
    sessionId: String,
    onBack: () -> Unit,
    onNavigateToProduct: (String, String) -> Unit,
    viewModel: ImportSessionViewModel,
    productsViewModel: ProductsViewModel,
    accountingViewModel: AccountingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    settingsViewModel: SettingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
) {
    val session by viewModel.getSessionById(sessionId).collectAsState()
    val accountingStatus by accountingViewModel.sessionAccountingStatus.collectAsState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var showFiles by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableIntStateOf(0) } // 0 - flat, 1 - grouped
    var showCancelAccountingDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(session?.title ?: "Детали импорта", fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (session?.status == ImportSessionStatus.COMPLETED) {
                        var showExportMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showExportMenu = true }) {
                                Icon(Icons.Default.FileDownload, contentDescription = "Экспорт")
                            }
                            DropdownMenu(
                                expanded = showExportMenu,
                                onDismissRequest = { showExportMenu = false }
                            ) {
                                DropdownMenuItem(text = { Text("В CSV") }, onClick = { 
                                    showExportMenu = false
                                    session?.let { 
                                        coroutineScope.launch { ExportService.exportToCsv(context, it, "export_csv") }
                                    }
                                })
                                DropdownMenuItem(text = { Text("В XLSX") }, onClick = { 
                                    showExportMenu = false
                                    session?.let { 
                                        coroutineScope.launch { ExportService.exportToXlsxLike(context, it, "export_xlsx") }
                                    }
                                })
                                DropdownMenuItem(text = { Text("В JSON") }, onClick = { 
                                    showExportMenu = false
                                    session?.let { 
                                        coroutineScope.launch { ExportService.exportToJson(context, it, "export_json") }
                                    }
                                })
                                DropdownMenuItem(text = { Text("В XML-УПД") }, onClick = { 
                                    showExportMenu = false
                                    session?.let { 
                                        coroutineScope.launch { ExportService.exportToXmlUpd(context, it, "export_upd") }
                                    }
                                })
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
                            if (session!!.status == ImportSessionStatus.COMPLETED) {
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
                                                    val labelType = if (file.type == "xml" || file.type == "json") "элементов" else "строк"
                                                    Text("${file.itemsCount} $labelType", style = MaterialTheme.typography.bodySmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (session!!.type == ImportSessionType.INVENTORY) {
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

                if (session!!.status == ImportSessionStatus.COMPLETED && session!!.type == ImportSessionType.RECOGNITION) {
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
                } else if (session!!.type == ImportSessionType.INVENTORY) {
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

                if (session!!.status == ImportSessionStatus.IN_PROGRESS || session!!.status == ImportSessionStatus.PENDING) {
                    Spacer(modifier = Modifier.height(32.dp))
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(progress = { session!!.progress })
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Обработка... ${(session!!.progress * 100).toInt()}%")
                    }
                }

                if (showAccountingDialog && session!!.recognizedCodes.isNotEmpty()) {
                    val codesToAccount = if (session!!.type == ImportSessionType.INVENTORY) session!!.scannedCodes.map { it.normalizedCode } else session!!.recognizedCodes.map { it.normalizedCode }
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

                if (session!!.status == ImportSessionStatus.COMPLETED && session!!.recognizedCodes.isNotEmpty()) {
                    if (session!!.type == ImportSessionType.INVENTORY && currentAccounting == null) {
                        com.example.ui.components.ScannerSelectionBlock(
                            settingsViewModel = settingsViewModel,
                            onSimulateScan = {
                                viewModel.simulateInventoryScan(sessionId)
                            },
                            onBarcodeScanned = { code ->
                                viewModel.scanCode(sessionId, code)
                            }
                        )
                    } else if (session!!.type == ImportSessionType.INVENTORY && currentAccounting != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "Учтено: ${currentAccounting.displayName}",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                TextButton(onClick = { showCancelAccountingDialog = true }) {
                                    Text("Отменить")
                                }
                            }
                        }
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
                        if (session!!.type == ImportSessionType.INVENTORY) {
                            Tab(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                text = { Text("Отсканированные") }
                            )
                        }
                    }

                    if (selectedTab == 0) {
                        ImportFlatCodesList(session!!.recognizedCodes, session!!.scannedCodes, onNavigateToProduct, productsViewModel, session!!.files)
                    } else if (selectedTab == 1) {
                        ImportGroupedCodesList(session!!.recognizedCodes, session!!.scannedCodes, onNavigateToProduct, productsViewModel, session!!.files)
                    } else if (selectedTab == 2) {
                        ImportScannedCodesList(session!!.scannedCodes, onNavigateToProduct, productsViewModel)
                    }
                }
            }
        }
    }

    if (showCancelAccountingDialog) {
        com.example.ui.components.CancelAccountingDialog(
            onConfirm = {
                accountingViewModel.cancelAccounting(sessionId)
                showCancelAccountingDialog = false
            },
            onDismiss = { showCancelAccountingDialog = false }
        )
    }
}

@Composable
fun ImportScannedCodesList(codes: List<ImportScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(codes.reversed()) { code -> 
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
fun ImportFlatCodesList(codes: List<ImportRecognizedCode>, scannedCodes: List<ImportScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<ImportFile>) {
    val recognizedNormalized = codes.map { it.normalizedCode }.toSet()
    val unknownScanned = scannedCodes.filter { it.normalizedCode !in recognizedNormalized }.distinctBy { it.normalizedCode }
    
    val allCodes = codes + unknownScanned.map { 
        ImportRecognizedCode(
            fullCode = it.normalizedCode, 
            normalizedCode = it.normalizedCode,
            gtin = it.normalizedCode.substring(2, 16),
            fileIndex = -1,
            itemIndex = -1
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(allCodes) { code ->
            val scans = scannedCodes.filter { it.normalizedCode == code.normalizedCode }
            ImportCodeItemCard(code, scans, onNavigateToProduct, productsViewModel, files)
        }
    }
}

@Composable
fun ImportGroupedCodesList(codes: List<ImportRecognizedCode>, scannedCodes: List<ImportScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<ImportFile>) {
    val recognizedNormalized = codes.map { it.normalizedCode }.toSet()
    val unknownScanned = scannedCodes.filter { it.normalizedCode !in recognizedNormalized }.distinctBy { it.normalizedCode }
    
    val allCodes = codes + unknownScanned.map { 
        val gtin = try { it.normalizedCode.substring(2, 16) } catch (e: Exception) { "UNKNOWN" }
        ImportRecognizedCode(
            fullCode = it.normalizedCode, 
            normalizedCode = it.normalizedCode,
            gtin = gtin,
            fileIndex = -1,
            itemIndex = -1
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
                                    ImportHighlightedNormalizedCode(code, scans, files)
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
fun ImportCodeItemCard(code: ImportRecognizedCode, scans: List<ImportScannedCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, files: List<ImportFile>) {
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
            ImportHighlightedNormalizedCode(code, scans, files)
            
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

@Composable
fun ImportHighlightedNormalizedCode(code: ImportRecognizedCode, scans: List<ImportScannedCode>, files: List<ImportFile>) {
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
                    text = "${code.itemIndex}",
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
        val file = files.getOrNull(code.fileIndex)
        val fileName = file?.name ?: "Unknown"
        val itemTypeLabel = if (file?.type == "xml" || file?.type == "json") "Индекс" else "Строка"
        
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
                    Text("$itemTypeLabel: ${code.itemIndex}")
                }
            }
        )
    }
}
