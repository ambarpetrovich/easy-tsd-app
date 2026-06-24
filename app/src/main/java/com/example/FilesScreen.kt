package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.ui.components.ActionCard
import com.example.ui.components.EmptyState
import com.example.ui.components.SectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    onNavigateToPdfSessions: () -> Unit = {},
    onNavigateToImportSessions: () -> Unit = {}
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text("Файлы", fontWeight = FontWeight.Black) 
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
            ActionCard(
                title = "Распознавание DataMatrix в PDF",
                description = "Извлечение полных кодов маркировки и GTIN из PDF-файлов",
                icon = Icons.Default.PictureAsPdf,
                onClick = onNavigateToPdfSessions
            )
            
            ActionCard(
                title = "Импорт XLSX, XML, CSV, JSON, TXT",
                description = "Извлечение кодов, GTIN и наименований товаров",
                icon = Icons.Default.Description,
                onClick = onNavigateToImportSessions
            )

            SectionHeader(title = "Загруженные файлы")

            EmptyState(
                title = "Нет файлов",
                subtitle = "Загрузите файл для обработки"
            )
        }
    }
}

