package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CardMembership
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.R
import com.example.data.model.CatalogItem
import com.example.data.model.DetflixCategory
import com.example.data.model.MembershipStatus
import com.example.data.repository.DetflixRepository
import com.example.ui.components.DetflixBrandMark
import com.example.ui.components.MembershipExpiredGateDialog
import com.example.ui.theme.DetflixBlack
import com.example.ui.theme.DetflixRed
import com.example.ui.theme.DetflixSurface
import com.example.ui.theme.DetflixTextSecondary
import com.example.ui.theme.DetflixWhite
import com.example.ui.viewmodel.AuthProfileViewModel
import com.example.ui.viewmodel.CatalogViewModel
import com.example.ui.viewmodel.MembershipPlayerViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private sealed class DetflixTab(
    val route: String,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
) {
    data object Home : DetflixTab("home", R.string.nav_home, Icons.Filled.Home, Icons.Outlined.Home)
    data object Search : DetflixTab("search", R.string.nav_search, Icons.Filled.Search, Icons.Outlined.Search)
    data object MyList : DetflixTab("my_list", R.string.nav_my_list, Icons.Filled.Bookmark, Icons.Outlined.BookmarkBorder)
    data object Membership : DetflixTab("membership", R.string.nav_membership, Icons.Filled.CardMembership, Icons.Outlined.CardMembership)
    data object Profiles : DetflixTab("profiles", R.string.nav_profile, Icons.Filled.Person, Icons.Outlined.Person)
}

@Composable
fun DetflixRootApp() {
    val context = LocalContext.current
    val repository = remember(context) { DetflixRepository(context.applicationContext) }

    val authVm: AuthProfileViewModel = viewModel(factory = AuthProfileViewModel.Factory(repository))
    val currentUserId by authVm.currentUserId.collectAsStateWithLifecycle()
    val currentProfileId by authVm.currentProfileId.collectAsStateWithLifecycle()
    val savedAccounts by authVm.savedAccounts.collectAsStateWithLifecycle()
    val currentUser by authVm.currentUser.collectAsStateWithLifecycle()
    val profiles by authVm.profiles.collectAsStateWithLifecycle()
    val activeProfile by authVm.activeProfile.collectAsStateWithLifecycle()
    val authUiState by authVm.authUiState.collectAsStateWithLifecycle()

    if (currentUserId == null) {
        AuthScreen(
            uiState = authUiState,
            savedAccounts = savedAccounts,
            onEnterWithNameAndCode = authVm::enterWithNameAndCode,
            onEnterSavedAccount = authVm::enterSavedAccount,
            onClearMessages = authVm::clearMessages
        )
        return
    }

    val catalogVm: CatalogViewModel = viewModel(
        key = "catalog_${currentUserId}",
        factory = CatalogViewModel.Factory(repository)
    )
    val membershipVm: MembershipPlayerViewModel = viewModel(
        key = "membership_${currentUserId}",
        factory = MembershipPlayerViewModel.Factory(repository)
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner, currentUserId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                membershipVm.refreshExpiration()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(currentUserId) {
        membershipVm.refreshExpiration()
        while (isActive) {
            delay(4_000L)
            membershipVm.verifySessionNotRevoked()
        }
    }

    val homeFeed by catalogVm.homeFeed.collectAsStateWithLifecycle()
    val favoriteIds by catalogVm.favoriteIds.collectAsStateWithLifecycle()
    val searchFilters by catalogVm.searchFilters.collectAsStateWithLifecycle()
    val searchResults by catalogVm.searchResults.collectAsStateWithLifecycle()
    val notifications by catalogVm.notifications.collectAsStateWithLifecycle()
    val unreadNotifications by catalogVm.unreadNotificationsCount.collectAsStateWithLifecycle()
    val syncLogs by catalogVm.syncLogs.collectAsStateWithLifecycle()
    val isSyncing by catalogVm.isSyncing.collectAsStateWithLifecycle()
    val syncMessage by catalogVm.syncMessage.collectAsStateWithLifecycle()

    val membershipState by membershipVm.membershipUiState.collectAsStateWithLifecycle()
    val redeemBanner by membershipVm.redeemBanner.collectAsStateWithLifecycle()
    val renewals by membershipVm.renewals.collectAsStateWithLifecycle()
    val cloudGatewayUrl by membershipVm.cloudGatewayUrl.collectAsStateWithLifecycle()
    val remoteCatalogUrl by membershipVm.remoteCatalogUrl.collectAsStateWithLifecycle()
    val preferredQuality by membershipVm.preferredQuality.collectAsStateWithLifecycle()
    val autoPlayNext by membershipVm.autoPlayNext.collectAsStateWithLifecycle()
    val subtitlesEnabled by membershipVm.subtitlesEnabled.collectAsStateWithLifecycle()

    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route ?: DetflixTab.Home.route

    var showMembershipGate by remember { mutableStateOf(false) }

    fun attemptPlay(item: CatalogItem, episodeId: String?) {
        scope.launch {
            val validSession = membershipVm.verifySessionNotRevoked()
            if (!validSession) return@launch
            val isMembershipValid = membershipState.status == MembershipStatus.ACTIVE ||
                membershipState.status == MembershipStatus.EXPIRING_SOON
            if (!isMembershipValid) {
                showMembershipGate = true
            } else {
                val epArg = episodeId ?: "none"
                navController.navigate("player/${item.id}/$epArg")
            }
        }
    }

    val tabs = listOf(
        DetflixTab.Home,
        DetflixTab.Search,
        DetflixTab.MyList,
        DetflixTab.Membership,
        DetflixTab.Profiles
    )
    val showChrome = currentRoute in tabs.map { it.route }

    BoxWithConstraints(modifier = Modifier.fillMaxSize().background(DetflixBlack)) {
        val useRail = maxWidth >= 600.dp

        Scaffold(
            containerColor = DetflixBlack,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                if (showChrome && !useRail) {
                    NavigationBar(
                        containerColor = DetflixSurface,
                        modifier = Modifier.navigationBarsPadding()
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationBarItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(DetflixTab.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationBarItemDefaults.colors(
                                    selectedIconColor = DetflixRed,
                                    selectedTextColor = DetflixWhite,
                                    unselectedIconColor = DetflixTextSecondary,
                                    unselectedTextColor = DetflixTextSecondary,
                                    indicatorColor = DetflixRed.copy(alpha = 0.16f)
                                ),
                                modifier = Modifier.testTag("nav_tab_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
                if (showChrome && useRail) {
                    NavigationRail(
                        containerColor = DetflixSurface,
                        header = {
                            Box(modifier = Modifier.padding(vertical = 12.dp)) {
                                DetflixBrandMark(size = 34.dp, showWordmark = false)
                            }
                        }
                    ) {
                        tabs.forEach { tab ->
                            val selected = currentRoute == tab.route
                            val label = stringResource(id = tab.labelRes)
                            NavigationRailItem(
                                selected = selected,
                                onClick = {
                                    navController.navigate(tab.route) {
                                        popUpTo(DetflixTab.Home.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                icon = {
                                    Icon(
                                        if (selected) tab.selectedIcon else tab.unselectedIcon,
                                        contentDescription = label
                                    )
                                },
                                label = { Text(label) },
                                colors = NavigationRailItemDefaults.colors(
                                    selectedIconColor = DetflixRed,
                                    selectedTextColor = DetflixWhite,
                                    unselectedIconColor = DetflixTextSecondary,
                                    unselectedTextColor = DetflixTextSecondary,
                                    indicatorColor = DetflixRed.copy(alpha = 0.16f)
                                )
                            )
                        }
                    }
                }

                NavHost(
                    navController = navController,
                    startDestination = DetflixTab.Home.route,
                    modifier = Modifier.weight(1f).fillMaxSize()
                ) {
                    composable(DetflixTab.Home.route) {
                        HomeScreen(
                            feed = homeFeed,
                            activeProfile = activeProfile,
                            favoriteIds = favoriteIds,
                            membershipStatus = membershipState.status,
                            daysRemaining = membershipState.daysRemaining,
                            unreadNotifications = unreadNotifications,
                            isSyncing = isSyncing,
                            syncMessage = syncMessage,
                            onContentClick = { item -> navController.navigate("detail/${item.id}") },
                            onPlayItem = { item, epId -> attemptPlay(item, epId) },
                            onToggleFavorite = catalogVm::toggleFavorite,
                            onCategoryClick = { cat -> navController.navigate("category/${cat.slug}") },
                            onNotificationsClick = { navController.navigate("notifications") },
                            onProfilesClick = { navController.navigate(DetflixTab.Profiles.route) },
                            onMembershipClick = { navController.navigate(DetflixTab.Membership.route) },
                            onTriggerSync = catalogVm::triggerCatalogSync
                        )
                    }

                    composable(DetflixTab.Search.route) {
                        SearchScreen(
                            filters = searchFilters,
                            results = searchResults,
                            recommendedFallback = homeFeed.recommended,
                            onQueryChange = catalogVm::updateSearchQuery,
                            onToggleType = catalogVm::updateTypeFilter,
                            onToggleCategory = catalogVm::updateCategoryFilter,
                            onToggleYear = catalogVm::updateYearFilter,
                            onToggleLanguage = catalogVm::updateLanguageFilter,
                            onToggleSort = catalogVm::toggleSortOrder,
                            onClearFilters = catalogVm::clearFilters,
                            onItemClick = { item -> navController.navigate("detail/${item.id}") },
                            onPlayItem = { item, epId -> attemptPlay(item, epId) }
                        )
                    }

                    composable(DetflixTab.MyList.route) {
                        MyListScreen(
                            favorites = homeFeed.myList,
                            continueWatching = homeFeed.continueWatching,
                            onItemClick = { item -> navController.navigate("detail/${item.id}") },
                            onContinueClick = { entry -> attemptPlay(entry.item, entry.episode?.id) }
                        )
                    }

                    composable(DetflixTab.Membership.route) {
                        MembershipScreen(
                            uiState = membershipState,
                            redeemBanner = redeemBanner,
                            renewals = renewals,
                            cloudGatewayUrl = cloudGatewayUrl,
                            remoteCatalogUrl = remoteCatalogUrl,
                            onRedeemCode = membershipVm::redeemCode,
                            onClearMessage = membershipVm::clearRedeemMessage,
                            onSaveGatewayUrl = membershipVm::updateCloudGatewayUrl,
                            onSaveRemoteCatalogUrl = membershipVm::updateRemoteCatalogUrl
                        )
                    }

                    composable(DetflixTab.Profiles.route) {
                        ProfileSelectorScreen(
                            user = currentUser,
                            profiles = profiles,
                            activeProfileId = currentProfileId,
                            onSelectProfile = authVm::selectProfile,
                            onSaveProfile = authVm::saveProfile,
                            onDeleteProfile = authVm::deleteProfile,
                            onLogout = authVm::logout,
                            onContinueToHome = {
                                navController.navigate(DetflixTab.Home.route) {
                                    popUpTo(DetflixTab.Home.route) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(
                        route = "category/{slug}",
                        arguments = listOf(navArgument("slug") { type = NavType.StringType })
                    ) { entry ->
                        val slug = entry.arguments?.getString("slug").orEmpty()
                        val cat = DetflixCategory.fromName(slug) ?: DetflixCategory.ACCION
                        CategoryScreen(
                            category = cat,
                            allItems = homeFeed.allItems,
                            onBack = { navController.popBackStack() },
                            onItemClick = { item -> navController.navigate("detail/${item.id}") }
                        )
                    }

                    composable(
                        route = "detail/{contentId}",
                        arguments = listOf(navArgument("contentId") { type = NavType.StringType })
                    ) { entry ->
                        val cid = entry.arguments?.getString("contentId").orEmpty()
                        val item = homeFeed.allItems.firstOrNull { it.id == cid }
                        val resumeEp = homeFeed.continueWatching.firstOrNull { it.item.id == cid }?.episode?.id
                        if (item != null) {
                            ContentDetailScreen(
                                item = item,
                                isFavorite = favoriteIds.contains(item.id),
                                resumeEpisodeId = resumeEp,
                                onBack = { navController.popBackStack() },
                                onPlay = { content, ep -> attemptPlay(content, ep?.id) },
                                onToggleFavorite = { catalogVm.toggleFavorite(item.id) }
                            )
                        }
                    }

                    composable(
                        route = "player/{contentId}/{episodeId}",
                        arguments = listOf(
                            navArgument("contentId") { type = NavType.StringType },
                            navArgument("episodeId") { type = NavType.StringType }
                        )
                    ) { entry ->
                        val cid = entry.arguments?.getString("contentId").orEmpty()
                        val epArg = entry.arguments?.getString("episodeId").orEmpty()
                        val item = homeFeed.allItems.firstOrNull { it.id == cid }
                        val episode = if (epArg == "none") null else item?.episodes?.firstOrNull { it.id == epArg }
                        if (item != null) {
                            PlayerScreen(
                                item = item,
                                episode = episode,
                                initialQuality = preferredQuality,
                                initialSubtitlesEnabled = subtitlesEnabled,
                                autoPlayNextEnabled = autoPlayNext,
                                onGetResumePosition = membershipVm::getResumePosition,
                                onSaveProgress = membershipVm::saveProgress,
                                onSavePlayerPrefs = membershipVm::savePlayerPreferences,
                                onPlayNextEpisode = { nextEp ->
                                    navController.navigate("player/${item.id}/${nextEp.id}") {
                                        popUpTo("player/$cid/$epArg") { inclusive = true }
                                    }
                                },
                                onBack = { navController.popBackStack() }
                            )
                        }
                    }

                    composable("notifications") {
                        NotificationsScreen(
                            notifications = notifications,
                            syncLogs = syncLogs,
                            isSyncing = isSyncing,
                            onBack = { navController.popBackStack() },
                            onMarkAllRead = catalogVm::markNotificationsRead,
                            onSyncNow = catalogVm::triggerCatalogSync
                        )
                    }
                }
            }
        }
    }

    if (showMembershipGate) {
        MembershipExpiredGateDialog(
            onGoToMembership = {
                showMembershipGate = false
                navController.navigate(DetflixTab.Membership.route) {
                    launchSingleTop = true
                }
            },
            onDismiss = { showMembershipGate = false }
        )
    }
}
