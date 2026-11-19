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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
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
import androidx.compose.material3.Switch
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
import com.detflix.admin.data.AdminContentEntity
import com.detflix.admin.data.AdminSettings
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
fun CatalogAndSettingsScreen(
    state: AdminUiState,
    onSaveContent: (
        id: String?,
        title: String,
        type: String,
        categories: String,
        year: Int,
        posterUrl: String,
        streamUrl: String,
        description: String,
        featured: Boolean
    ) -> Unit,
    onToggleContentAvailable: (AdminContentEntity) -> Unit,
    onDeleteContent: (AdminContentEntity) -> Unit,
    onExportCatalog: () -> Unit,
    onSaveSettings: (AdminSettings) -> Unit
) {
    var subTab by remember { mutableStateOf("CATALOG") }
    var showContentDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                FilterChip(
                    selected = subTab == "CATALOG",
                    onClick = { subTab = "CATALOG" },
                    label = { Text("Catálogo (${state.contents.size})") }
                )
                FilterChip(
                    selected = subTab == "SETTINGS",
                    onClick = { subTab = "SETTINGS" },
                    label = { Text("Configuración y PIN") }
                )
            }
        }

        if (subTab == "CATALOG") {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = { showContentDialog = true },
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        modifier = Modifier.weight(1f).height(48.dp).testTag("catalog_add_btn")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Agregar Título", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = onExportCatalog,
                        modifier = Modifier.weight(1f).height(48.dp).testTag("catalog_export_btn")
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Exportar JSON")
                    }
                }
            }

            items(state.contents, key = { it.id }) { item ->
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
                        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(item.title, style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
                                StatusPill(item.type, DetflixGold)
                            }
                            Text(
                                text = "${item.categories} • ${item.year}",
                                style = MaterialTheme.typography.bodyMedium,
                                color = DetflixTextSecondary
                            )
                            Text(
                                text = if (item.available) "Visible en catálogo" else "Oculto",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (item.available) DetflixSuccess else DetflixTextMuted
                            )
                        }
                        IconButton(onClick = { onToggleContentAvailable(item) }) {
                            Icon(
                                imageVector = if (item.available) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                contentDescription = "Visibilidad",
                                tint = DetflixGold
                            )
                        }
                        IconButton(onClick = { onDeleteContent(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar", tint = DetflixRed)
                        }
                    }
                }
            }
        } else {
            item {
                SettingsFormCard(settings = state.settings, onSave = onSaveSettings)
            }
        }
    }

    if (showContentDialog) {
        var title by remember { mutableStateOf("") }
        var type by remember { mutableStateOf("MOVIE") }
        var categories by remember { mutableStateOf("Acción, Drama") }
        var year by remember { mutableStateOf("2025") }
        var posterUrl by remember { mutableStateOf("") }
        var streamUrl by remember { mutableStateOf("") }
        var description by remember { mutableStateOf("") }
        var featured by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showContentDialog = false },
            title = { Text("Agregar Película / Serie / Novela") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("MOVIE" to "Película", "SERIES" to "Serie", "NOVELA" to "Novela").forEach { (k, label) ->
                            FilterChip(selected = type == k, onClick = { type = k }, label = { Text(label) })
                        }
                    }
                    OutlinedTextField(
                        value = categories,
                        onValueChange = { categories = it },
                        label = { Text("Categorías") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = year,
                        onValueChange = { year = it.filter { c -> c.isDigit() } },
                        label = { Text("Año") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = posterUrl,
                        onValueChange = { posterUrl = it },
                        label = { Text("URL Póster (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = { streamUrl = it },
                        label = { Text("URL Video MP4 / HLS (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Sinopsis") },
                        maxLines = 3,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = featured, onCheckedChange = { featured = it })
                        Text("Destacar en portada")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        onSaveContent(
                            null,
                            title,
                            type,
                            categories,
                            year.toIntOrNull() ?: 2025,
                            posterUrl,
                            streamUrl,
                            description,
                            featured
                        )
                        showContentDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixRed)
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContentDialog = false }) { Text("Cancelar") }
            }
        )
    }
}

@Composable
private fun SettingsFormCard(
    settings: AdminSettings,
    onSave: (AdminSettings) -> Unit
) {
    var price by remember(settings.monthlyPriceUsd) { mutableStateOf(settings.monthlyPriceUsd.toString()) }
    var days by remember(settings.defaultDurationDays) { mutableStateOf(settings.defaultDurationDays.toString()) }
    var support by remember(settings.supportChannel) { mutableStateOf(settings.supportChannel) }
    var requirePin by remember(settings.requirePin) { mutableStateOf(settings.requirePin) }
    var pin by remember(settings.adminPin) { mutableStateOf(settings.adminPin) }

    Card(
        modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.large),
        colors = CardDefaults.cardColors(containerColor = DetflixSurface),
        shape = MaterialTheme.shapes.large
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ajustes de la Consola", style = MaterialTheme.typography.titleMedium, color = DetflixWhite)
            OutlinedTextField(
                value = price,
                onValueChange = { price = it },
                label = { Text("Precio mensual USD") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = days,
                onValueChange = { days = it.filter { c -> c.isDigit() } },
                label = { Text("Duración estándar del código (días)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = support,
                onValueChange = { support = it },
                label = { Text("Canal de soporte mostrado") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Pedir PIN al abrir DETFLIX Admin", color = DetflixWhite, style = MaterialTheme.typography.titleMedium)
                    Text("Opcional para proteger tu consola", color = DetflixTextMuted, style = MaterialTheme.typography.bodyMedium)
                }
                Switch(checked = requirePin, onCheckedChange = { requirePin = it })
            }
            if (requirePin) {
                OutlinedTextField(
                    value = pin,
                    onValueChange = { if (it.length <= 8) pin = it.filter { c -> c.isDigit() } },
                    label = { Text("PIN numérico") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            Button(
                onClick = {
                    onSave(
                        settings.copy(
                            monthlyPriceUsd = price.toDoubleOrNull() ?: settings.monthlyPriceUsd,
                            defaultDurationDays = days.toIntOrNull()?.coerceAtLeast(1) ?: 30,
                            supportChannel = support,
                            requirePin = requirePin,
                            adminPin = pin.ifBlank { "2013" }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                modifier = Modifier.fillMaxWidth().height(50.dp).testTag("settings_save_btn")
            ) {
                Text("Guardar Configuración", fontWeight = FontWeight.Bold)
            }
        }
    }
}
