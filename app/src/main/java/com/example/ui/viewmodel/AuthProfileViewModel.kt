package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ProfileEntity
import com.example.data.local.UserEntity
import com.example.data.repository.DetflixRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AuthUiState(
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val infoMessage: String? = null,
    val recoveryQuestion: String? = null
)

@OptIn(ExperimentalCoroutinesApi::class)
class AuthProfileViewModel(private val repository: DetflixRepository) : ViewModel() {

    val currentUserId: StateFlow<String?> = repository.observeCurrentUserId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val currentProfileId: StateFlow<String?> = repository.observeCurrentProfileId()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val savedAccounts: StateFlow<List<UserEntity>> = repository.observeSavedAccounts()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val currentUser: StateFlow<UserEntity?> = currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(null) else repository.observeUser(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val profiles: StateFlow<List<ProfileEntity>> = currentUserId
        .flatMapLatest { uid -> if (uid == null) flowOf(emptyList()) else repository.observeProfiles(uid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val activeProfile: StateFlow<ProfileEntity?> = currentProfileId
        .flatMapLatest { pid -> if (pid == null) flowOf(null) else repository.observeProfile(pid) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    private val _authUiState = MutableStateFlow(AuthUiState())
    val authUiState: StateFlow<AuthUiState> = _authUiState.asStateFlow()

    init {
        viewModelScope.launch {
            repository.initializeApp()
        }
    }

    fun clearMessages() {
        _authUiState.update { it.copy(errorMessage = null, infoMessage = null) }
    }

    fun enterWithNameAndCode(displayName: String, accessCode: String) {
        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = repository.enterWithNameAndCode(displayName, accessCode)
            _authUiState.update { state ->
                if (result.isSuccess) state.copy(isLoading = false)
                else state.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun enterSavedAccount(uid: String) {
        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, errorMessage = null, infoMessage = null) }
            val result = repository.enterSavedAccount(uid)
            _authUiState.update { state ->
                if (result.isSuccess) state.copy(isLoading = false)
                else state.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun selectProfile(profileId: String) {
        viewModelScope.launch {
            repository.selectProfile(profileId)
        }
    }

    fun saveProfile(
        profileId: String?,
        name: String,
        avatarId: String,
        isKids: Boolean,
        parentalPin: String?,
        preferredGenres: List<String>
    ) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            _authUiState.update { it.copy(isLoading = true, errorMessage = null) }
            val result = repository.saveProfile(uid, profileId, name, avatarId, isKids, parentalPin, preferredGenres)
            _authUiState.update {
                if (result.isSuccess) it.copy(isLoading = false, infoMessage = "Perfil guardado.")
                else it.copy(isLoading = false, errorMessage = result.exceptionOrNull()?.message)
            }
        }
    }

    fun deleteProfile(profileId: String) {
        val uid = currentUserId.value ?: return
        viewModelScope.launch {
            val result = repository.deleteProfile(uid, profileId)
            if (result.isFailure) {
                _authUiState.update { it.copy(errorMessage = result.exceptionOrNull()?.message) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
        }
    }

    class Factory(private val repository: DetflixRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AuthProfileViewModel(repository) as T
        }
    }
}
