package com.detflix.admin.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.detflix.admin.data.AdminCodeEntity
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

@Composable
fun CodesScreen(
    state: AdminUiState,
    onGenerateCode: (clientName: String?, recordPayment: Boolean) -> Unit,
    onAssignCode: (AdminCodeEntity, String) -> Unit,
    onRevokeCode: (AdminCodeEntity) -> Unit,
    onCopyCode: (String) -> Unit
) {
    val context = LocalContext.current
    var filter by remember { mutableStateOf("ALL") }
    var showCreateDialog by remember { mutableStateOf(false) }
    var assignTarget by remember { mutableStateOf<AdminCodeEntity?>(null) }

    val filtered = remember(state.codes, filter) {
        when (filter) {
            "AVAILABLE" -> state.codes.filter { it.status == "AVAILABLE" }
            "ASSIGNED" -> state.codes.filter { it.status == "ASSIGNED" }
            "REVOKED" -> state.codes.filter { it.status == "REVOKED" }
            else -> state.codes
        }
    }

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
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Generador de Códigos (${state.settings.defaultDurationDays} días)",
                        style = MaterialTheme.typography.titleMedium,
                        color = DetflixWhite
                    )
                    Text(
                        text = "Cada código está firmado criptográficamente y funciona directo en DETFLIX APP.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextSecondary
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = { onGenerateCode(null, false) },
                            colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                            modifier = Modifier.weight(1f).height(50.dp).testTag("codes_generate_btn")
                        ) {
                            Icon(Icons.Default.Key, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Código Rápido", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = { showCreateDialog = true },
                            modifier = Modifier.weight(1f).height(50.dp).testTag("codes_assign_new_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Con Cliente")
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todos (${state.codes.size})",
                    "AVAILABLE" to "Libres (${state.codes.count { it.status == "AVAILABLE" }})",
                    "ASSIGNED" to "Asignados (${state.codes.count { it.status == "ASSIGNED" }})",
                    "REVOKED" to "Revocados (${state.codes.count { it.status == "REVOKED" }})"
                ).forEach { (key, title) ->
                    FilterChip(
                        selected = filter == key,
                        onClick = { filter = key },
                        label = { Text(title) }
                    )
                }
            }
        }

        if (filtered.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurface)
                ) {
                    Text(
                        text = "No hay códigos en esta categoría. Toca \"Código Rápido\" arriba para crear uno.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.code }) { item ->
                val pillColor = when (item.status) {
                    "AVAILABLE" -> DetflixSuccess
                    "ASSIGNED" -> DetflixGold
                    else -> DetflixRed
                }
                val pillLabel = when (item.status) {
                    "AVAILABLE" -> "LIBRE"
                    "ASSIGNED" -> "ASIGNADO"
                    else -> "REVOCADO"
                }
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.code,
                                style = MaterialTheme.typography.titleLarge,
                                color = DetflixWhite
                            )
                            StatusPill(pillLabel, pillColor)
                        }
                        Text(
                            text = buildString {
                                append("Creado: ${formatAdminDate(item.createdAtMillis)}")
                                if (!item.assignedClientName.isNullOrBlank()) {
                                    append(" • Cliente: ${item.assignedClientName}")
                                }
                                if (item.expiresAtMillis != null) {
                                    append(" • Vence: ${formatAdminDate(item.expiresAtMillis)}")
                                }
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = DetflixTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedButton(
                                onClick = { onCopyCode(item.code) },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copiar")
                                Spacer(Modifier.width(4.dp))
                                Text("Copiar")
                            }
                            OutlinedButton(
                                onClick = {
                                    shareAccessCode(context, item.assignedClientName, item.code, item.durationDays)
                                },
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Compartir")
                                Spacer(Modifier.width(4.dp))
                                Text("Enviar")
                            }
                            Spacer(Modifier.weight(1f))
                            if (item.status == "AVAILABLE") {
                                IconButton(onClick = { assignTarget = item }) {
                                    Icon(Icons.Default.PersonAdd, contentDescription = "Asignar", tint = DetflixGold)
                                }
                            }
                            if (item.status != "REVOKED") {
                                IconButton(onClick = { onRevokeCode(item) }) {
                                    Icon(Icons.Default.Block, contentDescription = "Revocar", tint = DetflixRed)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var clientName by remember { mutableStateOf("") }
        var recordPay by remember { mutableStateOf(true) }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Generar y Asignar Código") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = clientName,
                        onValueChange = { clientName = it },
                        label = { Text("Nombre del cliente") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = recordPay, onCheckedChange = { recordPay = it })
                        Text("Registrar pago ($${state.settings.monthlyPriceUsd})")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onGenerateCode(clientName, recordPay)
                        showCreateDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                ) {
                    Text("Generar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancelar") }
            }
        )
    }

    assignTarget?.let { target ->
        var clientName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { assignTarget = null },
            title = { Text("Asignar ${target.code}") },
            text = {
                OutlinedTextField(
                    value = clientName,
                    onValueChange = { clientName = it },
                    label = { Text("Nombre del cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (clientName.isNotBlank()) {
                            onAssignCode(target, clientName)
                            assignTarget = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                ) {
                    Text("Asignar")
                }
            },
            dismissButton = {
                TextButton(onClick = { assignTarget = null }) { Text("Cancelar") }
            }
        )
    }
}
