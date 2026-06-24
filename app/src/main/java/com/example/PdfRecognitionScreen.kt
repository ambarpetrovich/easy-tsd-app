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
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import android.content.Context
import android.provider.OpenableColumns
import android.net.Uri

fun getFileName(context: Context, uri: Uri): String? {
    var result: String? = null
    if (uri.scheme == "content") {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index != -1) {
                    result = cursor.getString(index)
                }
            }
        }
    }
    if (result == null) {
        result = uri.path
        val cut = result?.lastIndexOf('/') ?: -1
        if (cut != -1) {
            result = result?.substring(cut + 1)
        }
    }
    return result
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfRecognitionScreen(
    onBack: () -> Unit,
    onCreateSession: () -> Unit,
    onSessionClick: (String) -> Unit,
    viewModel: PdfSessionViewModel = viewModel()
) {
    val sessions by viewModel.sessions.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_40), fontWeight = FontWeight.Black) },
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
                    EmptyState(title = androidx.compose.ui.res.stringResource(com.example.R.string.str_42), subtitle = androidx.compose.ui.res.stringResource(com.example.R.string.str_160))
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(sessions, key = { it.id }) { session ->
                        PdfSessionCard(session = session, onClick = { onSessionClick(session.id) })
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfSessionCard(session: PdfSession, onClick: () -> Unit) {
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
                            PdfSessionStatus.COMPLETED -> MaterialTheme.colorScheme.primaryContainer
                            PdfSessionStatus.IN_PROGRESS -> MaterialTheme.colorScheme.secondaryContainer
                            PdfSessionStatus.FAILED -> MaterialTheme.colorScheme.errorContainer
                            PdfSessionStatus.PENDING -> MaterialTheme.colorScheme.surfaceVariant
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
                if (session.status == PdfSessionStatus.COMPLETED) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.str_159, session.totalCodes),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (session.status == PdfSessionStatus.IN_PROGRESS || session.status == PdfSessionStatus.PENDING) {
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
fun CreatePdfSessionScreen(
    onBack: () -> Unit,
    viewModel: PdfSessionViewModel = viewModel()
) {
    val context = LocalContext.current
    val selectedUris = remember { mutableStateListOf<android.net.Uri>() }
    val selectedFiles = remember { mutableStateListOf<String>() }

    val fallbackName = androidx.compose.ui.res.stringResource(com.example.R.string.str_158)
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetMultipleContents(),
        onResult = { uris ->
            uris.forEach { uri ->
                selectedUris.add(uri)
                val fileName = getFileName(context, uri) ?: fallbackName
                selectedFiles.add(fileName)
            }
        }
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_157), fontWeight = FontWeight.Black) },
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
            Button(
                onClick = { 
                    launcher.launch("application/pdf")
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
                        text = androidx.compose.ui.res.stringResource(com.example.R.string.str_155),
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
                                IconButton(onClick = { 
                                    selectedFiles.removeAt(index)
                                    selectedUris.removeAt(index)
                                }) {
                                    Icon(Icons.Default.Delete, contentDescription = androidx.compose.ui.res.stringResource(com.example.R.string.str_130), tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = {
                    if (selectedFiles.isNotEmpty()) {
                        viewModel.createSession(selectedUris.toList(), selectedFiles.toList(), context)
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
