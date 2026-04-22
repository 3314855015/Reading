package com.reading.my.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.data.local.UserSessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProfileUiState(
    val username: String = "",
    val bio: String? = null,
    val avatarUrl: String? = null,
    val isAssociated: Boolean = false,
    val localCount: Int = 0,
    val publishCount: Int = 0,
    val groupCount: Int = 0,
    val isDarkMode: Boolean = false,
    val isNetworkVerify: Boolean = true
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val sessionManager: UserSessionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadSession()
    }

    private fun loadSession() {
        viewModelScope.launch {
            sessionManager.sessionInfoFlow.collect { session ->
                if (session != null) {
                    _uiState.update {
                        it.copy(
                            username = session.username.ifBlank { session.email.substringBefore("@") },
                            avatarUrl = session.avatar?.takeIf { a -> a.isNotBlank() }
                        )
                    }
                }
            }
        }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    fun toggleNetworkVerify() {
        _uiState.update { it.copy(isNetworkVerify = !it.isNetworkVerify) }
    }
}
