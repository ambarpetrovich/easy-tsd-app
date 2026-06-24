package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.AccountingType
import com.example.AccountingViewModel
import com.example.AccountingOperation
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountingVerificationDialog(
    codes: List<String>,
    sessionId: String,
    sessionName: String,
    accountingViewModel: AccountingViewModel,
    onDismiss: () -> Unit
) {
    var selectedType by remember { mutableStateOf<AccountingType?>(null) }
    var warnings by remember { mutableStateOf<List<Pair<String, List<AccountingOperation>>>>(emptyList()) }
    var showWarnings by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_26)) },
        text = {
            if (!showWarnings) {
                Column {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_25))
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { selectedType = AccountingType.RECEIPT }) {
                            Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_24))
                        }
                        Button(onClick = { selectedType = AccountingType.SHIPMENT }) {
                            Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_23))
                        }
                    }
                    if (selectedType != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_22, androidx.compose.ui.res.stringResource(selectedType!!.displayNameRes)), fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_21), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_20, warnings.size, androidx.compose.ui.res.stringResource(selectedType!!.displayNameRes)))
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(warnings) { (code, ops) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_19, code), fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_18), style = MaterialTheme.typography.labelSmall)
                                    ops.forEach { op ->
                                        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(op.timestamp))
                                        Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_17, androidx.compose.ui.res.stringResource(op.type.displayNameRes), dateStr, op.sessionName), style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    
                    Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_16), fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
                }
            }
        },
        confirmButton = {
            if (selectedType != null) {
                Button(onClick = {
                    if (!showWarnings) {
                        val w = accountingViewModel.verifyOperations(codes, selectedType!!)
                        if (w.isNotEmpty()) {
                            warnings = w
                            showWarnings = true
                        } else {
                            accountingViewModel.applyAccounting(codes, selectedType!!, sessionId, sessionName)
                            onDismiss()
                        }
                    } else {
                        accountingViewModel.applyAccounting(codes, selectedType!!, sessionId, sessionName)
                        onDismiss()
                    }
                }) {
                    Text(if (showWarnings) androidx.compose.ui.res.stringResource(com.example.R.string.str_15) else androidx.compose.ui.res.stringResource(com.example.R.string.str_14))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(androidx.compose.ui.res.stringResource(com.example.R.string.str_13))
            }
        }
    )
}
