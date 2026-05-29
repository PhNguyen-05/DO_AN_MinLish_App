package com.minlish.app.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight

@Composable
fun LoginScreen(
    viewModel: AuthViewModel,
    onNavigateToRegister: () -> Unit,
    onNavigateToForgot: () -> Unit,
    onLoginSuccess: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }

    AuthPage(
        title = "Đăng nhập",
        subtitle = "Tiếp tục học từ vựng cùng MinLish",
        footer = {
            AuthFooterLink(
                text = "Chưa có tài khoản?",
                actionText = "Đăng ký",
                onClick = onNavigateToRegister
            )
        }
    ) {
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "Email",
            leadingIcon = Icons.Default.Email
        )

        AuthPasswordField(
            value = password,
            onValueChange = { password = it },
            label = "Mật khẩu",
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            leadingIcon = Icons.Default.Lock
        )

        AuthMessage(text = viewModel.loginError, isError = true)

        AuthPrimaryButton(
            text = "Đăng nhập",
            loading = viewModel.loginLoading,
            onClick = { viewModel.login(email, password, onLoginSuccess) }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            TextButton(onClick = onNavigateToForgot) {
                Text(
                    text = "Quên mật khẩu?",
                    color = AuthPrimaryDark,
                    fontWeight = FontWeight.SemiBold
                )
            }
        }
    }
}
