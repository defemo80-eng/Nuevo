package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MembershipEntity
import com.example.data.local.RenewalRecordEntity
import com.example.data.model.MembershipStatus
import com.example.data.repository.DetflixRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.math.ceil

data class MembershipUiState(
    val status: MembershipStatus = MembershipStatus.NONE,
    val planName: String = "Mensual DETFLIX 30 Días",
    val activatedAt: Long? = null,
    val expiresAt: Long? = null,
    val daysRemaining: Int = 0,
    val lastCodeUsed: String? = null,
    val isRedeeming: Boolean = false,
    val message: String? = null,
    val isError: Boolean = false
)

@OptIn(ExperimentalCoroutinesApi::class)
class MembershipPlayerViewModel(private val repository: DetflixRepository) : ViewModel() {

    private val currentUserId: StateFlow<String?> = repository.observeCurrentUserId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)
    private val currentProfileId: StateFlow<String?> = repository.observeCurrentProfileId()
        .stateIn(viewModelScope, SharingStarted.Eagerly, null)

    private val _redeemState = MutableStateFlow<Triple<Boolean, String?, Boolean>>(Triple(false, null, false))

    val membershipUiState: StateFlow<MembershipUiState> = currentUserId
        .flatMapLatest { uid ->
            if (uid == null) flowOf<MembershipEntity?>(null)
            else repository.observeMembership(uid)
        }
        .map { m ->
            if (m == null) {
                MembershipUiState()
            } else {
                val now = System.currentTimeMillis()
                val exp = m.expiresAt ?: 0L
                val remainingMs = (exp - now).coerceAtLeast(0L)
                val days = if (exp > now) ceil(remainingMs / 86_400_000.0).toInt() else 0
                val status = when (m.status) {
                    "SUSPENDED" -> MembershipStatus.SUSPENDED
                    "ACTIVE" -> when {
                        exp <= now -> MembershipStatus.EXPIRED
                        days <= 3 -> MembershipStatus.EXPIRING_SOON
                        else -> MembershipStatus.ACTIVE
                    }
                    "EXPIRED" -> MembershipStatus.EXPIRED
                    else -> MembershipStatus.NONE
                }
                MembershipUiState(
                    status = status,
                    planName = m.planName,
                    activatedAt = m.activatedAt,
                    expiresAt = m.expiresAt,
                    daysRemaining = days,
                    lastCodeUsed = m.lastCodeUsed
                )
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), MembershipUiState())

    val redeemBanner: StateFlow<Triple<Boolean, String?, Boolean>> = _redeemState.asStateFlow()

    val renewals: StateFlow<List<RenewalRecordEntity>> = currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeRenewals(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val cloudGatewayUrl: StateFlow<String> = repository.observeCloudGatewayUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val remoteCatalogUrl: StateFlow<String> = repository.observeRemoteCatalogUrl()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val preferredQuality: StateFlow<String> = repository.prefs.preferredQuality
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Auto")

    val autoPlayNext: StateFlow<Boolean> = repository.prefs.autoPlayNext
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    val subtitlesEnabled: StateFlow<Boolean> = repository.prefs.subtitlesEnabled
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), true)

    fun refreshExpiration() {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            repository.checkAndUpdateMembershipExpiration(uid)
        }
    }

    fun redeemCode(code: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            _redeemState.value = Triple(true, null, false)
            val result = repository.redeemAccessCode(uid, code)
            _redeemState.value = if (result.isSuccess) {
                Triple(false, "Membresía activada por 30 días. ¡Disfruta DETFLIX!", false)
            } else {
                Triple(false, result.exceptionOrNull()?.message ?: "No se pudo validar el código.", true)
            }
        }
    }

    fun clearRedeemMessage() {
        _redeemState.update { it.copy(second = null, third = false) }
    }

    fun updateCloudGatewayUrl(url: String) {
        viewModelScope.launch {
            repository.setCloudGatewayUrl(url)
        }
    }

    fun updateRemoteCatalogUrl(url: String) {
        viewModelScope.launch {
            repository.setRemoteCatalogUrl(url)
        }
    }

    fun savePlayerPreferences(quality: String, autoPlay: Boolean, subtitles: Boolean) {
        viewModelScope.launch {
            repository.savePlayerPreferences(quality, autoPlay, subtitles)
        }
    }

    suspend fun getResumePosition(contentId: String, episodeId: String?): Long {
        val pid = currentProfileId.value ?: return 0L
        return repository.getResumePosition(pid, contentId, episodeId)
    }

    fun saveProgress(contentId: String, episodeId: String?, positionMs: Long, durationMs: Long) {
        val uid = currentUserId.value ?: return
        val pid = currentProfileId.value ?: return
        viewModelScope.launch {
            repository.saveWatchProgress(uid, pid, contentId, episodeId, positionMs, durationMs)
        }
    }

    class Factory(private val repository: DetflixRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return MembershipPlayerViewModel(repository) as T
        }
    }
}
