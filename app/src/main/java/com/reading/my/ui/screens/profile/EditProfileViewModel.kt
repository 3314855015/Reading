package com.reading.my.ui.screens.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.data.local.ImageFileHelper
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
    val pendingAvatarUri: String? = null,   // 用户从相册选中的原始 URI（用于进入裁剪页）
    val pendingAvatarBase64: String? = null, // 裁剪确认后的 Base64 数据（待上传）
    val showAvatarCrop: Boolean = false,      // 是否显示头像裁剪页
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

    /**
     * 用户从相册选择了头像 → 保存 URI + 跳转裁剪页
     */
    fun onAvatarSelected(uri: String) {
        _uiState.update { it.copy(pendingAvatarUri = uri, showAvatarCrop = true) }
    }

    /** 关闭头像裁剪页 */
    fun dismissAvatarCrop() {
        _uiState.update { it.copy(showAvatarCrop = false) }
    }

    /**
     * 头像裁剪确认回调 — 立即保存本地 + 异步推送服务器
     *
     * 乐观更新策略：点击"使用"后立即写入 UserSessionManager，
     * 所有观察 sessionInfoFlow 的页面（我的/主页/资料）即时看到新头像。
     * 后台异步推送到服务器，成功后用规范 URL 替换临时数据。
     */
    fun onAvatarCropped(context: android.content.Context, base64: String) {
        // ── 1. Base64 → 本地文件 → file:// URI ──
        val localFileUri = ImageFileHelper.saveAvatarFromBase64(context, base64)
        if (localFileUri == null) {
            android.util.Log.e("AvatarSave", "保存头像文件失败")
            return
        }

        // ── 2. 立即保存本地（不阻塞 UI）──
        viewModelScope.launch {
            sessionManager.updateAvatar(localFileUri)
            android.util.Log.d("AvatarSave", "sessionManager.updateAvatar completed, uri=$localFileUri")
        }

        // ── 3. 更新 UI 状态（关闭裁剪页）──
        _uiState.update {
            it.copy(
                avatarUrl = localFileUri,
                pendingAvatarUri = null,
                pendingAvatarBase64 = null,
                showAvatarCrop = false,
                hasChanges = false,
                errorMessage = null
            )
        }

        // ── 3. 异步推送到服务器（后台静默执行）──
        viewModelScope.launch {
            try {
                val resp = apiService.updateAvatar(UpdateAvatarRequest(base64))
                if (resp.isSuccess()) {
                    val serverUrl = resp.data?.avatar?.takeIf { it.isNotBlank() } ?: localFileUri
                    sessionManager.updateAvatar(serverUrl)
                    _uiState.update { it.copy(avatarUrl = serverUrl) }
                }
            } catch (_: Exception) {
                // 静默失败：本地已有新头像，下次启动时可重试同步
            }
        }
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
                    _uiState.update {
                        it.copy(isLoading = false, username = newName, hasChanges = it.pendingAvatarBase64 != null)
                    }
                    onSuccess()
                } else {
                    _uiState.update { it.copy(isLoading = false, usernameError = resp.message) }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, usernameError = "网络异常，请重试") }
            }
        }
    }

    /**
     * 保存所有变更 — 乐观更新策略（先本地，后远程）
     *
     * 流程：
     *   1. 立即保存到本地 SessionManager → UI 马上看到新头像
     *   2. 异步推送到服务器
     *   3. 成功：用服务器返回的规范 URL 替换本地的临时数据
     *   4. 失败：保留本地数据（用户可正常使用），提示可稍后重试
     */
    fun saveAll(context: android.content.Context, onSuccess: () -> Unit) {
        val pendingBase64 = _uiState.value.pendingAvatarBase64 ?: run { onSuccess(); return }

        val localFileUri = com.reading.my.data.local.ImageFileHelper.saveAvatarFromBase64(context, pendingBase64)
            ?: run { onSuccess(); return }

        viewModelScope.launch {
            // ── 第一步：立即保存本地（乐观更新）──
            sessionManager.updateAvatar(localFileUri)

            _uiState.update {
                it.copy(
                    avatarUrl = localFileUri,
                    pendingAvatarUri = null,
                    pendingAvatarBase64 = null,
                    hasChanges = false,
                    errorMessage = null
                )
            }
            onSuccess()

            // ── 第二步：异步推送到服务器（不阻塞 UI）──
            try {
                val resp = apiService.updateAvatar(UpdateAvatarRequest(pendingBase64))
                if (resp.isSuccess()) {
                    val serverUrl = resp.data?.avatar?.takeIf { it.isNotBlank() } ?: localFileUri
                    sessionManager.updateAvatar(serverUrl)
                    _uiState.update { it.copy(avatarUrl = serverUrl) }
                } else {
                    _uiState.update { it.copy(errorMessage = "头像已保存到本地，同步到服务器失败：${resp.message}") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "头像已保存到本地，网络异常：${e.message}") }
            }
        }
    }
}
