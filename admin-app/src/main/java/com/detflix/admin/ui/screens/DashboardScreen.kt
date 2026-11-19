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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
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
import com.detflix.admin.ui.theme.DetflixWarning
import com.detflix.admin.ui.theme.DetflixWhite
import java.util.Locale

@Composable
fun DashboardScreen(
    state: AdminUiState,
    onGenerateQuickCode: () -> Unit,
    onOpenNewClientDialog: () -> Unit,
    onExportCatalog: () -> Unit,
    onCopyCode: (String) -> Unit
) {
    val context = LocalContext.current
    val m = state.metrics

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "INGRESOS TOTALES",
                    value = "$${String.format(Locale.US, "%.2f", m.totalRevenueUsd)}",
                    subtitle = "${state.payments.size} pagos",
                    icon = Icons.Default.AttachMoney,
                    accent = DetflixSuccess,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "CLIENTES ACTIVOS",
                    value = "${m.activeClients}",
                    subtitle = if (m.expiringSoonClients > 0) "${m.expiringSoonClients} por vencer" else "Al día",
                    icon = Icons.Default.People,
                    accent = if (m.expiringSoonClients > 0) DetflixWarning else DetflixGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                KpiCard(
                    title = "CÓDIGOS LIBRES",
                    value = "${m.availableCodes}",
                    subtitle = "${m.assignedCodes} asignados",
                    icon = Icons.Default.Key,
                    accent = DetflixRed,
                    modifier = Modifier.weight(1f)
                )
                KpiCard(
                    title = "CATÁLOGO ACTIVO",
                    value = "${m.publishedContents}",
                    subtitle = "Películas/Series/Novelas",
                    icon = Icons.Default.Movie,
                    accent = DetflixGold,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.large),
                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                shape = MaterialTheme.shapes.large
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Acciones Rápidas", style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                    Button(
                        onClick = onGenerateQuickCode,
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        modifier = Modifier.fillMaxWidth().height(50.dp).testTag("dash_generate_code_btn")
                    ) {
                        Icon(Icons.Default.Key, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("GENERAR CÓDIGO (${state.settings.defaultDurationDays} DÍAS)", fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onOpenNewClientDialog,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_new_client_btn")
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Cliente + Pago")
                        }
                        OutlinedButton(
                            onClick = onExportCatalog,
                            modifier = Modifier.weight(1f).height(48.dp).testTag("dash_export_catalog_btn")
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("catalog.json")
                        }
                    }
                }
            }
        }

        state.latestGeneratedCode?.let { latest ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, DetflixGold.copy(alpha = 0.6f), MaterialTheme.shapes.large),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
                    shape = MaterialTheme.shapes.large
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Último código generado",
                            style = MaterialTheme.typography.labelSmall,
                            color = DetflixGold
                        )
                        Text(
                            text = latest.code,
                            style = MaterialTheme.typography.headlineLarge,
                            color = DetflixWhite
                        )
                        if (!latest.assignedClientName.isNullOrBlank()) {
                            Text(
                                text = "Cliente: ${latest.assignedClientName} • ${latest.durationDays} días",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DetflixTextSecondary
                            )
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Button(
                                onClick = { onCopyCode(latest.code) },
                                colors = ButtonDefaults.buttonColors(containerColor = DetflixGold, contentColor = DetflixSurface),
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Copiar", fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = {
                                    shareAccessCode(context, latest.assignedClientName, latest.code, latest.durationDays)
                                },
                                modifier = Modifier.weight(1f).height(48.dp)
                            ) {
                                Text("Enviar WhatsApp")
                            }
                        }
                    }
                }
            }
        }

        item {
            Text(
                text = "Actividad Reciente",
                style = MaterialTheme.typography.titleMedium,
                color = DetflixWhite
            )
        }

        if (state.logs.isEmpty()) {
            item {
                Text(
                    text = "Sin actividad registrada todavía.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetflixTextMuted
                )
            }
        } else {
            items(state.logs.take(10), key = { it.id }) { log ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = log.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = DetflixTextSecondary,
                            modifier = Modifier.weight(1f).padding(end = 8.dp)
                        )
                        Text(
                            text = formatAdminDateTime(log.createdAtMillis),
                            style = MaterialTheme.typography.labelSmall,
                            color = DetflixTextMuted
                        )
                    }
                }
            }
        }
    }
}
