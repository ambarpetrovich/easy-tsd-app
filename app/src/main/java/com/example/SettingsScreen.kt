package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
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
    val tabs = listOf("Системные", "Прикладные")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Настройки", fontWeight = FontWeight.Black) 
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
        title = "О приложении",
        icon = Icons.Default.History,
        onClick = onNavigateToHistory
    ) {
        Text(
            text = "История версий",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }

    SettingsCard(
        title = "Язык",
        icon = Icons.Default.Language
    ) {
        OutlinedTextField(
            value = "Русский",
            onValueChange = {},
            modifier = Modifier.fillMaxWidth(),
            readOnly = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.background,
                unfocusedContainerColor = MaterialTheme.colorScheme.background
            )
        )
    }

    SettingsCard(
        title = "Режим сканирования по умолчанию",
        icon = Icons.Default.QrCodeScanner
    ) {
        Column {
            listOf("Камера", "HID-сканер", "USB-COM").forEach { mode ->
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
        title = "Общие",
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
                Text("Звук при сканировании", fontWeight = FontWeight.Bold)
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
                Text("Вибрация при сканировании", fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }

    SettingsCard(
        title = "USB-COM сканер",
        description = "Настройки последовательного порта для USB-сканеров. Большинство использует 9600 8N1.",
        icon = Icons.Default.Usb
    ) {
        UsbComDiagnosticBlock()
    }
}

@Composable
fun BusinessSettingsSection(onNavigateToProducts: () -> Unit) {
    SettingsCard(
        title = "Интеграция",
        description = "Настройте интеграцию с внешним сервисом для экспорта данных.",
        icon = Icons.Default.Link
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("URL внешнего сервиса") },
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
                label = { Text("Bearer токен") },
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
        title = "Реквизиты",
        description = "Настройка реквизитов организации и пользователя.",
        icon = Icons.Default.Business
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("ИНН Организации") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
            OutlinedTextField(
                value = "",
                onValueChange = {},
                label = { Text("ФИО сотрудника") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = MaterialTheme.colorScheme.background,
                    unfocusedContainerColor = MaterialTheme.colorScheme.background
                )
            )
        }
    }

    SettingsCard(
        title = "Товары",
        description = "Правила обработки номенклатуры и штрих-кодов.",
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
                Text("Разрешить неизвестные штрих-коды", fontWeight = FontWeight.Bold)
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
                Text("Автоматически загружать справочник", fontWeight = FontWeight.Bold)
                Switch(checked = true, onCheckedChange = {})
            }
        }
    }

    SettingsCard(
        title = "Импорт / Экспорт",
        description = "Форматы и правила обмена файлами.",
        icon = Icons.Default.ImportExport
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = "Excel (.xlsx)",
                onValueChange = {},
                label = { Text("Формат экспорта по умолчанию") },
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
    var logs by remember { mutableStateOf(listOf("[ОЖИДАНИЕ] Готов к сканированию USB устройств...")) }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { 
                    logs = listOf("[ПОИСК] Сканирование устройств...") + logs
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Обновить", fontWeight = FontWeight.Bold)
            }
            Button(
                onClick = {
                    logs = listOf("[ТЕСТ] Проверка подключения...") + logs
                },
                modifier = Modifier.weight(1f),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
            ) {
                Text("Проверить", fontWeight = FontWeight.Bold)
            }
        }
        
        Button(
            onClick = {
                logs = listOf("[АВТО] Запуск автоопределения...") + logs
            },
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary, contentColor = MaterialTheme.colorScheme.onSecondary)
        ) {
            Text("Автоопределение настроек", fontWeight = FontWeight.Bold)
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

