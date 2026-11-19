package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.CatalogItem
import com.example.data.model.ContinueWatchingEntry
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.PosterCard
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite

@Composable
fun MyListScreen(
    favorites: List<CatalogItem>,
    continueWatching: List<ContinueWatchingEntry>,
    onItemClick: (CatalogItem) -> Unit,
    onContinueClick: (ContinueWatchingEntry) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack)
            .statusBarsPadding()
    ) {
        Text(
            text = "MI LISTA",
            style = MaterialTheme.typography.displayMedium,
            color = DetflixWhite,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 14.dp)
        )

        if (favorites.isEmpty() && continueWatching.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Filled.BookmarkBorder,
                    contentDescription = null,
                    tint = DetflixTextSecondary,
                    modifier = Modifier.size(60.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Tu lista está vacía",
                    style = MaterialTheme.typography.titleLarge,
                    color = DetflixWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Agrega películas, series o documentales con el botón '+ Mi Lista' para verlos cuando quieras.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetflixTextSecondary,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 136.dp),
                contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 96.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                if (continueWatching.isNotEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = "CONTINUAR VIENDO",
                                style = MaterialTheme.typography.headlineLarge,
                                color = DetflixWhite
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                items(continueWatching, key = { it.item.id }) { entry ->
                                    ContinueWatchingCard(
                                        entry = entry,
                                        onClick = { onContinueClick(entry) }
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(18.dp))
                            Text(
                                text = "FAVORITOS GUARDADOS",
                                style = MaterialTheme.typography.headlineLarge,
                                color = DetflixWhite
                            )
                        }
                    }
                }
                items(favorites, key = { it.id }) { item ->
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
