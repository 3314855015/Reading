package com.reading.my.ui.screens.login

data class LoginUiState(
    val email: String = "",
    val verificationCode: String = "",
    val isEmailValid: Boolean = true,
    val isCodeValid: Boolean = true,
    val emailErrorMessage: String? = null,
    val codeErrorMessage: String? = null,
    val isCountingDown: Boolean = false,
    val countdownSeconds: Int = 0,
    val isLoginEnabled: Boolean = false,
    val isLoading: Boolean = false,
    val loginSuccess: Boolean = false,
    val errorMessage: String? = null
)

sealed class LoginEvent {
    data class EmailChanged(val email: String) : LoginEvent()
    data class CodeChanged(val code: String) : LoginEvent()
    data object SendCode : LoginEvent()
    data object Login : LoginEvent()
    data object ClearError : LoginEvent()
}
