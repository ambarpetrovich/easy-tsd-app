package com.example

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportRecognitionScreen(
    onBack: () -> Unit,
    onCreateSession: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: ImportSessionViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_38), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_68))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = onCreateSession,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_157))
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (sessions.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    EmptyState(title = androidx.compose.ui.res.stringResource(com.example.R.string.str_42), subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.str_193))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        ImportSessionCard(session = session, onClick = { onSessionClick(session.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportSessionCard(session: ImportSession, onClick: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Text(
                    text = session.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                SuggestionChip(
                    onClick = { },
                    label = { Text(androidx.compose.ui.res.stringResource(session.status.displayNameRes)) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = when (session.status) {
                            ImportSessionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            ImportSessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                            ImportSessionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            ImportSessionStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
                        }
                    ),
                    border = null
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = session.date,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                if (session.status == ImportSessionStatus.COMPLETED) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.str_159, session.totalCodes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (session.status == ImportSessionStatus.IN_PROGRESS || session.status == ImportSessionStatus.PENDING) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { session.progress },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.primaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${(session.progress * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateImportSessionScreen(
    onBack: () -> Unit,
    viewModel: ImportSessionViewModel = viewModel()
) {
    val selectedFiles = remember { mutableStateListOf<String>() }
    val context = androidx.compose.ui.platform.LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_192), fontWeight = FontWeight.Black) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_68))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.secondary,
                    titleContentColor = MaterialTheme.colorScheme.onSecondary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSecondary
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val fakeFileNameFormat = androidx.compose.ui.res.stringResource(com.example.R.string.str_191, selectedFiles.size + 1, listOf("csv", "xlsx", "xml", "txt", "json").random())
            Button(
                onClick = {
                    // Actually the str_191 is "Данные_${selectedFiles.size + 1}.${exts.random()}"
                    // Oh! This string had string interpolation inside the string literal!
                    // My naive script broke it.
                    selectedFiles.add(fakeFileNameFormat)
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer
                )
            ) {
                Icon(Icons.Default.AttachFile, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_156))
            }

            if (selectedFiles.isEmpty()) {
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.str_190),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            } else {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_154), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(selectedFiles.size, key = { it }) { index ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(selectedFiles[index], modifier = Modifier.weight(1f))
                                IconButton(onClick = { selectedFiles.removeAt(index) }) {
                                    Icon(Icons.Default.Delete, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_130), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                    
                    item {
                        if (selectedFiles.any { it.endsWith(".xml", ignoreCase = true) }) {
                            var createNew by remember { mutableStateOf(false) }
                            var updateExisting by remember { mutableStateOf(false) }
                            var barcodeType by remember { mutableStateOf("") }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_189), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = createNew, onCheckedChange = { createNew = it })
                                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_188))
                            }
                            
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Checkbox(checked = updateExisting, onCheckedChange = { updateExisting = it })
                                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_187))
                            }
                            
                            OutlinedTextField(
                                value = barcodeType,
                                onValueChange = { barcodeType = it },
                                label = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_186)) },
                                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                            )
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedFiles.isNotEmpty()) {
                        viewModel.createSession(selectedFiles.toList(), context)
                        onBack()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                enabled = selectedFiles.isNotEmpty(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_153), fontWeight = FontWeight.Black)
            }
        }
    }
}
