package com.reading.my.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.data.local.UserSessionManager
import com.reading.my.data.network.ApiService
import com.reading.my.data.network.model.UpdateAvatarRequest
import com.reading.my.data.network.model.UpdateUsernameRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class EditProfileUiState(
    val username: String = "",
    val avatarUrl: String? = null,
    val pendingAvatarUri: String? = null,  // 用户选择但未保存的头像
    val isLoading: Boolean = false,
    val hasChanges: Boolean = false,
    val usernameError: String? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class EditProfileViewModel @Inject constructor(
    private val sessionManager: UserSessionManager,
    private val apiService: ApiService
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProfileUiState())
    val uiState: StateFlow<EditProfileUiState> = _uiState.asStateFlow()

    private var originalUsername = ""

    init {
        viewModelScope.launch {
            sessionManager.sessionInfoFlow.firstOrNull()?.let { session ->
                originalUsername = session.username
                _uiState.update {
                    it.copy(
                        username = session.username,
                        avatarUrl = session.avatar?.takeIf { a -> a.isNotBlank() }
                    )
                }
            }
        }
    }

    /** 用户从相册选择了头像 URI */
    fun onAvatarSelected(uri: String) {
        _uiState.update { it.copy(pendingAvatarUri = uri, hasChanges = true) }
    }

    /** 更新昵称（单独保存，带回调关闭编辑页） */
    fun updateUsername(newName: String, onSuccess: () -> Unit) {
        if (newName == originalUsername) { onSuccess(); return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, usernameError = null) }
            try {
                val resp = apiService.updateUsername(UpdateUsernameRequest(newName))
                if (resp.isSuccess() && resp.data != null) {
                    originalUsername = newName
                    sessionManager.saveSession(
                        userId = resp.data!!.id,
                        email = resp.data!!.email,
                        username = resp.data!!.username,
                        avatar = resp.data!!.avatar,
                        accessToken = sessionManager.sessionInfoFlow.firstOrNull()?.accessToken ?: "",
                        refreshToken = sessionManager.sessionInfoFlow.firstOrNull()?.refreshToken ?: ""
                    )
                    _uiState.update { it.copy(isLoading = false, username = newName, hasChanges = it.pendingAvatarUri != null) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, usernameError = resp.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, usernameError = "网络异常，请重试") }
            }
        }
    }

    /** 保存所有变更（头像等） */
    fun saveAll(onSuccess: () -> Unit) {
        val pending = _uiState.value.pendingAvatarUri ?: run { onSuccess(); return }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            try {
                // TODO: [头像上传] 后续实现真实图片上传到服务器，当前仅保存本地 URI
                val resp = apiService.updateAvatar(UpdateAvatarRequest(pending))
                if (resp.isSuccess()) {
                    sessionManager.updateAvatar(pending)
                    _uiState.update { it.copy(isLoading = false, avatarUrl = pending, pendingAvatarUri = null, hasChanges = false) }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = resp.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "保存失败，请重试") }
            }
        }
    }
}
