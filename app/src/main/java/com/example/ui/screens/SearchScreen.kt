package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.DetflixCategory
import com.example.ui.components.PosterCard
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.SearchFilterState

@Composable
fun SearchScreen(
    filters: SearchFilterState,
    results: List<CatalogItem>,
    onQueryChange: (String) -> Unit,
    onToggleType: (ContentType?) -> Unit,
    onToggleCategory: (String?) -> Unit,
    onToggleYear: (Int?) -> Unit,
    onToggleLanguage: (String?) -> Unit,
    onToggleSort: () -> Unit,
    onClearFilters: () -> Unit,
    onItemClick: (CatalogItem) -> Unit
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
                Text("BUSCADOR INTELIGENTE", style = MaterialTheme.typography.displayMedium, color = DetflixWhite)
                TextButton(onClick = onToggleSort, modifier = Modifier.testTag("search_sort_button")) {
                    Icon(Icons.Filled.Sort, contentDescription = null, tint = DetflixRed)
                    Text(
                        text = if (filters.sortByPopularity) " Popularidad" else " Año",
                        color = DetflixWhite
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = filters.query,
                onValueChange = onQueryChange,
                placeholder = { Text("Películas, series, actores, directores, géneros...", color = DetflixTextSecondary) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = DetflixRed) },
                trailingIcon = {
                    if (filters.query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda", tint = DetflixTextSecondary)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(14.dp),
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
                    label = { Text(type.label) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DetflixRed, selectedLabelColor = DetflixWhite)
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedYear == 2000,
                    onClick = { onToggleYear(2000) },
                    label = { Text("Desde 2000") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DetflixRed, selectedLabelColor = DetflixWhite)
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedLanguage == "Español",
                    onClick = { onToggleLanguage("Español") },
                    label = { Text("Español") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DetflixRed, selectedLabelColor = DetflixWhite)
                )
            }
            item {
                FilterChip(
                    selected = filters.selectedLanguage == "Inglés",
                    onClick = { onToggleLanguage("Inglés") },
                    label = { Text("Inglés") },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DetflixRed, selectedLabelColor = DetflixWhite)
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
                    label = { Text(cat.displayName, style = MaterialTheme.typography.labelSmall) },
                    colors = FilterChipDefaults.filterChipColors(selectedContainerColor = DetflixRed, selectedLabelColor = DetflixWhite)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${results.size} resultados encontrados",
                style = MaterialTheme.typography.labelLarge,
                color = DetflixTextSecondary
            )
            if (filters.query.isNotBlank() || filters.selectedType != null || filters.selectedCategory != null || filters.selectedYear != null || filters.selectedLanguage != null) {
                TextButton(onClick = onClearFilters) {
                    Text("Limpiar filtros", color = DetflixRed)
                }
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 136.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(results, key = { it.id }) { item ->
                PosterCard(
                    item = item,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
