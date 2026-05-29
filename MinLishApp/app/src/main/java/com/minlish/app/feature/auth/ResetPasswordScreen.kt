package com.minlish.app.feature.auth

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun ResetPasswordScreen(
    viewModel: AuthViewModel,
    email: String?,
    onResetSuccess: () -> Unit,
    onBack: () -> Unit
) {
    var otp by remember { mutableStateOf("") }
    var newPassword by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmVisible by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf("") }

    AuthPage(
        title = "Đặt lại mật khẩu",
        subtitle = "Nhập OTP trong email và tạo mật khẩu mới cho tài khoản.",
        onBack = onBack
    ) {
        AuthTextField(
            value = otp,
            onValueChange = {
                otp = it
                localError = ""
            },
            label = "Mã OTP",
            leadingIcon = Icons.Default.Key,
            isError = localError.isNotBlank()
        )

        AuthPasswordField(
            value = newPassword,
            onValueChange = {
                newPassword = it
                localError = ""
            },
            label = "Mật khẩu mới",
            visible = passwordVisible,
            onVisibilityChange = { passwordVisible = !passwordVisible },
            leadingIcon = Icons.Default.Lock,
            isError = localError.isNotBlank()
        )

        AuthPasswordField(
            value = confirmPassword,
            onValueChange = {
                confirmPassword = it
                localError = ""
            },
            label = "Xác nhận mật khẩu",
            visible = confirmVisible,
            onVisibilityChange = { confirmVisible = !confirmVisible },
            leadingIcon = Icons.Default.Lock,
            isError = localError.isNotBlank()
        )

        AuthMessage(text = localError, isError = true)

        AuthPrimaryButton(
            text = "Cập nhật mật khẩu",
            loading = viewModel.resetLoading,
            onClick = {
                if (newPassword != confirmPassword) {
                    localError = "Mật khẩu xác nhận không khớp."
                    return@AuthPrimaryButton
                }
                viewModel.resetPassword(
                    email = email.orEmpty(),
                    otp = otp,
                    newPassword = newPassword,
                    onSuccess = onResetSuccess,
                    onError = { localError = it }
                )
            }
        )
    }
}
