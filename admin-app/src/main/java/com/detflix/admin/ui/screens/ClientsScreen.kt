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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.detflix.admin.data.AdminClientEntity
import com.detflix.admin.ui.AdminUiState
import com.detflix.admin.ui.theme.DetflixBorder
import com.detflix.admin.ui.theme.DetflixGold
import com.detflix.admin.ui.theme.DetflixRed
import com.detflix.admin.ui.theme.DetflixSuccess
import com.detflix.admin.ui.theme.DetflixSurface
import com.detflix.admin.ui.theme.DetflixSurfaceElevated
import com.detflix.admin.ui.theme.DetflixTextMuted
import com.detflix.admin.ui.theme.DetflixTextSecondary
import com.detflix.admin.ui.theme.DetflixWarning
import com.detflix.admin.ui.theme.DetflixWhite

@Composable
fun ClientsScreen(
    state: AdminUiState,
    showAddDialogInitial: Boolean = false,
    onDismissInitialAdd: () -> Unit = {},
    onRegisterOrRenew: (name: String, contact: String, days: Int, amountUsd: Double, method: String) -> Unit,
    onToggleStatus: (AdminClientEntity) -> Unit,
    onDeleteClient: (AdminClientEntity) -> Unit
) {
    val context = LocalContext.current
    var search by remember { mutableStateOf("") }
    var filter by remember { mutableStateOf("ALL") }
    var showAddDialog by remember(showAddDialogInitial) { mutableStateOf(showAddDialogInitial) }
    var renewTarget by remember { mutableStateOf<AdminClientEntity?>(null) }

    val now = System.currentTimeMillis()
    val filtered = remember(state.clients, search, filter) {
        state.clients.filter { client ->
            val matchesQuery = search.isBlank() ||
                client.name.contains(search, ignoreCase = true) ||
                client.contact.contains(search, ignoreCase = true)
            val daysLeft = daysRemaining(client.expiresAtMillis)
            val matchesFilter = when (filter) {
                "ACTIVE" -> client.status == "ACTIVE" && client.expiresAtMillis > now
                "EXPIRING" -> client.status == "ACTIVE" && daysLeft in 1..5
                "SUSPENDED" -> client.status == "SUSPENDED" || client.expiresAtMillis <= now
                else -> true
            }
            matchesQuery && matchesFilter
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Buscar cliente...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
                    singleLine = true,
                    modifier = Modifier.weight(1f).testTag("clients_search_input")
                )
                Button(
                    onClick = { showAddDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                    modifier = Modifier.height(52.dp).testTag("clients_add_btn")
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Nuevo cliente")
                    Spacer(Modifier.width(4.dp))
                    Text("Nuevo", fontWeight = FontWeight.Bold)
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "ALL" to "Todos",
                    "ACTIVE" to "Activos",
                    "EXPIRING" to "Por Vencer",
                    "SUSPENDED" to "Suspendidos/Vencidos"
                ).forEach { (key, title) ->
                    FilterChip(selected = filter == key, onClick = { filter = key }, label = { Text(title) })
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
                        text = "No hay clientes aún. Toca \"Nuevo\" para activar a tu primer cliente con su código mensual.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextMuted,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            items(filtered, key = { it.id }) { client ->
                val remaining = daysRemaining(client.expiresAtMillis)
                val statusColor = when {
                    client.status == "SUSPENDED" || remaining == 0L -> DetflixRed
                    remaining <= 5L -> DetflixWarning
                    else -> DetflixSuccess
                }
                val statusLabel = when {
                    client.status == "SUSPENDED" -> "SUSPENDIDO"
                    remaining == 0L -> "VENCIDO"
                    remaining <= 5L -> "VENCE EN ${remaining}D"
                    else -> "ACTIVO • ${remaining}D"
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
                            Column(modifier = Modifier.weight(1f)) {
                                Text(client.name, style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                                if (client.contact.isNotBlank()) {
                                    Text(client.contact, style = MaterialTheme.typography.bodyMedium, color = DetflixTextMuted)
                                }
                            }
                            StatusPill(statusLabel, statusColor)
                        }
                        Text(
                            text = "Último código: ${client.lastCode.ifBlank { "—" }} • Vence: ${formatAdminDate(client.expiresAtMillis)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DetflixTextSecondary
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Button(
                                onClick = { renewTarget = client },
                                colors = ButtonDefaults.buttonColors(containerColor = DetflixGold, contentColor = DetflixSurface),
                                modifier = Modifier.height(48.dp)
                            ) {
                                Icon(Icons.Default.Autorenew, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text("Renovar", fontWeight = FontWeight.Bold)
                            }
                            if (client.lastCode.isNotBlank()) {
                                OutlinedButton(
                                    onClick = {
                                        shareAccessCode(context, client.name, client.lastCode, client.planDays)
                                    },
                                    modifier = Modifier.height(48.dp)
                                ) {
                                    Icon(Icons.Default.Share, contentDescription = "Compartir")
                                    Spacer(Modifier.width(4.dp))
                                    Text("Enviar")
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            IconButton(onClick = { onToggleStatus(client) }) {
                                Icon(
                                    imageVector = if (client.status == "ACTIVE") Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                                    contentDescription = "Cambiar estado",
                                    tint = if (client.status == "ACTIVE") DetflixWarning else DetflixSuccess
                                )
                            }
                            IconButton(onClick = { onDeleteClient(client) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DetflixRed)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        ClientFormDialog(
            title = "Nuevo Cliente + Código",
            initialName = "",
            initialContact = "",
            defaultDays = state.settings.defaultDurationDays,
            defaultAmount = state.settings.monthlyPriceUsd,
            onDismiss = {
                showAddDialog = false
                onDismissInitialAdd()
            },
            onConfirm = { n, c, d, a, m ->
                onRegisterOrRenew(n, c, d, a, m)
                showAddDialog = false
                onDismissInitialAdd()
            }
        )
    }

    renewTarget?.let { target ->
        ClientFormDialog(
            title = "Renovar a ${target.name}",
            initialName = target.name,
            initialContact = target.contact,
            defaultDays = state.settings.defaultDurationDays,
            defaultAmount = state.settings.monthlyPriceUsd,
            onDismiss = { renewTarget = null },
            onConfirm = { n, c, d, a, m ->
                onRegisterOrRenew(n, c, d, a, m)
                renewTarget = null
            }
        )
    }
}

@Composable
private fun ClientFormDialog(
    title: String,
    initialName: String,
    initialContact: String,
    defaultDays: Int,
    defaultAmount: Double,
    onDismiss: () -> Unit,
    onConfirm: (name: String, contact: String, days: Int, amountUsd: Double, method: String) -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var contact by remember { mutableStateOf(initialContact) }
    var days by remember { mutableStateOf(defaultDays.toString()) }
    var amount by remember { mutableStateOf(defaultAmount.toString()) }
    var method by remember { mutableStateOf("Transferencia") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del cliente") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = contact,
                    onValueChange = { contact = it },
                    label = { Text("WhatsApp o nota (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = days,
                        onValueChange = { days = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Días") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                    OutlinedTextField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = { Text("Monto USD") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.weight(1f)
                    )
                }
                OutlinedTextField(
                    value = method,
                    onValueChange = { method = it },
                    label = { Text("Método de pago") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val d = days.toIntOrNull() ?: defaultDays
                    val a = amount.toDoubleOrNull() ?: defaultAmount
                    onConfirm(name, contact, d, a, method)
                },
                colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
            ) {
                Text("Activar y Generar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        }
    )
}
