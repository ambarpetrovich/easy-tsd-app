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
        title = { Text("Принять к учету") },
        text = {
            if (!showWarnings) {
                Column {
                    Text("Выберите тип операции учета:")
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                        Button(onClick = { selectedType = AccountingType.RECEIPT }) {
                            Text("Приёмка")
                        }
                        Button(onClick = { selectedType = AccountingType.SHIPMENT }) {
                            Text("Отгрузка")
                        }
                    }
                    if (selectedType != null) {
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Выбрано: ${selectedType!!.displayName}", fontWeight = FontWeight.Bold)
                    }
                }
            } else {
                Column(modifier = Modifier.fillMaxHeight(0.8f)) {
                    Text("Предупреждение!", color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("У ${warnings.size} кодов тип последней операции совпадает с текущим (${selectedType!!.displayName}).")
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn(modifier = Modifier.weight(1f)) {
                        items(warnings) { (code, ops) ->
                            Card(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text("Код: $code", fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text("История операций:", style = MaterialTheme.typography.labelSmall)
                                    ops.forEach { op ->
                                        val dateStr = SimpleDateFormat("dd.MM.yyyy HH:mm:ss", Locale.getDefault()).format(Date(op.timestamp))
                                        Text("- ${op.type.displayName} ($dateStr) в [${op.sessionName}]", style = MaterialTheme.typography.labelSmall)
                                    }
                                }
                            }
                        }
                    }
                    
                    Text("Все равно принять к учету?", fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 8.dp))
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
                    Text(if (showWarnings) "Подтвердить" else "Проверить и Принять")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
