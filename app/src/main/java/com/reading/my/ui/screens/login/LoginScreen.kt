package com.reading.my.ui.screens.login

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.reading.my.ui.theme.BackgroundWhite
import com.reading.my.ui.theme.BorderColor
import com.reading.my.ui.theme.ErrorRed
import com.reading.my.ui.theme.PrimaryOrange
import com.reading.my.ui.theme.TextHint
import com.reading.my.ui.theme.TextPrimary

@Composable
fun LoginScreen(
    viewModel: LoginViewModel = hiltViewModel(),
    onLoginSuccess: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loginSuccess) {
        onLoginSuccess()
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundWhite)
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(80.dp))

        Text(
            text = "📚 Reading",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "欢迎来到阅读世界",
            fontSize = 14.sp,
            color = TextHint
        )

        Spacer(modifier = Modifier.height(60.dp))

        EmailInputField(
            email = uiState.email,
            isValid = uiState.isEmailValid,
            errorMessage = uiState.emailErrorMessage,
            onEmailChange = { viewModel.onEvent(LoginEvent.EmailChanged(it)) }
        )

        Spacer(modifier = Modifier.height(16.dp))

        VerificationCodeInputField(
            code = uiState.verificationCode,
            isValid = uiState.isCodeValid,
            errorMessage = uiState.codeErrorMessage,
            isCountingDown = uiState.isCountingDown,
            countdownSeconds = uiState.countdownSeconds,
            isLoading = uiState.isLoading,
            onCodeChange = { viewModel.onEvent(LoginEvent.CodeChanged(it)) },
            onSendCode = { viewModel.onEvent(LoginEvent.SendCode) }
        )

        if (uiState.errorMessage != null) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = uiState.errorMessage!!,
                color = ErrorRed,
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(40.dp))

        LoginButton(
            enabled = uiState.isLoginEnabled && !uiState.isLoading,
            isLoading = uiState.isLoading,
            onClick = { viewModel.onEvent(LoginEvent.Login) }
        )
    }
}

@Composable
private fun EmailInputField(
    email: String,
    isValid: Boolean,
    errorMessage: String?,
    onEmailChange: (String) -> Unit
) {
    Column {
        OutlinedTextField(
            value = email,
            onValueChange = onEmailChange,
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .border(
                    width = if (isValid) 1.dp else 1.5.dp,
                    color = if (isValid) BorderColor else ErrorRed,
                    shape = RoundedCornerShape(8.dp)
                ),
            placeholder = {
                Text(
                    text = "邮箱",
                    color = TextHint
                )
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = PrimaryOrange,
                unfocusedBorderColor = Color.Transparent,
                errorBorderColor = ErrorRed,
                focusedContainerColor = BackgroundWhite,
                unfocusedContainerColor = BackgroundWhite,
                errorContainerColor = BackgroundWhite
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
            singleLine = true,
            isError = !isValid
        )

        if (!isValid && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun VerificationCodeInputField(
    code: String,
    isValid: Boolean,
    errorMessage: String?,
    isCountingDown: Boolean,
    countdownSeconds: Int,
    isLoading: Boolean,
    onCodeChange: (String) -> Unit,
    onSendCode: () -> Unit
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedTextField(
                value = code,
                onValueChange = { if (it.length <= 6) onCodeChange(it) },
                modifier = Modifier
                    .weight(2f)
                    .clip(RoundedCornerShape(8.dp))
                    .border(
                        width = if (isValid) 1.dp else 1.5.dp,
                        color = if (isValid) BorderColor else ErrorRed,
                        shape = RoundedCornerShape(8.dp)
                    ),
                placeholder = {
                    Text(
                        text = "验证码",
                        color = TextHint
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryOrange,
                    unfocusedBorderColor = Color.Transparent,
                    errorBorderColor = ErrorRed,
                    focusedContainerColor = BackgroundWhite,
                    unfocusedContainerColor = BackgroundWhite,
                    errorContainerColor = BackgroundWhite
                ),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true,
                isError = !isValid
            )

            Spacer(modifier = Modifier.width(12.dp))

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(56.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (isCountingDown) TextHint else PrimaryOrange
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                } else {
                    Button(
                        onClick = onSendCode,
                        enabled = !isCountingDown,
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Transparent),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Transparent,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = Color.White,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isCountingDown) "${countdownSeconds}s" else "获取",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        if (!isValid && errorMessage != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = errorMessage,
                color = ErrorRed,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 4.dp)
            )
        }
    }
}

@Composable
private fun LoginButton(
    enabled: Boolean,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = PrimaryOrange,
            disabledContainerColor = PrimaryOrange.copy(alpha = 0.5f),
            contentColor = Color.White,
            disabledContentColor = Color.White
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = Color.White,
                strokeWidth = 2.dp
            )
        } else {
            Text(
                text = "登 录",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )
        }
    }
}
