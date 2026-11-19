package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.NotificationEntity
import com.example.data.local.ProfileEntity
import com.example.data.local.SyncLogEntity
import com.example.data.model.CatalogItem
import com.example.data.model.ContentType
import com.example.data.model.ContinueWatchingEntry
import com.example.data.repository.DetflixRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.Normalizer

data class SearchFilterState(
    val query: String = "",
    val selectedType: ContentType? = null,
    val selectedCategory: String? = null,
    val selectedYear: Int? = null,
    val selectedLanguage: String? = null,
    val sortByPopularity: Boolean = true
) {
    val hasActiveQueryOrFilters: Boolean
        get() = query.isNotBlank() ||
            selectedType != null ||
            selectedCategory != null ||
            selectedYear != null ||
            selectedLanguage != null
}

data class HomeFeedState(
    val featured: List<CatalogItem> = emptyList(),
    val topTen: List<CatalogItem> = emptyList(),
    val latestReleases: List<CatalogItem> = emptyList(),
    val popularSeries: List<CatalogItem> = emptyList(),
    val dramas: List<CatalogItem> = emptyList(),
    val novelas: List<CatalogItem> = emptyList(),
    val actionSciFi: List<CatalogItem> = emptyList(),
    val horrorSuspense: List<CatalogItem> = emptyList(),
    val comedyFamily: List<CatalogItem> = emptyList(),
    val docsAndSports: List<CatalogItem> = emptyList(),
    val recommended: List<CatalogItem> = emptyList(),
    val trending: List<CatalogItem> = emptyList(),
    val continueWatching: List<ContinueWatchingEntry> = emptyList(),
    val myList: List<CatalogItem> = emptyList(),
    val newAdditions: List<CatalogItem> = emptyList(),
    val allItems: List<CatalogItem> = emptyList()
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
class CatalogViewModel(private val repository: DetflixRepository) : ViewModel() {

    private val currentUserId: StateFlow<String?> = repository.observeCurrentUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val currentProfileId: StateFlow<String?> = repository.observeCurrentProfileId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val activeProfile: StateFlow<ProfileEntity?> = currentProfileId
        .flatMapLatest { pid -> if (pid == null) flowOf(null) else repository.observeProfile(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val favoriteIds: StateFlow<Set<String>> = currentProfileId
        .flatMapLatest { pid -> if (pid == null) flowOf(emptySet()) else repository.observeFavoritesIds(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    private val continueWatchingFlow = currentProfileId
        .flatMapLatest { pid -> if (pid == null) flowOf(emptyList()) else repository.observeContinueWatching(pid) }

    private fun normalize(text: String): String {
        val decomposed = Normalizer.normalize(text.lowercase(), Normalizer.Form.NFD)
        return Regex("\\p{InCombiningDiacriticalMarks}+").replace(decomposed, "")
    }

    val homeFeed: StateFlow<HomeFeedState> = combine(
        repository.observeCatalogWithEpisodes(),
        activeProfile,
        favoriteIds,
        continueWatchingFlow
    ) { rawCatalog, profile, favs, continueList ->
        val catalog = if (profile?.isKids == true) {
            rawCatalog.filter {
                it.ageRating.isAllowedForKids() ||
                    it.categories.any { c -> c.equals("Infantil", ignoreCase = true) || c.equals("Animación", ignoreCase = true) }
            }
        } else {
            rawCatalog
        }

        val preferredGenres = profile?.preferredGenres
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        val watchedCategories = continueList
            .flatMap { it.item.categories }
            .groupingBy { it }
            .eachCount()

        val recommended = catalog
            .sortedByDescending { item ->
                val genreBoost = item.categories.sumOf { cat ->
                    (watchedCategories[cat] ?: 0) * 15 + if (preferredGenres.contains(cat)) 25 else 0
                }
                item.popularityScore + genreBoost
            }
            .take(15)

        HomeFeedState(
            featured = catalog.filter { it.isFeatured }.ifEmpty { catalog.take(5) },
            topTen = catalog.sortedByDescending { it.popularityScore }.take(10),
            latestReleases = catalog.filter { it.isNewRelease }.sortedByDescending { it.year },
            popularSeries = catalog.filter { it.type == ContentType.SERIES }.sortedByDescending { it.popularityScore },
            dramas = catalog.filter { item ->
                item.categories.any { it.equals("Drama", ignoreCase = true) }
            }.sortedByDescending { it.popularityScore },
            novelas = catalog.filter {
                it.type == ContentType.NOVELA || it.categories.any { c -> c.equals("Novelas", ignoreCase = true) }
            }.sortedByDescending { it.popularityScore },
            actionSciFi = catalog.filter { item ->
                item.categories.any {
                    it.equals("Acción", true) || it.equals("Ciencia ficción", true) || it.equals("Aventura", true)
                }
            }.sortedByDescending { it.popularityScore },
            horrorSuspense = catalog.filter { item ->
                item.categories.any {
                    it.equals("Terror", true) || it.equals("Suspenso", true)
                }
            }.sortedByDescending { it.popularityScore },
            comedyFamily = catalog.filter { item ->
                item.categories.any {
                    it.equals("Comedia", true) || it.equals("Romance", true) || it.equals("Infantil", true) || it.equals("Animación", true)
                }
            }.sortedByDescending { it.popularityScore },
            docsAndSports = catalog.filter { item ->
                item.type == ContentType.DOCUMENTARY || item.categories.any {
                    it.equals("Documentales", true) || it.equals("Deportes", true)
                }
            }.sortedByDescending { it.popularityScore },
            recommended = recommended,
            trending = catalog.filter { it.isTrending }.sortedByDescending { it.popularityScore },
            continueWatching = continueList.filter { cw -> catalog.any { it.id == cw.item.id } },
            myList = catalog.filter { favs.contains(it.id) },
            newAdditions = catalog.filter { it.isNewAddition }.sortedByDescending { it.popularityScore },
            allItems = catalog
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), HomeFeedState())

    private val _searchFilters = MutableStateFlow(SearchFilterState())
    val searchFilters: StateFlow<SearchFilterState> = _searchFilters.asStateFlow()

    private val _isSearchingOnline = MutableStateFlow(false)
    val isSearchingOnline: StateFlow<Boolean> = _isSearchingOnline.asStateFlow()

    val searchResults: StateFlow<List<CatalogItem>> = combine(
        homeFeed,
        _searchFilters
    ) { feed, filters ->
        val q = normalize(filters.query.trim())
        feed.allItems.filter { item ->
            val matchesQuery = q.isBlank() ||
                normalize(item.title).contains(q) ||
                normalize(item.description).contains(q) ||
                normalize(item.director).contains(q) ||
                normalize(item.type.label).contains(q) ||
                item.year.toString().contains(q) ||
                item.cast.any { normalize(it).contains(q) } ||
                item.categories.any { normalize(it).contains(q) }

            val matchesType = filters.selectedType == null || item.type == filters.selectedType
            val matchesCategory = filters.selectedCategory == null ||
                item.categories.any { normalize(it) == normalize(filters.selectedCategory) }
            val matchesYear = filters.selectedYear == null || item.year >= filters.selectedYear
            val matchesLanguage = filters.selectedLanguage == null ||
                item.language.equals(filters.selectedLanguage, ignoreCase = true) ||
                item.availableLanguages.any { it.contains(filters.selectedLanguage, ignoreCase = true) }

            matchesQuery && matchesType && matchesCategory && matchesYear && matchesLanguage
        }.let { list ->
            if (filters.sortByPopularity) list.sortedByDescending { it.popularityScore }
            else list.sortedByDescending { it.year }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val notifications: StateFlow<List<NotificationEntity>> = currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeNotifications(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val unreadNotificationsCount: StateFlow<Int> = currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(0) else repository.observeUnreadNotificationCount(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val syncLogs: StateFlow<List<SyncLogEntity>> = repository.observeSyncLogs()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val lastSyncAt: StateFlow<Long> = repository.observeLastSyncAt()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0L)

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _syncMessage = MutableStateFlow<String?>(null)
    val syncMessage: StateFlow<String?> = _syncMessage.asStateFlow()

    init {
        viewModelScope.launch {
            _isSyncing.value = true
            val res = repository.syncCatalogNow()
            _isSyncing.value = false
            if (res.isSuccess && res.getOrDefault(0) > 0) {
                _syncMessage.value = "Catálogo actualizado automáticamente (${res.getOrDefault(0)} novedades y mejoras)."
            }
        }
        viewModelScope.launch {
            _searchFilters
                .map { it.query.trim() }
                .distinctUntilChanged()
                .debounce(450L)
                .collect { q ->
                    if (q.length >= 2) {
                        _isSearchingOnline.value = true
                        runCatching { repository.searchCatalogOnline(q) }
                        _isSearchingOnline.value = false
                    } else {
                        _isSearchingOnline.value = false
                    }
                }
        }
    }

    fun updateSearchQuery(query: String) {
        _searchFilters.update { it.copy(query = query) }
    }

    fun updateTypeFilter(type: ContentType?) {
        _searchFilters.update { it.copy(selectedType = if (it.selectedType == type) null else type) }
    }

    fun updateCategoryFilter(category: String?) {
        _searchFilters.update { it.copy(selectedCategory = if (it.selectedCategory == category) null else category) }
    }

    fun updateYearFilter(minYear: Int?) {
        _searchFilters.update { it.copy(selectedYear = if (it.selectedYear == minYear) null else minYear) }
    }

    fun updateLanguageFilter(language: String?) {
        _searchFilters.update { it.copy(selectedLanguage = if (it.selectedLanguage == language) null else language) }
    }

    fun toggleSortOrder() {
        _searchFilters.update { it.copy(sortByPopularity = !it.sortByPopularity) }
    }

    fun clearFilters() {
        _searchFilters.value = SearchFilterState()
    }

    fun toggleFavorite(contentId: String) {
        val uid = currentUserId.value ?: return
        val pid = currentProfileId.value ?: return
        viewModelScope.launch {
            repository.toggleFavorite(uid, pid, contentId)
        }
    }

    fun triggerCatalogSync() {
        if (_isSyncing.value) return
        viewModelScope.launch {
            _isSyncing.value = true
            _syncMessage.value = null
            val res = repository.syncCatalogNow()
            _isSyncing.value = false
            _syncMessage.value = if (res.isSuccess) {
                "Catálogo sincronizado (${res.getOrDefault(0)} novedades)."
            } else {
                "Modo sin conexión: usando catálogo local autorizado."
            }
        }
    }

    fun clearSyncMessage() {
        _syncMessage.value = null
    }

    fun markNotificationsRead() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repository.markAllNotificationsRead(uid)
        }
    }

    class Factory(private val repository: DetflixRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CatalogViewModel(repository) as T
        }
    }
}
