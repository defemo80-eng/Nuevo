package com.detflix.admin.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.detflix.admin.ui.AdminUiState
import com.detflix.admin.ui.theme.DetflixBorder
import com.detflix.admin.ui.theme.DetflixGold
import com.detflix.admin.ui.theme.DetflixRed
import com.detflix.admin.ui.theme.DetflixSuccess
import com.detflix.admin.ui.theme.DetflixSurface
import com.detflix.admin.ui.theme.DetflixSurfaceElevated
import com.detflix.admin.ui.theme.DetflixTextMuted
import com.detflix.admin.ui.theme.DetflixTextSecondary
import com.detflix.admin.ui.theme.DetflixWhite
import java.util.Locale

@Composable
fun PaymentsScreen(
    state: AdminUiState,
    onRecordPayment: (clientName: String, amountUsd: Double, method: String, note: String) -> Unit
) {
    var showAddPayment by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.large),
                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                shape = MaterialTheme.shapes.large
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("INGRESOS ACUMULADOS", style = MaterialTheme.typography.labelSmall, color = DetflixTextSecondary)
                        Text(
                            text = "$${String.format(Locale.US, "%.2f", state.metrics.totalRevenueUsd)} USD",
                            style = MaterialTheme.typography.displayMedium,
                            color = DetflixSuccess
                        )
                        Text("${state.payments.size} movimientos", style = MaterialTheme.typography.bodyMedium, color = DetflixTextMuted)
                    }
                    Button(
                        onClick = { showAddPayment = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        modifier = Modifier.height(48.dp).testTag("payments_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Pago", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        if (state.payments.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurface)
                ) {
                    Text(
                        text = "No hay pagos registrados todavía.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(state.payments, key = { it.id }) { payment ->
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(payment.clientName, style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                            Text(
                                text = "${payment.method} • ${formatAdminDateTime(payment.paidAtMillis)}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DetflixTextSecondary
                            )
                            if (payment.referenceCode.isNotBlank() || payment.note.isNotBlank()) {
                                Text(
                                    text = listOf(payment.referenceCode, payment.note).filter { it.isNotBlank() }.joinToString(" • "),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = DetflixGold
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.AttachMoney, contentDescription = null, tint = DetflixSuccess)
                            Text(
                                text = String.format(Locale.US, "%.2f", payment.amountUsd),
                                style = MaterialTheme.typography.titleLarge,
                                color = DetflixSuccess
                            )
                        }
                    }
                }
            }
        }
    }

    if (showAddPayment) {
        var clientName by remember { mutableStateOf("") }
        var amount by remember { mutableStateOf(state.settings.monthlyPriceUsd.toString()) }
        var method by remember { mutableStateOf("Transferencia") }
        var note by remember { mutableStateOf("Mensualidad DETFLIX") }

        AlertDialog(
            onDismissRequest = { showAddPayment = false },
            title = { Text("Registrar Pago") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto USD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = method,
                        onValueChange = { method = it },
                        label = { Text("Método (Nequi, Efectivo, Zelle...)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = note,
                        onValueChange = { note = it },
                        label = { Text("Nota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onRecordPayment(clientName, amount.toDoubleOrNull() ?: 0.0, method, note)
                        showAddPayment = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddPayment = false }) { Text("Cancelar") }
            }
        )
    }
}
