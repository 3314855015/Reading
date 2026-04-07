package com.reading.my.ui.screens.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LoginViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    private val _countdownJob: Job? = null
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

    private fun handleEmailChanged(email: String) {
        val isValid = email.isEmpty() || EMAIL_REGEX.matches(email)
        _uiState.update {
            it.copy(
                email = email,
                isEmailValid = isValid,
                emailErrorMessage = if (isValid) null else "请输入有效邮箱",
                isLoginEnabled = calculateLoginEnabled(email, it.verificationCode)
            )
        }
    }

    private fun handleCodeChanged(code: String) {
        val isValid = code.isEmpty() || code.length == 6
        _uiState.update {
            it.copy(
                verificationCode = code,
                isCodeValid = isValid,
                codeErrorMessage = if (isValid) null else "请输入6位验证码",
                isLoginEnabled = calculateLoginEnabled(it.email, code)
            )
        }
    }

    private fun handleSendCode() {
        val currentEmail = _uiState.value.email
        if (!EMAIL_REGEX.matches(currentEmail)) {
            _uiState.update {
                it.copy(
                    isEmailValid = false,
                    emailErrorMessage = "请输入有效邮箱"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                delay(1000)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCountingDown = true,
                        countdownSeconds = 60
                    )
                }
                startCountdown()

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "验证码发送失败，请稍后重试"
                    )
                }
            }
        }
    }

    private fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            while (_uiState.value.countdownSeconds > 0) {
                delay(1000)
                _uiState.update {
                    val newSeconds = it.countdownSeconds - 1
                    it.copy(
                        countdownSeconds = newSeconds,
                        isCountingDown = newSeconds > 0
                    )
                }
            }
        }
    }

    private fun handleLogin() {
        val currentState = _uiState.value

        if (!EMAIL_REGEX.matches(currentState.email)) {
            _uiState.update {
                it.copy(
                    isEmailValid = false,
                    emailErrorMessage = "请输入有效邮箱"
                )
            }
            return
        }

        if (currentState.verificationCode.length != 6) {
            _uiState.update {
                it.copy(
                    isCodeValid = false,
                    codeErrorMessage = "请输入正确验证码"
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                delay(1500)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        loginSuccess = true
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        isCodeValid = false,
                        codeErrorMessage = "验证码错误"
                    )
                }
            }
        }
    }

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

    private fun calculateLoginEnabled(email: String, code: String): Boolean {
        return EMAIL_REGEX.matches(email) && code.length == 6
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
