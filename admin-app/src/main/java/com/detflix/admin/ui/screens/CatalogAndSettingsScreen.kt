package com.detflix.admin.ui.screens

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlaylistAdd
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
import androidx.compose.material3.HorizontalDivider
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
import com.detflix.admin.data.AdminEpisodeEntity
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

private val SUGGESTED_CATEGORIES = listOf(
    "Acción",
    "Drama",
    "Comedia",
    "Terror",
    "Romance",
    "Novelas",
    "Ciencia ficción",
    "Aventura",
    "Animación",
    "Infantil",
    "Documentales",
    "Deportes"
)

@OptIn(ExperimentalLayoutApi::class)
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
    onSaveEpisode: (
        seriesId: String,
        seasonNumber: Int,
        episodeNumber: Int,
        title: String,
        description: String,
        thumbnailUrl: String,
        streamUrl: String,
        durationMinutes: Int
    ) -> Unit,
    onDeleteEpisode: (AdminEpisodeEntity) -> Unit,
    onExportCatalog: () -> Unit,
    onSaveSettings: (AdminSettings) -> Unit
) {
    var subTab by remember { mutableStateOf("CATALOG") }
    var showContentDialog by remember { mutableStateOf(false) }
    var editingContent by remember { mutableStateOf<AdminContentEntity?>(null) }
    var episodeManagerSeries by remember { mutableStateOf<AdminContentEntity?>(null) }

    val episodesBySeries = remember(state.episodes) { state.episodes.groupBy { it.seriesId } }

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
                        onClick = {
                            editingContent = null
                            showContentDialog = true
                        },
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
                val seriesEpisodes = episodesBySeries[item.id].orEmpty()
                val supportsEpisodes = item.type == "SERIES" || item.type == "NOVELA"

                Card(
                    modifier = Modifier.fillMaxWidth().border(1.dp, DetflixBorder, MaterialTheme.shapes.medium),
                    colors = CardDefaults.cardColors(containerColor = DetflixSurfaceElevated),
                    shape = MaterialTheme.shapes.medium
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
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
                                    text = if (item.available) "Visible en catálogo • Sincroniza con DETFLIX APP" else "Oculto",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (item.available) DetflixSuccess else DetflixTextMuted
                                )
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(
                                    onClick = {
                                        editingContent = item
                                        showContentDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = "Editar", tint = DetflixWhite)
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

                        if (supportsEpisodes) {
                            OutlinedButton(
                                onClick = { episodeManagerSeries = item },
                                modifier = Modifier.fillMaxWidth().height(48.dp)
                            ) {
                                Icon(Icons.Default.PlaylistAdd, contentDescription = null, tint = DetflixGold)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Episodios / Capítulos (${seriesEpisodes.size})",
                                    color = DetflixWhite,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
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
        val current = editingContent
        var title by remember(current?.id) { mutableStateOf(current?.title ?: "") }
        var type by remember(current?.id) { mutableStateOf(current?.type ?: "MOVIE") }
        var categories by remember(current?.id) { mutableStateOf(current?.categories ?: "Acción, Drama") }
        var year by remember(current?.id) { mutableStateOf((current?.year ?: 2025).toString()) }
        var posterUrl by remember(current?.id) { mutableStateOf(current?.posterUrl ?: "") }
        var streamUrl by remember(current?.id) { mutableStateOf(current?.streamUrl ?: "") }
        var description by remember(current?.id) { mutableStateOf(current?.description ?: "") }
        var featured by remember(current?.id) { mutableStateOf(current?.featured ?: false) }

        val activeCatTokens = categories.split(",").map { it.trim() }.filter { it.isNotEmpty() }

        AlertDialog(
            onDismissRequest = { showContentDialog = false },
            title = {
                Text(
                    text = if (current == null) "Agregar Título Autorizado" else "Editar Título",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Título") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf(
                            "MOVIE" to "Película",
                            "SERIES" to "Serie",
                            "NOVELA" to "Novela",
                            "DOCUMENTARY" to "Documental"
                        ).forEach { (k, label) ->
                            FilterChip(selected = type == k, onClick = { type = k }, label = { Text(label) })
                        }
                    }
                    Text(
                        text = "Categorías rápidas (toca para agregar o quitar):",
                        style = MaterialTheme.typography.labelSmall,
                        color = DetflixTextSecondary
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        SUGGESTED_CATEGORIES.forEach { cat ->
                            val selected = activeCatTokens.any { it.equals(cat, ignoreCase = true) }
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val updated = if (selected) {
                                        activeCatTokens.filterNot { it.equals(cat, ignoreCase = true) }
                                    } else {
                                        activeCatTokens + cat
                                    }
                                    categories = updated.joinToString(", ")
                                },
                                label = { Text(cat) }
                            )
                        }
                    }
                    OutlinedTextField(
                        value = categories,
                        onValueChange = { categories = it },
                        label = { Text("Categorías (separadas por coma)") },
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
                        label = { Text("URL Póster (HTTPS opcional)") },
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
                            current?.id,
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
                    Text(if (current == null) "Guardar" else "Actualizar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showContentDialog = false }) { Text("Cancelar") }
            }
        )
    }

    episodeManagerSeries?.let { series ->
        val existingEpisodes = episodesBySeries[series.id].orEmpty()
        var seasonInput by remember(series.id, existingEpisodes.size) {
            mutableStateOf((existingEpisodes.maxOfOrNull { it.seasonNumber } ?: 1).toString())
        }
        var episodeNumInput by remember(series.id, existingEpisodes.size) {
            val nextNum = (existingEpisodes.maxOfOrNull { it.episodeNumber } ?: 0) + 1
            mutableStateOf(nextNum.toString())
        }
        var epTitle by remember(series.id, existingEpisodes.size) { mutableStateOf("") }
        var epDuration by remember(series.id, existingEpisodes.size) { mutableStateOf("42") }
        var epStreamUrl by remember(series.id, existingEpisodes.size) { mutableStateOf(series.streamUrl) }
        var epDesc by remember(series.id, existingEpisodes.size) { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { episodeManagerSeries = null },
            title = {
                Text("Capítulos: ${series.title}", fontWeight = FontWeight.Bold)
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 470.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (existingEpisodes.isEmpty()) {
                        Text(
                            text = "Aún no hay episodios/capítulos personalizados. Agrega el primero abajo:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = DetflixTextSecondary
                        )
                    } else {
                        existingEpisodes.forEach { ep ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = "T${ep.seasonNumber} E${ep.episodeNumber} • ${ep.title}",
                                            style = MaterialTheme.typography.titleSmall,
                                            color = DetflixWhite
                                        )
                                        Text(
                                            text = "${ep.durationMinutes} min",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = DetflixTextMuted
                                        )
                                    }
                                    IconButton(onClick = { onDeleteEpisode(ep) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Eliminar episodio", tint = DetflixRed)
                                    }
                                }
                            }
                        }
                    }

                    HorizontalDivider(color = DetflixBorder, modifier = Modifier.padding(vertical = 4.dp))
                    Text("Nuevo Episodio / Capítulo", style = MaterialTheme.typography.titleSmall, color = DetflixGold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = seasonInput,
                            onValueChange = { seasonInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Temp.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = episodeNumInput,
                            onValueChange = { episodeNumInput = it.filter { c -> c.isDigit() } },
                            label = { Text("Ep./Cap.") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = epDuration,
                            onValueChange = { epDuration = it.filter { c -> c.isDigit() } },
                            label = { Text("Min") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    OutlinedTextField(
                        value = epTitle,
                        onValueChange = { epTitle = it },
                        label = { Text("Título del capítulo") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = epStreamUrl,
                        onValueChange = { epStreamUrl = it },
                        label = { Text("URL Video MP4 / HLS (HTTPS)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = epDesc,
                        onValueChange = { epDesc = it },
                        label = { Text("Descripción breve (opcional)") },
                        maxLines = 2,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Button(
                        onClick = {
                            onSaveEpisode(
                                series.id,
                                seasonInput.toIntOrNull() ?: 1,
                                episodeNumInput.toIntOrNull() ?: 1,
                                epTitle,
                                epDesc,
                                series.posterUrl,
                                epStreamUrl,
                                epDuration.toIntOrNull() ?: 40
                            )
                            epTitle = ""
                            epDesc = ""
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        modifier = Modifier.fillMaxWidth().height(48.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Agregar Episodio", fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { episodeManagerSeries = null }) {
                    Text("Cerrar")
                }
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
