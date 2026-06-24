package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Article
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material.icons.filled.Inventory
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Usb
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateToHistory: () -> Unit,
    onNavigateToProducts: () -> Unit,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    val tabs = listOf(androidx.compose.ui.res.stringResource(com.example.R.string.str_102), androidx.compose.ui.res.stringResource(com.example.R.string.str_101))

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_100), fontWeight = FontWeight.Black) 
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
                .imePadding()
        ) {
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary,
                indicator = { tabPositions ->
                    if (selectedTab < tabPositions.size) {
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            ) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = { Text(title, fontWeight = FontWeight.Bold) },
                        unselectedContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (selectedTab == 0) {
                    SystemSettingsSection(onNavigateToHistory, settingsViewModel)
                } else {
                    BusinessSettingsSection(onNavigateToProducts)
                }
            }
        }
    }
}

@Composable
fun SystemSettingsSection(onNavigateToHistory: () -> Unit, settingsViewModel: SettingsViewModel = viewModel()) {
    val defaultScanner by settingsViewModel.defaultScannerMode.collectAsState()
    
    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_99),
        icon = Icons.Default.History,
        onClick = onNavigateToHistory
    ) {
        Text(
            text = androidx.compose.ui.res.stringResource(com.example.R.string.str_69),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }

    var showLanguageDropdown by remember { mutableStateOf(false) }
    val currentLangCode by LanguageManager.currentLanguage.collectAsState()
    val context = LocalContext.current
    
    val languageDisplayName = if (currentLangCode == "ru") "Русский" else "English"

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_98),
        icon = Icons.Default.Language
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            OutlinedTextField(
                value = languageDisplayName,
                onValueChange = {},
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                trailingIcon = {
                    IconButton(onClick = { showLanguageDropdown = true }) {
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            DropdownMenu(
                expanded = showLanguageDropdown,
                onDismissRequest = { showLanguageDropdown = false },
                modifier = Modifier.fillMaxWidth(0.9f)
            ) {
                DropdownMenuItem(
                    text = { Text("Русский") },
                    onClick = {
                        LanguageManager.setLanguage(context, "ru")
                        showLanguageDropdown = false
                    }
                )
                DropdownMenuItem(
                    text = { Text("English") },
                    onClick = {
                        LanguageManager.setLanguage(context, "en")
                        showLanguageDropdown = false
                    }
                )
            }
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_96),
        icon = Icons.Default.QrCodeScanner
    ) {
        Column {
            listOf(androidx.compose.ui.res.stringResource(com.example.R.string.str_12), androidx.compose.ui.res.stringResource(com.example.R.string.str_11), "USB-COM").forEach { mode ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { settingsViewModel.setDefaultScannerMode(mode) }
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(mode, fontWeight = FontWeight.Bold)
                    RadioButton(
                        selected = defaultScanner == mode,
                        onClick = { settingsViewModel.setDefaultScannerMode(mode) }
                    )
                }
            }
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_95),
        icon = Icons.Default.Settings
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_94), fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = {})
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_93), fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_6),
        description = androidx.compose.ui.res.stringResource(com.example.R.string.str_92),
        icon = Icons.Default.Usb
    ) {
        UsbComDiagnosticBlock()
    }
}

@Composable
fun BusinessSettingsSection(onNavigateToProducts: () -> Unit) {
    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_91),
        description = androidx.compose.ui.res.stringResource(com.example.R.string.str_90),
        icon = Icons.Default.Link
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_89)) },
                placeholder = { Text("https://api.example.com") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_88)) },
                placeholder = { Text("Token") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                ),
                visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
            )
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_87),
        description = androidx.compose.ui.res.stringResource(com.example.R.string.str_86),
        icon = Icons.Default.Business
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_85)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_84)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_83),
        description = androidx.compose.ui.res.stringResource(com.example.R.string.str_82),
        icon = Icons.Default.Inventory,
        onClick = onNavigateToProducts
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_81), fontWeight = FontWeight.Bold)
                Switch(checked = false, onCheckedChange = {})
            }
            HorizontalDivider()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_80), fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }

    SettingsCard(
        title = androidx.compose.ui.res.stringResource(com.example.R.string.str_79),
        description = androidx.compose.ui.res.stringResource(com.example.R.string.str_78),
        icon = Icons.Default.ImportExport
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "Excel (.xlsx)",
                onValueChange = {},
                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_77)) },
                modifier = Modifier.fillMaxWidth(),
                readOnly = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }
}

@Composable
fun UsbComDiagnosticBlock() {
    val logReady = androidx.compose.ui.res.stringResource(com.example.R.string.str_76)
    val logSearch = androidx.compose.ui.res.stringResource(com.example.R.string.str_75)
    val logTest = androidx.compose.ui.res.stringResource(com.example.R.string.str_73)
    val logAuto = androidx.compose.ui.res.stringResource(com.example.R.string.str_71)
    
    var logs by remember { mutableStateOf(listOf(logReady)) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    logs = listOf(logSearch) + logs
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_74), fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    logs = listOf(logTest) + logs
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_72), fontWeight = FontWeight.Bold)
            }
        }
        
        Button(
            onClick = {
                logs = listOf(logAuto) + logs
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
        ) {
            Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_70), fontWeight = FontWeight.Bold)
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f), shape = MaterialTheme.shapes.small)
                .padding(8.dp)
        ) {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                logs.forEach { log ->
                    Text(
                        text = log,
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    description: String? = null,
    icon: ImageVector,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    Card(
        modifier = if (onClick != null) Modifier.clickable { onClick() } else Modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(MaterialTheme.colorScheme.secondary, shape = MaterialTheme.shapes.small),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSecondary,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (description != null) {
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            content()
        }
    }
}

