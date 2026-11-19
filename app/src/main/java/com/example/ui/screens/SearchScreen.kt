package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.DetflixCategory
import com.example.ui.components.DetflixArtworkImage
import com.example.ui.components.PosterCard
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.SearchFilterState

@Composable
fun SearchScreen(
    filters: SearchFilterState,
    results: List<CatalogItem>,
    recommendedFallback: List<CatalogItem>,
    isSearchingOnline: Boolean = false,
    onQueryChange: (String) -> Unit,
    onToggleType: (ContentType?) -> Unit,
    onToggleCategory: (String?) -> Unit,
    onToggleYear: (Int?) -> Unit,
    onToggleLanguage: (String?) -> Unit,
    onToggleSort: () -> Unit,
    onClearFilters: () -> Unit,
    onItemClick: (CatalogItem) -> Unit,
    onPlayItem: (CatalogItem, String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack)
            .statusBarsPadding()
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "BUSCAR",
                    style = MaterialTheme.typography.displayMedium,
                    color = DetflixWhite,
                    maxLines = 1
                )
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = DetflixSurfaceElevated,
                    border = BorderStroke(1.dp, DetflixBorder),
                    modifier = Modifier
                        .clickable(onClick = onToggleSort)
                        .testTag("search_sort_button")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Icon(
                            Icons.Filled.Sort,
                            contentDescription = null,
                            tint = DetflixRed,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (filters.sortByPopularity) "Popularidad" else "Año",
                            color = DetflixWhite,
                            style = MaterialTheme.typography.labelLarge,
                            maxLines = 1,
                            softWrap = false
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = filters.query,
                onValueChange = onQueryChange,
                placeholder = {
                    Text(
                        text = "Buscar películas, series, dramas, novelas...",
                        color = DetflixTextSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DetflixRed) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda", tint = DetflixTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = DetflixRed,
                    unfocusedBorderColor = DetflixBorder,
                    focusedContainerColor = DetflixSurface,
                    unfocusedContainerColor = DetflixSurface,
                    focusedTextColor = DetflixWhite,
                    unfocusedTextColor = DetflixWhite
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("search_input")
            )
            if (isSearchingOnline) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    color = DetflixRed,
                    trackColor = DetflixSurfaceElevated,
                    modifier = Modifier.fillMaxWidth().height(2.dp)
                )
            }
        }

        // Type & Year & Language Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(ContentType.entries) { type ->
                FilterChip(
                    selected = filters.selectedType == type,
                    onClick = { onToggleType(type) },
                    label = { Text(type.label, maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DetflixRed,
                        selectedLabelColor = DetflixWhite
                    )
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedYear == 2023,
                    onClick = { onToggleYear(2023) },
                    label = { Text("Recientes", maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DetflixRed,
                        selectedLabelColor = DetflixWhite
                    )
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedLanguage == "Español",
                    onClick = { onToggleLanguage("Español") },
                    label = { Text("Español", maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DetflixRed,
                        selectedLabelColor = DetflixWhite
                    )
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedLanguage == "Inglés",
                    onClick = { onToggleLanguage("Inglés") },
                    label = { Text("Inglés", maxLines = 1, softWrap = false) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DetflixRed,
                        selectedLabelColor = DetflixWhite
                    )
                )
            }
        }

        // Category Filter Row
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(DetflixCategory.entries) { cat ->
                FilterChip(
                    selected = filters.selectedCategory == cat.displayName,
                    onClick = { onToggleCategory(cat.displayName) },
                    label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall, maxLines = 1) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = DetflixRed,
                        selectedLabelColor = DetflixWhite
                    )
                )
            }
        }

        if (!filters.hasActiveQueryOrFilters) {
            // Netflix "Búsquedas principales" horizontal landscape list with Play buttons
            Text(
                text = "Búsquedas principales (${results.size} títulos)",
                style = MaterialTheme.typography.headlineMedium,
                color = DetflixWhite,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
            LazyColumn(
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(results, key = { "top_search_${it.id}" }) { item ->
                    NetflixSearchRowCard(
                        item = item,
                        onOpenDetail = { onItemClick(item) },
                        onPlay = { onPlayItem(item, item.episodes.firstOrNull()?.id) }
                    )
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when {
                        isSearchingOnline -> "Buscando en catálogo y en línea..."
                        results.isEmpty() -> "Sin coincidencias para «${filters.query.trim()}»"
                        else -> "Resultados encontrados (${results.size})"
                    },
                    style = MaterialTheme.typography.labelLarge,
                    color = DetflixTextSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                TextButton(onClick = onClearFilters) {
                    Text("Limpiar", color = DetflixRed)
                }
            }

            if (results.isEmpty() && !isSearchingOnline) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.SearchOff,
                        contentDescription = null,
                        tint = DetflixRed,
                        modifier = Modifier.size(46.dp)
                    )
                    Text(
                        text = "No se encontró «${filters.query.ifBlank { filters.selectedCategory ?: "este filtro" }}»",
                        style = MaterialTheme.typography.titleMedium,
                        color = DetflixWhite,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "DETFLIX reproduce títulos libres autorizados y los títulos propios que el administrador agregue desde DETFLIX Admin. Por derechos de autor, no incluye el catálogo privado de plataformas externas como Netflix.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = DetflixTextSecondary,
                        textAlign = TextAlign.Center
                    )
                    Button(
                        onClick = onClearFilters,
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver todo el catálogo (${recommendedFallback.size}+)", color = DetflixWhite)
                    }
                }
            } else {
                BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                    val gridColumns = if (maxWidth < 600.dp) GridCells.Fixed(3) else GridCells.Adaptive(minSize = 132.dp)
                    LazyVerticalGrid(
                        columns = gridColumns,
                        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(results, key = { "grid_${it.id}" }) { item ->
                            PosterCard(
                                item = item,
                                onClick = { onItemClick(item) },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun NetflixSearchRowCard(
    item: CatalogItem,
    onOpenDetail: () -> Unit,
    onPlay: () -> Unit
) {
    Surface(
        color = DetflixSurface,
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, DetflixBorder.copy(alpha = 0.45f)),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpenDetail)
            .testTag("search_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(126.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DetflixSurfaceElevated)
            ) {
                DetflixArtworkImage(
                    url = item.backdropUrl.ifBlank { item.posterUrl },
                    contentDescription = item.title,
                    categories = item.categories,
                    contentType = item.type,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = DetflixWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = "${item.type.label} • ${item.year} • ${item.ageRating.code}",
                    style = MaterialTheme.typography.labelSmall,
                    color = DetflixTextSecondary,
                    maxLines = 1
                )
                Text(
                    text = item.categories.take(3).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = DetflixTextSecondary.copy(alpha = 0.8f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            IconButton(
                onClick = onPlay,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(DetflixSurfaceElevated)
                    .testTag("search_row_play_${item.id}")
            ) {
                Icon(
                    Icons.Filled.PlayArrow,
                    contentDescription = "Reproducir ${item.title}",
                    tint = DetflixWhite
                )
            }
        }
    }
}
