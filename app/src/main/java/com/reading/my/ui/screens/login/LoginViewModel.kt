package com.reading.my.ui.screens.login

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.reading.my.data.local.UserSessionManager
import com.reading.my.data.network.ApiResponse
import com.reading.my.data.network.ApiService
import com.reading.my.data.network.model.LoginRequest
import com.reading.my.data.network.model.SendEmailCodeRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * 登录 ViewModel
 * 
 * 通过 ApiService 发起真实网络请求（当前由 MockApiInterceptor 拦截返回模拟数据）
 * 后端就绪后，关闭 MockApiInterceptor.ENABLED = false 即可无缝切换
 *
 * 登录成功后自动保存 Session 到 DataStore（7天有效期）
 */
@HiltViewModel
class LoginViewModel @Inject constructor(
    private val apiService: ApiService,
    private val sessionManager: UserSessionManager  // ★ 新增：会话管理
) : ViewModel() {

    companion object {
        private const val TAG = "LoginViewModel"
    }

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")

    fun onEvent(event: LoginEvent) {
        when (event) {
            is LoginEvent.EmailChanged -> handleEmailChanged(event.email)
            is LoginEvent.CodeChanged -> handleCodeChanged(event.code)
            LoginEvent.SendCode -> handleSendCode()
            LoginEvent.Login -> handleLogin()
            LoginEvent.ClearError -> handleClearError()
        }
    }

    // ==================== 邮箱输入 ====================

    private fun handleEmailChanged(email: String) {
        val isValid = email.isEmpty() || EMAIL_REGEX.matches(email)
        _uiState.update {
            it.copy(
                email = email,
                isEmailValid = isValid,
                emailErrorMessage = if (!isValid && email.isNotEmpty()) "请输入有效邮箱" else null,
                isLoginEnabled = calculateLoginEnabled(email, it.verificationCode)
            )
        }
    }

    // ==================== 验证码输入 ====================

    private fun handleCodeChanged(code: String) {
        val isValid = code.isEmpty() || code.length == 6
        _uiState.update {
            it.copy(
                verificationCode = code,
                isCodeValid = isValid,
                codeErrorMessage = if (!isValid && code.isNotEmpty()) "请输入6位验证码" else null,
                isLoginEnabled = calculateLoginEnabled(it.email, code)
            )
        }
    }

    // ==================== 发送验证码 ====================

    private fun handleSendCode() {
        val currentEmail = _uiState.value.email

        // 前端校验
        if (!EMAIL_REGEX.matches(currentEmail)) {
            _uiState.update {
                it.copy(isEmailValid = false, emailErrorMessage = "请输入有效邮箱")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                Log.d(TAG, "[API] 调用发送验证码接口 → email=$currentEmail")

                val request = SendEmailCodeRequest(email = currentEmail)
                val response: ApiResponse<*> = apiService.sendEmailCode(request)

                Log.d(TAG, "[API] 发送验证码响应: code=${response.code}, msg=${response.message}")

                if (response.isSuccess()) {
                    Log.i(TAG, "[API] 验证码发送成功")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCountingDown = true,
                            countdownSeconds = 60
                        )
                    }
                    startCountdown()
                } else {
                    // 业务错误（如邮箱格式不正确、频率限制等）
                    Log.w(TAG, "[API] 发送验证码失败: ${response.getErrorMessage()}")
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = response.getErrorMessage(),
                            emailErrorMessage = response.getErrorMessage(),
                            isEmailValid = false
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "[API] 发送验证码异常: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "网络异常，请检查连接后重试"
                    )
                }
            }
        }
    }

    // ==================== 登录 ====================

    private fun handleLogin() {
        val currentState = _uiState.value

        // 前端校验 - 邮箱
        if (!EMAIL_REGEX.matches(currentState.email)) {
            _uiState.update {
                it.copy(isEmailValid = false, emailErrorMessage = "请输入有效邮箱")
            }
            return
        }

        // 前端校验 - 验证码长度
        if (currentState.verificationCode.length != 6) {
            _uiState.update {
                it.copy(isCodeValid = false, codeErrorMessage = "请输入正确验证码")
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            try {
                Log.d(TAG, "[API] 调用登录接口 → email=${currentState.email}, code=${currentState.verificationCode}")

                val request = LoginRequest(
                    email = currentState.email,
                    code = currentState.verificationCode
                )

                val response = apiService.login(request)

                Log.d(TAG, "[API] 登录响应: code=${response.code}, msg=${response.message}")

                if (response.isSuccess() && response.data != null) {
                    val loginData = response.data!!
                    Log.i(TAG, "[API] ✅ 登录成功! userId=${loginData.userId}, username=${loginData.username}, isNewUser=${loginData.isNewUser}")

                    // ★ 保存登录会话到 DataStore（7天有效期）
                    sessionManager.saveSession(
                        userId = loginData.userId,
                        email = loginData.email,
                        username = loginData.username,
                        avatar = loginData.avatar,
                        accessToken = loginData.accessToken,
                        refreshToken = loginData.refreshToken
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loginSuccess = true
                        )
                    }
                } else {
                    // 登录业务错误（验证码错误/过期/账户封禁等）
                    Log.w(TAG, "[API] 登录失败: ${response.getErrorMessage()}")
                    
                    // 根据错误码分类处理
                    val errorMsg = response.getErrorMessage()
                    val codeErrorMap = mapOf(
                        20006 to "验证码错误",
                        20007 to "验证码已过期",
                        20008 to "验证码已使用",
                        20009 to "账户已被封禁，请联系客服"
                    )

                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            isCodeValid = false,
                            codeErrorMessage = codeErrorMap[response.code] ?: errorMsg,
                            errorMessage = errorMsg
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e(TAG, "[API] 登录异常: ${e.message}", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "登录失败，请稍后重试"
                    )
                }
            }
        }
    }

    // ==================== 清除错误 ====================

    private fun handleClearError() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                isEmailValid = true,
                isCodeValid = true,
                emailErrorMessage = null,
                codeErrorMessage = null
            )
        }
    }

    // ==================== 倒计时 ====================

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdownSeconds > 0) {
                kotlinx.coroutines.delay(1000L)
                _uiState.update {
                    val newSeconds = it.countdownSeconds - 1
                    it.copy(countdownSeconds = newSeconds, isCountingDown = newSeconds > 0)
                }
            }
        }
    }

    // ==================== 工具方法 ====================

    private fun calculateLoginEnabled(email: String, code: String): Boolean =
        EMAIL_REGEX.matches(email) && code.length == 6

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
