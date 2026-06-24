package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.text.KeyboardOptions
import kotlin.random.Random

@Composable
fun CancelAccountingDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    var num1 by remember { mutableIntStateOf(Random.nextInt(1, 20)) }
    var num2 by remember { mutableIntStateOf(Random.nextInt(1, 20)) }
    var answer by remember { mutableStateOf("") }
    var hasError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Отмена учета") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Вы уверены, что хотите отменить принятие сессии к учету? Для подтверждения решите пример:")
                Text("$num1 + $num2 = ?", style = MaterialTheme.typography.titleMedium)
                OutlinedTextField(
                    value = answer,
                    onValueChange = { 
                        answer = it
                        hasError = false
                    },
                    label = { Text("Ответ") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = hasError,
                    singleLine = true
                )
                if (hasError) {
                    Text("Неверный ответ", color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val intAnswer = answer.toIntOrNull()
                    if (intAnswer == num1 + num2) {
                        onConfirm()
                    } else {
                        hasError = true
                    }
                }
            ) {
                Text("Подтвердить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )
}
