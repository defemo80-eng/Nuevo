package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ClosedCaption
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.EpisodeItem
import com.example.ui.components.BadgePill
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixGold
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite

@Composable
fun ContentDetailScreen(
    item: CatalogItem,
    isFavorite: Boolean,
    resumeEpisodeId: String?,
    onBack: () -> Unit,
    onPlay: (CatalogItem, EpisodeItem?) -> Unit,
    onToggleFavorite: () -> Unit
) {
    BackHandler(onBack = onBack)

    val seasons = remember(item.episodes) {
        item.episodes.map { it.seasonNumber }.distinct().sorted().ifEmpty { listOf(1) }
    }
    var selectedSeason by remember { mutableIntStateOf(seasons.first()) }
    val seasonEpisodes = remember(item.episodes, selectedSeason) {
        item.episodes.filter { it.seasonNumber == selectedSeason }
    }

    val primaryEpisode = remember(item, resumeEpisodeId) {
        if (item.type == ContentType.SERIES || item.type == ContentType.NOVELA) {
            item.episodes.firstOrNull { it.id == resumeEpisodeId } ?: item.episodes.firstOrNull()
        } else null
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack),
        contentPadding = PaddingValues(bottom = 80.dp)
    ) {
        // Backdrop Banner
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(290.dp)
            ) {
                AsyncImage(
                    model = item.backdropUrl,
                    contentDescription = item.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    DetflixBlack.copy(alpha = 0.55f),
                                    Color.Transparent,
                                    DetflixBlack
                                )
                            )
                        )
                )
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(12.dp)
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(DetflixBlack.copy(alpha = 0.65f))
                        .testTag("detail_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(id = R.string.cd_back),
                        tint = DetflixWhite
                    )
                }
            }
        }

        // Metadata & Actions
        item {
            Column(modifier = Modifier.padding(horizontal = 18.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    BadgePill(text = item.type.label.uppercase(), background = DetflixRed)
                    BadgePill(text = item.ageRating.code, background = DetflixSurfaceElevated)
                    BadgePill(text = "${item.year}", background = DetflixSurfaceElevated)
                    BadgePill(text = "${item.durationMinutes} min", background = DetflixSurfaceElevated)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Filled.Star, contentDescription = null, tint = DetflixGold, modifier = Modifier.size(15.dp))
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(String.format("%.1f", item.rating), color = DetflixWhite, style = MaterialTheme.typography.labelLarge)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.displayMedium,
                    color = DetflixWhite
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = item.categories.joinToString(" • "),
                    style = MaterialTheme.typography.labelLarge,
                    color = DetflixRed
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = { onPlay(item, primaryEpisode) },
                        colors = ButtonDefaults.buttonColors(containerColor = DetflixRed),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(50.dp)
                            .testTag("detail_play_button")
                    ) {
                        Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DetflixWhite)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (primaryEpisode != null) "Reproducir T${primaryEpisode.seasonNumber}:E${primaryEpisode.episodeNumber}" else "Reproducir Ahora",
                            color = DetflixWhite,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    OutlinedButton(
                        onClick = onToggleFavorite,
                        border = BorderStroke(1.dp, DetflixBorder),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .height(50.dp)
                            .testTag("detail_favorite_button")
                    ) {
                        Icon(
                            if (isFavorite) Icons.Filled.Check else Icons.Filled.Add,
                            contentDescription = "Mi lista",
                            tint = DetflixWhite
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (isFavorite) "En Mi Lista" else "Mi Lista", color = DetflixWhite)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = DetflixWhite
                )

                Spacer(modifier = Modifier.height(14.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = DetflixSurface),
                    border = BorderStroke(1.dp, DetflixBorder),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Dirección: ${item.director}", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                        Text("Elenco: ${item.cast.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.Language, contentDescription = null, tint = DetflixTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Idiomas: ${item.availableLanguages.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Filled.ClosedCaption, contentDescription = null, tint = DetflixTextSecondary, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Subtítulos: ${item.subtitles.joinToString(", ")}", style = MaterialTheme.typography.bodyMedium, color = DetflixTextSecondary)
                        }
                        Text("Fuente autorizada: ${item.sourceName}", style = MaterialTheme.typography.labelSmall, color = DetflixGold)
                    }
                }
            }
        }

        // Seasons & Episodes for Series and Novelas
        if ((item.type == ContentType.SERIES || item.type == ContentType.NOVELA) && item.episodes.isNotEmpty()) {
            item {
                Column(modifier = Modifier.padding(top = 22.dp, start = 18.dp, end = 18.dp)) {
                    Text(
                        text = if (item.type == ContentType.NOVELA) "TEMPORADAS Y CAPÍTULOS" else "TEMPORADAS Y EPISODIOS",
                        style = MaterialTheme.typography.headlineLarge,
                        color = DetflixWhite
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(seasons) { season ->
                            FilterChip(
                                selected = selectedSeason == season,
                                onClick = { selectedSeason = season },
                                label = { Text("Temporada $season") },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = DetflixRed,
                                    selectedLabelColor = DetflixWhite
                                ),
                                modifier = Modifier.testTag("season_chip_$season")
                            )
                        }
                    }
                }
            }

            items(seasonEpisodes, key = { it.id }) { ep ->
                EpisodeRowCard(
                    episode = ep,
                    onClick = { onPlay(item, ep) }
                )
            }
        }
    }
}

@Composable
private fun EpisodeRowCard(
    episode: EpisodeItem,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp, vertical = 6.dp)
            .clickable(onClick = onClick)
            .testTag("episode_card_${episode.id}"),
        colors = CardDefaults.cardColors(containerColor = DetflixSurface),
        border = BorderStroke(1.dp, DetflixBorder),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(124.dp)
                    .height(74.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(DetflixSurfaceElevated)
            ) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = episode.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(DetflixBlack.copy(alpha = 0.7f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DetflixWhite, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${episode.episodeNumber}. ${episode.title}",
                    style = MaterialTheme.typography.titleMedium,
                    color = DetflixWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${episode.durationMinutes} min",
                    style = MaterialTheme.typography.labelSmall,
                    color = DetflixRed
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = episode.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = DetflixTextSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
