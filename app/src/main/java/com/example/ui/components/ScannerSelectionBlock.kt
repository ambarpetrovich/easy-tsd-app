package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.UsbComScanner
import kotlinx.coroutines.delay

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScannerSelectionBlock(
    settingsViewModel: SettingsViewModel = viewModel(),
    onSimulateScan: () -> Unit = {},
    onBarcodeScanned: (String) -> Unit = {}
) {
    val defaultScanner by settingsViewModel.defaultScannerMode.collectAsState()
    var selectedScanner by remember(defaultScanner) { mutableStateOf(defaultScanner) }
    val scanners = listOf(androidx.compose.ui.res.stringResource(com.example.R.string.str_12), androidx.compose.ui.res.stringResource(com.example.R.string.str_11), "USB-COM")

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        SingleChoiceSegmentedButtonRow(
            modifier = Modifier.fillMaxWidth()
        ) {
            scanners.forEachIndexed { index, label ->
                SegmentedButton(
                    selected = selectedScanner == label,
                    onClick = { selectedScanner = label },
                    shape = SegmentedButtonDefaults.itemShape(index = index, count = scanners.size)
                ) {
                    Text(label, style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (selectedScanner) {
            androidx.compose.ui.res.stringResource(com.example.R.string.str_12) -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .background(MaterialTheme.colorScheme.surface, shape = MaterialTheme.shapes.medium)
                        .padding(2.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CameraScanner(onBarcodeScanned = { code -> 
                        onBarcodeScanned(code)
                    })
                }
            }
            androidx.compose.ui.res.stringResource(com.example.R.string.str_11) -> {
                HidScannerView(onBarcodeScanned, onSimulateScan)
            }
            "USB-COM" -> {
                UsbComScannerView(onBarcodeScanned, onSimulateScan)
            }
        }
    }
}

@Composable
fun HidScannerView(onBarcodeScanned: (String) -> Unit, onSimulateScan: () -> Unit) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(100) // Small delay to ensure view is ready
        focusRequester.requestFocus()
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.str_10),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.str_9),
                style = MaterialTheme.typography.bodyMedium
            )
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = text,
                onValueChange = {
                    if (it.contains("\n") || it.contains("\r")) {
                        val code = it.replace("\n", "").replace("\r", "").trim()
                        if (code.isNotEmpty()) onBarcodeScanned(code)
                        text = ""
                    } else {
                        text = it
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester)
                    .onKeyEvent { keyEvent ->
                        if (keyEvent.type == KeyEventType.KeyUp && 
                            (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)) {
                            if (text.isNotBlank()) {
                                onBarcodeScanned(text.trim())
                                text = ""
                            }
                            true
                        } else {
                            false
                        }
                    },
                placeholder = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_8)) },
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSimulateScan) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_2)) }
        }
    }
}

@Composable
fun UsbComScannerView(onBarcodeScanned: (String) -> Unit, onSimulateScan: () -> Unit) {
    val context = LocalContext.current
    val usbComScanner = remember { UsbComScanner(context) }
    val initialStatus = androidx.compose.ui.res.stringResource(com.example.R.string.str_7)
    var status by remember { mutableStateOf(initialStatus) }

    LaunchedEffect(usbComScanner) {
        usbComScanner.autoConnect()
    }

    LaunchedEffect(usbComScanner) {
        usbComScanner.statusFlow.collect { newStatus ->
            status = newStatus
        }
    }

    LaunchedEffect(usbComScanner) {
        usbComScanner.barcodeFlow.collect { barcode ->
            onBarcodeScanned(barcode)
        }
    }

    DisposableEffect(usbComScanner) {
        onDispose {
            usbComScanner.destroy()
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    ) {
        Column(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.str_6),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = androidx.compose.ui.res.stringResource(com.example.R.string.str_5, status),
                style = MaterialTheme.typography.bodyMedium,
                color = if (status == androidx.compose.ui.res.stringResource(com.example.R.string.str_4)) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { usbComScanner.autoConnect() }) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_3)) }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = onSimulateScan) { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_2)) }
        }
    }
}

