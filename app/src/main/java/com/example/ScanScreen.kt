package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.unit.dp
import androidx.compose.animation.AnimatedVisibility

import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanScreen(
    scanViewModel: ScanSessionViewModel = viewModel(),
    productsViewModel: ProductsViewModel = viewModel(),
    accountingViewModel: AccountingViewModel = viewModel(),
    settingsViewModel: SettingsViewModel = viewModel(),
    initialSessionId: String? = null,
    onNavigateToProduct: (String, String) -> Unit = { _, _ -> }
) {
    var selectedScanner by remember { mutableStateOf("Камера") }
    val scanners = listOf("Камера", "HID-сканер", "USB-COM")
    
    LaunchedEffect(initialSessionId) {
        if (initialSessionId != null) {
            scanViewModel.selectSession(initialSessionId)
        } else {
            val latestSession = scanViewModel.sessions.value.firstOrNull()
            if (latestSession != null && !accountingViewModel.sessionAccountingStatus.value.containsKey(latestSession.id)) {
                scanViewModel.selectSession(latestSession.id)
            } else {
                scanViewModel.createNewSession()
            }
        }
    }
    
    val scannedCodes by scanViewModel.scannedCodes.collectAsState()
    
    var selectedTab by remember { mutableStateOf(0) }
    val tabs = listOf("Общий список", "По товарам")

    var showAccountingDialog by remember { mutableStateOf(false) }
    var showCancelAccountingDialog by remember { mutableStateOf(false) }

    val accountingStatus by accountingViewModel.sessionAccountingStatus.collectAsState()
    val currentId = scanViewModel.currentSessionId.collectAsState().value
    val currentAccounting = currentId?.let { accountingStatus[it] }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Сканирование", fontWeight = FontWeight.Black) 
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
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (currentAccounting == null) {
                com.example.ui.components.ScannerSelectionBlock(
                    settingsViewModel = settingsViewModel,
                    onSimulateScan = {
                        val dummy = listOf("010460123456789021ABCD123", "010469876543210921XYZ789", "010460123456789021NEW111")
                        scanViewModel.addCode(dummy.random()) 
                    },
                    onBarcodeScanned = { code ->
                        scanViewModel.addCode(code)
                    }
                )
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
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
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) }
                    )
                }
            }

            if (selectedTab == 0) {
                ScanFlatCodesList(scannedCodes, onNavigateToProduct, productsViewModel, modifier = Modifier.weight(1f))
            } else {
                ScanGroupedCodesList(scannedCodes, onNavigateToProduct, productsViewModel, modifier = Modifier.weight(1f))
            }
            
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                
                Button(
                    onClick = { scanViewModel.createNewSession() },
                    modifier = Modifier.weight(1f).height(56.dp),
                    shape = MaterialTheme.shapes.small,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer)
                ) {
                    Text("НОВАЯ", fontWeight = FontWeight.Black)
                }

                if (currentAccounting != null) {
                    SuggestionChip(
                        onClick = { },
                        label = { Text("УЧТЕНО", fontWeight = FontWeight.Black) },
                        modifier = Modifier.weight(1f).height(56.dp),
                        colors = SuggestionChipDefaults.suggestionChipColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer)
                    )
                } else {
                    Button(
                        onClick = { showAccountingDialog = true },
                        modifier = Modifier.weight(1f).height(56.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiary)
                    ) {
                        Text("УЧЕТ", fontWeight = FontWeight.Black)
                    }
                }
                
                var showExportMenu by remember { mutableStateOf(false) }
                
                Box(modifier = Modifier.weight(1.5f)) {
                    Button(
                        onClick = { showExportMenu = true },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.small,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("ЭКСПОРТ", fontWeight = FontWeight.Black, maxLines = 1)
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
        }
    }
    
    val currentSessionId by scanViewModel.currentSessionId.collectAsState()
    
    if (showAccountingDialog && scannedCodes.isNotEmpty() && currentSessionId != null) {
        com.example.ui.components.AccountingVerificationDialog(
            codes = scannedCodes.map { it.normalizedCode },
            sessionId = currentSessionId!!,
            sessionName = "Свободное сканирование",
            accountingViewModel = accountingViewModel,
            onDismiss = { showAccountingDialog = false }
        )
    } else if (showAccountingDialog) {
        showAccountingDialog = false
    }
    
    if (showCancelAccountingDialog && currentId != null) {
        com.example.ui.components.CancelAccountingDialog(
            onConfirm = {
                accountingViewModel.cancelAccounting(currentId)
                showCancelAccountingDialog = false
            },
            onDismiss = { showCancelAccountingDialog = false }
        )
    }
}

@Composable
fun ScanFlatCodesList(codes: List<FreeScanCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(codes) { code ->
            ScanCodeItemCard(code, onNavigateToProduct, productsViewModel)
        }
    }
}

@Composable
fun ScanGroupedCodesList(codes: List<FreeScanCode>, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel, modifier: Modifier = Modifier) {
    val grouped = remember(codes) { codes.groupBy { try { it.normalizedCode.substring(2, 16) } catch (e: Exception) { "UNKNOWN" } } }
    
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        grouped.forEach { (gtin, gtinCodes) ->
            item(key = "header_$gtin") {
                var expanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                    onClick = { expanded = !expanded }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        val product = productsViewModel.getProductByGtin(gtin).collectAsState(initial = null).value
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("GTIN: $gtin", fontWeight = FontWeight.Bold)
                                if (product != null) {
                                    Text(product.product.name, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                                } else {
                                    Text("Товар не найден", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                                Text("Кодов: ${gtinCodes.size}", style = MaterialTheme.typography.bodySmall)
                            }
                            Icon(if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore, contentDescription = null)
                        }
                        
                        AnimatedVisibility(visible = expanded) {
                            Column(modifier = Modifier.padding(top = 8.dp)) {
                                Button(
                                    onClick = { onNavigateToProduct(gtin, if (product != null) "view" else "create") },
                                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                                ) {
                                    Text(if (product != null) "Открыть товар" else "Добавить товар")
                                }
                                gtinCodes.forEach { code ->
                                    ScanHighlightedNormalizedCode(code)
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
fun ScanCodeItemCard(code: FreeScanCode, onNavigateToProduct: (String, String) -> Unit, productsViewModel: ProductsViewModel) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth().clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            ScanHighlightedNormalizedCode(code)
            
            AnimatedVisibility(visible = expanded) {
                Column {
                    val gtin = try { code.normalizedCode.substring(2, 16) } catch (e: Exception) { "" }
                    if (gtin.isNotEmpty()) {
                        val product = productsViewModel.getProductByGtin(gtin).collectAsState(initial = null).value
                        if (product != null) {
                            Text("Товар: ${product.product.name}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 8.dp))
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

@Composable
fun ScanHighlightedNormalizedCode(code: FreeScanCode) {
    val prefix = "01"
    val gtin = if (code.normalizedCode.length >= 16) code.normalizedCode.substring(2, 16) else ""
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
}
