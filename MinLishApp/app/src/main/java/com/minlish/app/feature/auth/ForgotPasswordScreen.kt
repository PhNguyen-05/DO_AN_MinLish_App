package com.minlish.app.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ForgotPasswordScreen(
    viewModel: AuthViewModel,
    onNavigateToReset: (String) -> Unit,
    onBack: () -> Unit
) {
    var email by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.clearForgotState()
    }

    AuthPage(
        title = "Quên mật khẩu",
        subtitle = "Nhập email tài khoản để nhận mã OTP đặt lại mật khẩu.",
        onBack = onBack
    ) {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = Icons.Default.Email,
            isError = viewModel.forgotError.isNotBlank()
        )

        AuthMessage(text = viewModel.forgotError, isError = true)
        AuthMessage(text = viewModel.forgotMessage, isError = false)

        AuthPrimaryButton(
            text = "Gửi mã OTP",
            loading = viewModel.forgotLoading,
            onClick = {
                val normalizedEmail = email.trim()
                viewModel.sendForgotPassword(
                    email = normalizedEmail,
                    onSent = { onNavigateToReset(normalizedEmail) }
                )
            }
        )
    }
}
