package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import com.example.data.local.ProfileEntity
import com.example.data.model.CatalogItem
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.DetflixCategory
import com.example.data.model.MembershipStatus
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BadgePill
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.DetflixBrandMark
import com.example.ui.components.PosterCard
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixBorder
import com.example.ui.theme.DetflixGold
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixSurfaceElevated
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.HomeFeedState

@Composable
fun HomeScreen(
    feed: HomeFeedState,
    activeProfile: ProfileEntity?,
    favoriteIds: Set<String>,
    membershipStatus: MembershipStatus,
    daysRemaining: Int,
    unreadNotifications: Int,
    isSyncing: Boolean,
    syncMessage: String?,
    onContentClick: (CatalogItem) -> Unit,
    onPlayItem: (CatalogItem, String?) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onCategoryClick: (DetflixCategory) -> Unit,
    onNotificationsClick: () -> Unit,
    onProfilesClick: () -> Unit,
    onMembershipClick: () -> Unit,
    onTriggerSync: () -> Unit
) {
    var heroIndex by remember(feed.featured) { mutableIntStateOf(0) }
    val heroItem = feed.featured.getOrNull(heroIndex % feed.featured.size.coerceAtLeast(1))

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(DetflixBlack),
        contentPadding = PaddingValues(bottom = 96.dp)
    ) {
        // Top Cinematic Bar
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                DetflixBrandMark(size = 34.dp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (membershipStatus == MembershipStatus.ACTIVE || membershipStatus == MembershipStatus.EXPIRING_SOON)
                            DetflixGold.copy(alpha = 0.16f) else DetflixRed.copy(alpha = 0.2f),
                        border = BorderStroke(
                            1.dp,
                            if (membershipStatus == MembershipStatus.ACTIVE) DetflixGold.copy(alpha = 0.6f) else DetflixRed
                        ),
                        modifier = Modifier
                            .clickable(onClick = onMembershipClick)
                            .testTag("home_membership_pill")
                    ) {
                        Text(
                            text = if (membershipStatus == MembershipStatus.ACTIVE || membershipStatus == MembershipStatus.EXPIRING_SOON)
                                "VIP • ${daysRemaining}d" else "Activar Código",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (membershipStatus == MembershipStatus.ACTIVE) DetflixGold else DetflixWhite,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }

                    IconButton(
                        onClick = onTriggerSync,
                        modifier = Modifier.testTag("home_sync_button")
                    ) {
                        if (isSyncing) {
                            CircularProgressIndicator(Modifier.size(20.dp), color = DetflixRed, strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Filled.Sync, contentDescription = "Sincronizar catálogo", tint = DetflixWhite)
                        }
                    }

                    IconButton(
                        onClick = onNotificationsClick,
                        modifier = Modifier.testTag("home_notifications_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (unreadNotifications > 0) {
                                    Badge(containerColor = DetflixRed) {
                                        Text("$unreadNotifications", color = DetflixWhite)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                Icons.Filled.Notifications,
                                contentDescription = stringResource(id = R.string.cd_notifications),
                                tint = DetflixWhite
                            )
                        }
                    }

                    activeProfile?.let { profile ->
                        Box(
                            modifier = Modifier
                                .clickable(onClick = onProfilesClick)
                                .testTag("home_profile_avatar")
                        ) {
                            AvatarBadge(
                                avatarId = profile.avatarId,
                                name = profile.name,
                                isKids = profile.isKids,
                                size = 34.dp
                            )
                        }
                    }
                }
            }
        }

        // Optional Sync Feedback Banner
        syncMessage?.let { msg ->
            item {
                Surface(
                    color = DetflixSurfaceElevated,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = msg,
                        color = DetflixTextSecondary,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
        }

        // Hero Spotlight Banner (Películas destacadas)
        if (heroItem != null) {
            item {
                HeroSpotlightSection(
                    item = heroItem,
                    isFavorite = favoriteIds.contains(heroItem.id),
                    totalFeatured = feed.featured.size,
                    currentIndex = heroIndex % feed.featured.size.coerceAtLeast(1),
                    onSelectHeroIndex = { heroIndex = it },
                    onPlay = { onPlayItem(heroItem, heroItem.episodes.firstOrNull()?.id) },
                    onToggleFavorite = { onToggleFavorite(heroItem.id) },
                    onInfo = { onContentClick(heroItem) }
                )
            }
        }

        // 12 Categories Horizontal Bar
        item {
            Column(modifier = Modifier.padding(top = 16.dp)) {
                Text(
                    text = "EXPLORAR POR CATEGORÍA",
                    style = MaterialTheme.typography.headlineLarge,
                    color = DetflixWhite,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DetflixCategory.entries) { category ->
                        Surface(
                            shape = RoundedCornerShape(22.dp),
                            color = DetflixSurfaceElevated,
                            border = BorderStroke(1.dp, DetflixBorder),
                            modifier = Modifier
                                .height(40.dp)
                                .clickable { onCategoryClick(category) }
                                .testTag("category_chip_${category.slug}")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = category.displayName,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = DetflixWhite
                                )
                            }
                        }
                    }
                }
            }
        }

        // 1. Continuar viendo
        if (feed.continueWatching.isNotEmpty()) {
            item {
                ContinueWatchingRail(
                    entries = feed.continueWatching,
                    onClickEntry = { entry -> onPlayItem(entry.item, entry.episode?.id) }
                )
            }
        }

        // 2. Mi lista
        if (feed.myList.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "MI LISTA",
                    items = feed.myList,
                    onItemClick = onContentClick
                )
            }
        }

        // 3. Últimos estrenos
        if (feed.latestReleases.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "ÚLTIMOS ESTRENOS",
                    items = feed.latestReleases,
                    onItemClick = onContentClick
                )
            }
        }

        // 4. Series populares
        if (feed.popularSeries.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "SERIES POPULARES",
                    items = feed.popularSeries,
                    onItemClick = onContentClick
                )
            }
        }

        // 4b. Novelas y dramas
        if (feed.novelas.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "NOVELAS Y DRAMAS",
                    items = feed.novelas,
                    onItemClick = onContentClick
                )
            }
        }

        // 5. Tendencias
        if (feed.trending.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "TENDENCIAS EN DETFLIX",
                    items = feed.trending,
                    onItemClick = onContentClick
                )
            }
        }

        // 6. Películas recomendadas
        if (feed.recommended.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "RECOMENDADAS PARA TI",
                    items = feed.recommended,
                    onItemClick = onContentClick
                )
            }
        }

        // 7. Nuevas incorporaciones
        if (feed.newAdditions.isNotEmpty()) {
            item {
                CatalogRail(
                    title = "NUEVAS INCORPORACIONES",
                    items = feed.newAdditions,
                    onItemClick = onContentClick
                )
            }
        }
    }
}

@Composable
private fun HeroSpotlightSection(
    item: CatalogItem,
    isFavorite: Boolean,
    totalFeatured: Int,
    currentIndex: Int,
    onSelectHeroIndex: (Int) -> Unit,
    onPlay: () -> Unit,
    onToggleFavorite: () -> Unit,
    onInfo: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(410.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(DetflixSurface)
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
                            Color.Transparent,
                            DetflixBlack.copy(alpha = 0.45f),
                            DetflixBlack.copy(alpha = 0.96f)
                        )
                    )
                )
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                BadgePill(text = "DESTACADO", background = DetflixRed)
                BadgePill(text = item.type.label.uppercase(), background = DetflixSurfaceElevated)
                BadgePill(text = item.ageRating.code, background = DetflixSurfaceElevated)
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.displayMedium,
                color = DetflixWhite,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = item.categories.joinToString(" • ") + " • ${item.year}",
                style = MaterialTheme.typography.labelLarge,
                color = DetflixTextSecondary
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = item.description,
                style = MaterialTheme.typography.bodyMedium,
                color = DetflixTextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(14.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onPlay,
                    colors = ButtonDefaults.buttonColors(containerColor = DetflixWhite),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .testTag("hero_play_button")
                ) {
                    Icon(Icons.Filled.PlayArrow, contentDescription = null, tint = DetflixBlack)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reproducir", color = DetflixBlack, fontWeight = FontWeight.Bold)
                }
                OutlinedButton(
                    onClick = onToggleFavorite,
                    border = BorderStroke(1.dp, DetflixWhite.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .height(48.dp)
                        .testTag("hero_mylist_button")
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Check else Icons.Filled.Add,
                        contentDescription = "Mi lista",
                        tint = DetflixWhite
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Mi Lista", color = DetflixWhite)
                }
                IconButton(
                    onClick = onInfo,
                    modifier = Modifier
                        .size(48.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(DetflixSurfaceElevated.copy(alpha = 0.85f))
                        .testTag("hero_info_button")
                ) {
                    Icon(Icons.Filled.Info, contentDescription = "Más información", tint = DetflixWhite)
                }
            }
            if (totalFeatured > 1) {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(totalFeatured) { idx ->
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (idx == currentIndex) 10.dp else 7.dp)
                                .clip(CircleShape)
                                .background(if (idx == currentIndex) DetflixRed else DetflixTextSecondary.copy(alpha = 0.5f))
                                .clickable { onSelectHeroIndex(idx) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ContinueWatchingRail(
    entries: List<ContinueWatchingEntry>,
    onClickEntry: (ContinueWatchingEntry) -> Unit
) {
    Column(modifier = Modifier.padding(top = 20.dp)) {
        Text(
            text = "CONTINUAR VIENDO",
            style = MaterialTheme.typography.headlineLarge,
            color = DetflixWhite,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(entries, key = { it.item.id }) { entry ->
                ContinueWatchingCard(entry = entry, onClick = { onClickEntry(entry) })
            }
        }
    }
}

@Composable
private fun CatalogRail(
    title: String,
    items: List<CatalogItem>,
    onItemClick: (CatalogItem) -> Unit
) {
    Column(modifier = Modifier.padding(top = 22.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineLarge,
            color = DetflixWhite,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        Spacer(modifier = Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(items, key = { "${title}_${it.id}" }) { item ->
                PosterCard(item = item, onClick = { onItemClick(item) })
            }
        }
    }
}
