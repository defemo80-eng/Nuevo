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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AlertDialog
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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.local.ProfileEntity
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.ContinueWatchingEntry
import com.example.data.model.DetflixCategory
import com.example.data.model.MembershipStatus
import com.example.ui.components.AvatarBadge
import com.example.ui.components.BadgePill
import com.example.ui.components.ContinueWatchingCard
import com.example.ui.components.DetflixArtworkImage
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

private enum class HomeQuickFilter(val label: String) {
    ALL("Todo"),
    SERIES("Series"),
    MOVIES("Películas"),
    DRAMAS("Dramas"),
    NOVELAS("Novelas")
}

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
    var selectedQuickFilter by remember { mutableStateOf(HomeQuickFilter.ALL) }
    var showCategoriesModal by remember { mutableStateOf(false) }

    val filteredCatalog = remember(feed.allItems, selectedQuickFilter) {
        when (selectedQuickFilter) {
            HomeQuickFilter.ALL -> feed.allItems
            HomeQuickFilter.SERIES -> feed.allItems.filter { it.type == ContentType.SERIES }
            HomeQuickFilter.MOVIES -> feed.allItems.filter { it.type == ContentType.MOVIE }
            HomeQuickFilter.DRAMAS -> feed.allItems.filter {
                it.categories.any { c -> c.equals("Drama", ignoreCase = true) }
            }
            HomeQuickFilter.NOVELAS -> feed.allItems.filter {
                it.type == ContentType.NOVELA || it.categories.any { c -> c.equals("Novelas", ignoreCase = true) }
            }
        }
    }

    val heroCandidates = remember(feed.featured, filteredCatalog, selectedQuickFilter) {
        if (selectedQuickFilter == HomeQuickFilter.ALL) feed.featured
        else filteredCatalog.sortedByDescending { it.popularityScore }.take(5)
    }
    var heroIndex by remember(heroCandidates) { mutableIntStateOf(0) }
    val heroItem = heroCandidates.getOrNull(heroIndex % heroCandidates.size.coerceAtLeast(1))

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
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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

        // Netflix-style Quick Filter Bar (Todo / Series / Películas / Dramas / Novelas / Categorías ▾)
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(HomeQuickFilter.entries) { filter ->
                    val selected = selectedQuickFilter == filter
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (selected) DetflixWhite else Color.Transparent,
                        border = BorderStroke(1.dp, if (selected) DetflixWhite else DetflixBorder),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { selectedQuickFilter = filter }
                            .testTag("home_filter_${filter.name.lowercase()}")
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = filter.label,
                                style = MaterialTheme.typography.labelLarge,
                                color = if (selected) DetflixBlack else DetflixWhite,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
                item {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = DetflixSurfaceElevated,
                        border = BorderStroke(1.dp, DetflixBorder),
                        modifier = Modifier
                            .height(36.dp)
                            .clickable { showCategoriesModal = true }
                            .testTag("home_categories_dropdown")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 14.dp)
                        ) {
                            Text(
                                text = "Categorías",
                                style = MaterialTheme.typography.labelLarge,
                                color = DetflixWhite,
                                maxLines = 1,
                                softWrap = false
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Filled.KeyboardArrowDown,
                                contentDescription = "Abrir categorías",
                                tint = DetflixWhite,
                                modifier = Modifier.size(18.dp)
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

        // Hero Spotlight Banner
        if (heroItem != null) {
            item {
                HeroSpotlightSection(
                    item = heroItem,
                    isFavorite = favoriteIds.contains(heroItem.id),
                    totalFeatured = heroCandidates.size,
                    currentIndex = heroIndex % heroCandidates.size.coerceAtLeast(1),
                    onSelectHeroIndex = { heroIndex = it },
                    onPlay = { onPlayItem(heroItem, heroItem.episodes.firstOrNull()?.id) },
                    onToggleFavorite = { onToggleFavorite(heroItem.id) },
                    onInfo = { onContentClick(heroItem) }
                )
            }
        }

        if (selectedQuickFilter != HomeQuickFilter.ALL) {
            val popularSubset = filteredCatalog.sortedByDescending { it.popularityScore }
            val recentSubset = filteredCatalog.sortedByDescending { it.year }
            item {
                CatalogRail(
                    title = "${selectedQuickFilter.label.uppercase()} MÁS POPULARES (${popularSubset.size})",
                    items = popularSubset,
                    onItemClick = onContentClick
                )
            }
            if (recentSubset.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "ESTRENOS EN ${selectedQuickFilter.label.uppercase()}",
                        items = recentSubset,
                        onItemClick = onContentClick
                    )
                }
            }
        } else {
            if (feed.continueWatching.isNotEmpty()) {
                item {
                    ContinueWatchingRail(
                        entries = feed.continueWatching,
                        onClickEntry = { entry -> onPlayItem(entry.item, entry.episode?.id) }
                    )
                }
            }

            if (feed.topTen.isNotEmpty()) {
                item {
                    TopTenRail(
                        title = "TOP 10 EN DETFLIX HOY",
                        items = feed.topTen,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.myList.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "MI LISTA",
                        items = feed.myList,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.latestReleases.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "ÚLTIMOS ESTRENOS",
                        items = feed.latestReleases,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.popularSeries.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "SERIES POPULARES",
                        items = feed.popularSeries,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.dramas.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "DRAMAS INTENSOS",
                        items = feed.dramas,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.novelas.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "NOVELAS Y ROMANCE",
                        items = feed.novelas,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.actionSciFi.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "ACCIÓN Y CIENCIA FICCIÓN",
                        items = feed.actionSciFi,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.horrorSuspense.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "TERROR Y SUSPENSO",
                        items = feed.horrorSuspense,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.comedyFamily.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "COMEDIA, ROMANCE E INFANTIL",
                        items = feed.comedyFamily,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.docsAndSports.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "DOCUMENTALES Y DEPORTES",
                        items = feed.docsAndSports,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.recommended.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "RECOMENDADAS PARA TI",
                        items = feed.recommended,
                        onItemClick = onContentClick
                    )
                }
            }

            if (feed.allItems.isNotEmpty()) {
                item {
                    CatalogRail(
                        title = "CATÁLOGO COMPLETO (${feed.allItems.size} TÍTULOS)",
                        items = feed.allItems,
                        onItemClick = onContentClick
                    )
                }
            }
        }

        // All 13 Categories Explorer
        item {
            Column(modifier = Modifier.padding(top = 22.dp)) {
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
                                    color = DetflixWhite,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCategoriesModal) {
        AlertDialog(
            onDismissRequest = { showCategoriesModal = false },
            containerColor = DetflixSurface,
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Todas las Categorías", color = DetflixWhite, style = MaterialTheme.typography.headlineMedium)
                    IconButton(onClick = { showCategoriesModal = false }) {
                        Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = DetflixWhite)
                    }
                }
            },
            text = {
                LazyColumn(
                    modifier = Modifier.height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(DetflixCategory.entries) { cat ->
                        Surface(
                            color = DetflixSurfaceElevated,
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showCategoriesModal = false
                                    onCategoryClick(cat)
                                }
                        ) {
                            Column(modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
                                Text(cat.displayName, color = DetflixWhite, style = MaterialTheme.typography.titleMedium)
                                Text(cat.tagline, color = DetflixTextSecondary, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCategoriesModal = false }) {
                    Text("Cerrar", color = DetflixRed)
                }
            }
        )
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
            .clickable(onClick = onInfo)
    ) {
        DetflixArtworkImage(
            url = item.backdropUrl,
            contentDescription = item.title,
            categories = item.categories,
            contentType = item.type,
            modifier = Modifier.fillMaxSize()
        )
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Transparent,
                            DetflixBlack.copy(alpha = 0.4f),
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
                color = DetflixTextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
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
private fun TopTenRail(
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
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            itemsIndexed(items, key = { idx, it -> "top10_${idx}_${it.id}" }) { index, item ->
                Row(
                    verticalAlignment = Alignment.Bottom,
                    modifier = Modifier.clickable { onItemClick(item) }
                ) {
                    Text(
                        text = "${index + 1}",
                        fontSize = 64.sp,
                        fontWeight = FontWeight.Black,
                        color = DetflixRed.copy(alpha = 0.85f),
                        modifier = Modifier.padding(end = 2.dp, bottom = 2.dp)
                    )
                    PosterCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        width = 122.dp
                    )
                }
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
